// 文件版本：V0.01
// 两端（后台 adminClient / 客人端 customerClient）共用：写请求的请求编号（幂等键）跟着「一次操作」走，不跟着「一次 HTTP 请求」走。
// 依据（M725-F02、M725-F03）：治理地图第 303 行「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款、重复入账或重复核销」
// 「没有返回确认的不算完成，不扣减、不核销。同一次操作重复提交时，返回第一次的结果并提示已完成」，第 528 行红线⑤。
//
// 同一方法、同一路径、同一请求内容（不含请求编号）算同一次操作。首发一开始就记下它的编号：在它拿到明确结果之前（还在路上、
// 网络中断、超时、服务暂时不可用、服务端仍在处理），再提交同样内容沿用这个编号——页面按点击现生成的编号也让位于它——
// 服务端据此只认一次、返回第一次的结果。什么时候清掉：首发拿到结果（成功或报错；报错时须没有同一编号的再次提交还没拿到明确
// 结果）；沿用编号的再次提交成功；对同一对象开始反向或改写的操作（见 reverses 与同一路径的 PUT／PATCH）——它以首发身份被明确
// 拒绝、当时带同一编号的提交都已被明确拒绝（没有一笔执行）时放回；换了登录的人。沿用编号的再次提交收到的报错不是第一次的结果，编号照留。
// 服务端的请求编号记录不过期（IdempotencyService 只写 expires_at、从不读），这里也不设存活时限。
// ponytail: 只记在页面内存里，刷新页面、另开标签页、换设备即丢——已知上限：之后再提交同样内容会换新编号，服务端按新的一次操作
// 处理，第一次若已生效也不会返回第一次的结果；需要跨刷新时改存 sessionStorage。
// ponytail: 只按内容认同一次操作，反向只认同一路径的 PUT／PATCH 与成对的一正一反；同一路径 POST 来回设值、撤掉后再建一模一样的、
// 同一内容本来就会合法地再做一次（扫描、计算、同额登记、再兑同一件等）、本标签页里另一地址的操作顺带撤销了第一次的效果（如范围变更
// 撤掉角色与模板套用、审批通过解除显式拒绝）、别人或别的终端在中间反向操作，都会把新的一次认成第一次那次（服务端不执行、页面却
// 提示已完成）——已知上限；改法是由页面持有编号。

// open：带这个编号、还没拿到明确结果的提交笔数（在路上的，和结果未明的）。holds：作废了它、还没被明确拒绝的反向或改写操作数，
// 大于 0 时不再沿用。voided：这次操作开始时作废的登记。
type Unsettled = { key: string; path: string; content: Record<string, unknown>; open: number; holds: number; voided: Unsettled[] };
const unsettled = new Map<string, Unsettled>();
// 请求内容里带发生时刻的操作（见 firstSubmitTime）：内容（不含时刻）→ 第一次提交时取的时刻。
const firstSubmitTimes = new Map<string, string>();
// 当前登录的人。
let operator: string | undefined;

/**
 * 两端登录成功、恢复会话时调用，传入登录身份（后台用户编号、客人编号）。只在换了人（或此前不知道是谁）时清掉此前没拿到结果的
 * 请求编号：编号只按「方法＋路径＋正文」认，正文里不一定带是谁，两端登出都不刷新页面，不清就会串到下一个人。同一个人重新登录
 * （含会话失效后）照常沿用：服务端按提交范围（账号与所属组织，带商户归属的按所属项目与商户）认同一编号的重复提交，与会话无关。
 */
export function forgetWriteOperations(identity: string): void {
  if (identity === operator) return;
  operator = identity;
  unsettled.clear();
  firstSubmitTimes.clear();
}

/**
 * 请求内容里要带「操作发生的时刻」的页面用（订台时间、导出发生时间）：时刻不能每次点击现取——没拿到结果再点时请求内容就变了，
 * beginWriteOperation 认不出是同一次操作，会换新编号。content 是这次操作实际发出的、除时刻之外的内容（开头放页面自己的名字，
 * 免得不同页面撞上）：第一次提交时取当前时刻并记下，拿到成功结果（forgetSubmitTime）之前再提交同样内容都返回这一个——不论页面
 * 是否重新挂载（切到别的菜单再回来）、中间是否提交过别的内容。登记范围与请求编号相同：只在页面内存里，换了登录的人即清。
 * ponytail: 被明确拒绝或放弃后很久再提交同样内容，带的仍是第一次的时刻；要按结果区分时由页面在 catch 里按报错调 forgetSubmitTime。
 */
export function firstSubmitTime(content: unknown): string {
  const id = JSON.stringify(content);
  const time = firstSubmitTimes.get(id) ?? new Date().toISOString();
  firstSubmitTimes.set(id, time);
  return time;
}

/** 这次操作拿到了成功结果：之后再提交同样内容是新的一次，另取时刻。 */
export function forgetSubmitTime(content: unknown): void {
  firstSubmitTimes.delete(JSON.stringify(content));
}

/** 撤销动作直接接在原路径后：…/role-assignments 与 …/role-assignments/unassign、…/permission-overrides 与 …/remove、…/invitation 与 …/invitation/revoke。 */
const UNDO_SUFFIXES = new Set(["unassign", "remove", "revoke"]);

/**
 * 对象写在正文里的成对路径（按正向路径末段）：这些字段都相交（单值相等、列表有交集）才是同一对象。
 * 装配／卸载角色是集合路径，对象是正文里的用户与角色；员工级覆盖的路径只带用户，对象是正文里的效果与权限码
 * （AccessService：移除拒绝只删这些码的拒绝行，移除授予只删这些码的授予行）。其余成对路径的对象编号已在路径里。
 * ponytail: 按现有页面的驼峰字段名列出；以后新增对象写在正文里的成对路径要补进来，否则按路径认、会把别的对象的反向当成这个对象的。
 */
const OBJECT_FIELDS: Record<string, string[]> = {
  "role-assignments": ["userId", "roleId"],
  "permission-overrides": ["effect", "permissionCodes"],
};

function overlaps(x: unknown, y: unknown): boolean {
  const ys = Array.isArray(y) ? y : [y];
  return (Array.isArray(x) ? x : [x]).some((value) => ys.includes(value));
}

/** 登记的 a 与新开始的 b 是同一对象上的一正一反（任一方向）：末段只差一个 un（…/archive 与 …/unarchive），或撤销动作直接接在原路径后且对象相同。 */
function reverses(a: string, aContent: Record<string, unknown>, b: string, bContent: Record<string, unknown>): boolean {
  const cut = (p: string): [string, string] => [p.slice(0, p.lastIndexOf("/")), p.slice(p.lastIndexOf("/") + 1)];
  const [parentA, lastA] = cut(a);
  const [parentB, lastB] = cut(b);
  if (parentA === parentB && (lastA === `un${lastB}` || lastB === `un${lastA}`)) return true;
  const [short, long]: [string, string] = a.length < b.length ? [a, b] : [b, a];
  if (!long.startsWith(`${short}/`) || !UNDO_SUFFIXES.has(long.slice(short.length + 1))) return false;
  return (OBJECT_FIELDS[cut(short)[1]] ?? []).every((field) => overlaps(aContent[field], bContent[field]));
}

export interface WriteOperation {
  /** 本次提交要带的请求编号。 */
  readonly key: string;
  /** 按这次提交的结果收尾：status 为 HTTP 状态码，没收到响应（网络中断、超时）传 null；detailCode 为报错细码。 */
  settle(status: number | null, detailCode?: string | null): void;
}

export function beginWriteOperation(
  method: string,
  path: string,
  body: Record<string, unknown>,
  newKey: () => string,
): WriteOperation {
  const content: Record<string, unknown> = { ...body };
  delete content.idempotencyKey;
  delete content.idempotency_key;
  const id = `${method} ${path} ${JSON.stringify(content)}`;
  const current = unsettled.get(id);
  const pending = current && current.holds === 0 ? current : undefined;
  const own = body.idempotencyKey;
  // 首发一开始就登记：在途时再提交同一内容（双击、切页回来再点）也沿用这个编号；此后只删不加，迟到的失败不会把已清掉的编号放回去。
  const entry = pending ?? { key: typeof own === "string" && own ? own : newKey(), path, content, open: 0, holds: 0, voided: [] };
  // 对同一对象开始反向或改写的操作：之前对它没拿到结果的登记作废，之后同样内容是新的一次操作。PUT／PATCH 改写的是路径所指
  // 的那个对象；POST 只认成对的一正一反（同一路径的 POST 常是不同对象，如发券、兑换，不能因为它作废）。开始时就作废：反向操作
  // 自己没拿到结果时，服务端可能已反向，再做第一次那个操作须真正执行；这类来回切换的动作再执行一次不会重复扣减。
  // 已作废的也记上这次：作废它的操作全都被明确拒绝了才放回，其中有一个成功或结果未明，对象就可能已变。
  for (const [other, older] of unsettled) {
    if (other === id || entry.voided.includes(older)) continue;
    if ((method !== "POST" && older.path === path) || reverses(older.path, older.content, path, content)) {
      older.holds += 1;
      entry.voided.push(older);
    }
  }
  entry.open += 1;
  if (pending === undefined) unsettled.set(id, entry);
  // 新的一次提交开始，撤下上一条「已完成」提示。
  announceReplayed(false);
  return {
    key: entry.key,
    settle(status, detailCode = null) {
      // 没收到响应、5xx（含网关超时）、「请求正在处理中」：结果未明，登记照留，这一笔仍算没拿到明确结果。
      if (status === null || status >= 500 || detailCode === "IDEMPOTENCY_IN_PROGRESS") return;
      entry.open -= 1;
      // 首发被拒（如 429 限速、业务报错回滚）只说明首发没执行成；带同一编号的再次提交还没拿到明确结果时，它在服务端可能照常执行，
      // 编号照留、由它的结果定。沿用编号的再次提交被拒，只说明这一次没成，不说明第一次没生效（例如第一次还在执行、这一次抢输被拒），
      // 编号照留；「该编号已被不同的请求使用」（IDEMPOTENCY_CONFLICT）除外，留着只会一直被拒。「该提交编号已被使用」
      // （IDEMPOTENCY_KEY_USED，别的提交范围已用掉这个编号）不在除外之列：本人换了所属组织或商户后再提交会收到它，这时第一次
      // 已经生效，清掉就会换新编号再执行一次。
      const unrun = status >= 300 && pending === undefined && entry.open === 0;
      if (status < 300 || unrun || detailCode === "IDEMPOTENCY_CONFLICT") {
        if (unsettled.get(id) === entry) unsettled.delete(id);
      }
      // 这次操作以首发身份被明确拒绝、当时带同一编号的提交都已被明确拒绝（没有一笔执行）：服务端没执行它，对象没变，它开始时
      // 作废的登记放回，再提交与第一次逐字相同的操作仍是同一次。同一编号有一笔执行了，作废的登记永不放回（沿用的一笔先成功、
      // 首发随后被拒时也不放回）；沿用编号的一笔被拒不放回：同一编号此前的一笔可能已执行。
      if (status < 300) entry.voided = [];
      if (unrun) for (const older of entry.voided) older.holds -= 1;
    },
  };
}

export const REPLAYED_NOTICE = "该操作已完成（重复提交，返回首次结果）";

const listeners = new Set<(notice: string | null) => void>();

export function onReplayedNotice(listener: (notice: string | null) => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** 服务端对重复提交返回第一次的结果时带 replayed=true：调用方照常按成功返回，这里负责提示已完成。 */
export function announceReplayed(replayed: boolean): void {
  for (const listener of listeners) listener(replayed ? REPLAYED_NOTICE : null);
}
