// 文件版本：V0.04
// 官方客人 App 外壳（移动布局）：底部四标签（首页／票与订单／酒吧／我的）＋二级页，页面栈在内存里（不靠地址栏），
// Android 返回键回上一页、首页再按退出；深链经 platform.onDeepLink 进入（只认 customer-core 白名单路由）。
// 终端开关（PGM「关闭后菜单不可见或不可点，接口也要拒绝业务动作」）：本端被关、本版本被关或拉取失败显示「本端暂停服务」；模块被关隐藏该模块入口。
// 当前综合体（PGM「可以按全局、综合体……开关」）：页面所选综合体、深链带的综合体，都没有时只有一个综合体就用它；请求带 X-Project-Id
// （后端按综合体拒绝）；唯一的综合体被关即本端暂停服务，深链带的综合体被关该页只显示暂停服务，选综合体的页面不列被关的综合体。
// 令牌只在内存与本端安全会话（platform.session）里，不进 URL、localStorage、日志或分享内容；恢复会话时断网或服务端异常不删令牌，回前台再恢复。
// ENUM「扫码点单身份：非会员可以浏览、点单和付款」：未登录也可扫桌台码（或收到桌台码深链）直接进扫码点单，不经登录页。
// BSM「查看综合体介绍、营业时间、活动公告、票务入口」：首页只调匿名接口，未登录也可从登录页进首页（与小程序同口径），其余页面仍要登录。
import { AppstoreOutlined, CoffeeOutlined, HomeOutlined, LeftOutlined, UserOutlined } from "@ant-design/icons";
import { Button, ConfigProvider, List, Result, Space } from "antd";
import zhCN from "antd/es/locale/zh_CN";
import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";

import {
  customerSwitchState,
  parseCustomerLink,
  resetCustomerSessionExpiredNotice,
  setCustomerSessionExpiredHandler,
  type CustomerLinkRoute,
  type CustomerLoginResponse,
  type CustomerModule,
  type CustomerSession,
  type TerminalSwitchRow,
} from "@complex/customer-core";

import { CUSTOMER_APP_VERSION, CUSTOMER_LINK_HOST, createCustomerService } from "./api";
import { CustomerAuthShell } from "./components/CustomerAuthShell";
import { ReplayedNotice } from "./components/ReplayedNotice";
import { TicketExpiryReminder } from "./components/TicketExpiryReminder";
import { AccountPage } from "./pages/AccountPage";
import { BarReservationPage } from "./pages/BarReservationPage";
import { BarScanOrderPage } from "./pages/BarScanOrderPage";
import { BuyTicketPage } from "./pages/BuyTicketPage";
import { ComplaintsPage } from "./pages/ComplaintsPage";
import { DeregisterPage } from "./pages/DeregisterPage";
import { HomePage } from "./pages/HomePage";
import { InvitationAcceptPage } from "./pages/InvitationAcceptPage";
import { MyOrdersPage } from "./pages/MyOrdersPage";
import { MyReservationsPage } from "./pages/MyReservationsPage";
import { ParkingPage } from "./pages/ParkingPage";
import { PointsPage } from "./pages/PointsPage";
import { SavedBottlesPage } from "./pages/SavedBottlesPage";
import { StudentCertificationPage } from "./pages/StudentCertificationPage";
import { TicketTransferPage } from "./pages/TicketTransferPage";
import { VoucherWalletPage } from "./pages/VoucherWalletPage";
import { WalletPage } from "./pages/WalletPage";
import { platform } from "./platform";

// antd 组件（日期选择器等）中文化；dayjs 同步切中文，避免英文文案（PGM「界面全中文」）。
dayjs.locale("zh-cn");

type TabKey = "home" | "tickets" | "bar" | "me";
type PageKey = TabKey | "buy" | "parking" | "orders" | "transfer" | "reserve" | "reservations" | "invitation" | "scan" | "points" | "wallet"
  | "vouchers" | "bottles" | "complaints" | "student" | "account" | "deregister";
/** projectId＝该页报上来的所选综合体（停车、投诉；购票、订台为所选票种／台位所属的综合体）。 */
type Route = { page: PageKey; params?: Record<string, string>; projectId?: string };

// 页面标题单一来源（标签、入口与每页 h1 共用）；module＝该页所属客人模块（请求头 X-Module-Code、开关行 scope_id）。
const PAGES: Record<PageKey, { title: string; module?: CustomerModule }> = {
  home: { title: "首页" },
  tickets: { title: "票与订单" },
  bar: { title: "酒吧" },
  me: { title: "我的" },
  buy: { title: "购票", module: "ticketing" },
  parking: { title: "停车缴费", module: "parking" },
  orders: { title: "我的订单 / 票 / 二维码", module: "ticketing" },
  transfer: { title: "门票转赠", module: "transfer" },
  reserve: { title: "订台", module: "bar" },
  reservations: { title: "我的订台", module: "bar" },
  invitation: { title: "接受邀请", module: "bar" },
  scan: { title: "扫码点单", module: "bar" },
  points: { title: "积分 / 签到", module: "points" },
  wallet: { title: "储值 / 会员", module: "wallet" },
  vouchers: { title: "我的券包", module: "vouchers" },
  bottles: { title: "我的存酒", module: "saved_bottles" },
  complaints: { title: "投诉与反馈", module: "complaints" },
  student: { title: "学生认证", module: "account" },
  account: { title: "账户与条款", module: "account" },
  deregister: { title: "注销账号", module: "account" },
};

const TABS: Array<{ key: TabKey; icon: ReactNode; entries: PageKey[] }> = [
  { key: "home", icon: <HomeOutlined />, entries: ["buy", "parking"] },
  { key: "tickets", icon: <AppstoreOutlined />, entries: ["orders", "transfer", "buy"] },
  { key: "bar", icon: <CoffeeOutlined />, entries: ["reserve", "reservations", "invitation", "scan"] },
  { key: "me", icon: <UserOutlined />, entries: ["points", "wallet", "vouchers", "bottles", "complaints", "student", "account", "deregister"] },
];

// 深链路由 → 页面，参数照传（停车：项目与车牌；酒吧专属购票入口：酒吧；桌台码：桌台；领取：领取码；邀请函：码载荷）。
const LINK_PAGES: Record<CustomerLinkRoute, PageKey> = {
  parking: "parking",
  bar: "buy",
  table: "scan",
  "ticket-claim": "transfer",
  "voucher-claim": "vouchers",
  invitation: "invitation",
};

const HOME: Route = { page: "home" };

export default function CustomerApp(): React.ReactElement {
  const tokenRef = useRef<string | null>(null);
  const moduleRef = useRef<string | null>(null);
  const projectRef = useRef<string | null>(null);
  // 业务请求带当前页面所属模块与当前综合体；开关拉取不带模块（某模块被关时仍要能拉到开关、只隐藏那个模块）。
  const service = useMemo(
    () => createCustomerService(() => tokenRef.current, () => ({ moduleCode: moduleRef.current, projectId: projectRef.current })),
    [],
  );
  const switchService = useMemo(() => createCustomerService(), []);
  const [session, setSession] = useState<CustomerSession | null>(null);
  const sessionRef = useRef(session);
  sessionRef.current = session;
  const [restoring, setRestoring] = useState(true);
  const [notice, setNotice] = useState<string | null>(null);
  const [stack, setStack] = useState<Route[]>([HOME]);
  // 本端已关闭的开关行：undefined＝尚未拉到，null＝拉取失败（按本端暂停服务）。
  const [switchRows, setSwitchRows] = useState<TerminalSwitchRow[] | null | undefined>(undefined);
  // 只有一个综合体时的那个综合体（多个时为空，以页面所属综合体为准）。
  const [onlyProject, setOnlyProject] = useState<string | null>(null);

  const switches = switchRows === undefined ? null
    : customerSwitchState(switchRows, { version: CUSTOMER_APP_VERSION, projectId: onlyProject });
  const projectOpen = (projectId: string) =>
    !switchRows || !customerSwitchState(switchRows, { version: CUSTOMER_APP_VERSION, projectId }).paused;
  const hidden = switches?.hiddenModules;
  const visible = (page: PageKey) => {
    const module = PAGES[page].module;
    return !module || !hidden?.has(module);
  };
  // 模块被关：栈里属于该模块的页一并收起，退回所在标签。
  const visibleStack = stack.filter((route) => visible(route.page));
  const current = visibleStack[visibleStack.length - 1] ?? HOME;
  // 子页的请求在本组件的 effect 之前发出，故在渲染时同步当前模块与当前综合体。
  moduleRef.current = PAGES[current.page].module ?? null;
  const linkProject = current.params?.project_id ?? null;
  projectRef.current = current.projectId ?? linkProject ?? onlyProject;
  const linkPaused = linkProject !== null && !projectOpen(linkProject);
  const stackRef = useRef(visibleStack);
  stackRef.current = visibleStack;

  const focusMain = () => requestAnimationFrame(() => document.getElementById("main-content")?.focus());
  const open = (page: PageKey, params?: Record<string, string>) => {
    setStack([...stackRef.current, { page, params }]);
    focusMain();
  };
  const switchTab = (tab: TabKey) => {
    setStack([{ page: tab }]);
    focusMain();
  };
  const setPageProject = useCallback((projectId: string) => {
    setStack((now) => now.map((route, index) => (index === now.length - 1 ? { ...route, projectId } : route)));
  }, []);
  const back = useCallback(() => {
    const now = stackRef.current;
    if (now.length > 1) setStack(now.slice(0, -1));
    else if (now[0]?.page !== "home") setStack([HOME]);
    else platform.exitApp();
  }, []);

  useEffect(() => platform.onBackButton(back), [back]);

  useEffect(
    () =>
      platform.onDeepLink((url) => {
        const link = parseCustomerLink(url, CUSTOMER_LINK_HOST);
        if (link) setStack([HOME, { page: LINK_PAGES[link.route], params: link.params }]);
      }),
    [],
  );

  // 启动与回前台拉本端已关闭的开关；拉取失败按兜底关闭（本端暂停服务）。启动时读一次综合体列表定「只有一个综合体」。
  useEffect(() => {
    const load = () => {
      switchService
        .listEffectiveTerminalSwitches()
        .then((rows) => setSwitchRows(rows ?? []))
        .catch(() => setSwitchRows(null));
    };
    load();
    switchService.listProjects().then((rows) => setOnlyProject(rows?.length === 1 ? rows[0]!.projectId : null)).catch(() => undefined);
    return platform.onResume(load);
  }, [switchService]);

  const clearLocalSession = useCallback(async () => {
    tokenRef.current = null;
    await platform.session.clear();
  }, []);

  // 会话过期全局回登录页：任何页面任何请求 401 → 清本端令牌、回登录视图并提示；真 403 权限不足不登出。
  useEffect(() => {
    setCustomerSessionExpiredHandler(() => {
      void clearLocalSession();
      setSession(null);
      setNotice("登录已过期，请重新登录");
    });
    return () => setCustomerSessionExpiredHandler(null);
  }, [clearLocalSession]);

  // 重新登录成功后复位一次性防抖标记，下次会话再过期仍能触发回登录。
  useEffect(() => {
    if (session) resetCustomerSessionExpiredNotice();
  }, [session]);

  // 凭本端安全会话里的令牌恢复登录态：服务端判令牌已失效（401）即清掉；断网、超时或服务端异常不算失效，令牌留着，回到前台再恢复。
  const restore = useCallback(async () => {
    const token = await platform.session.load();
    if (!token) return;
    tokenRef.current = token;
    try {
      const info = await service.restoreSession();
      if (!info) await clearLocalSession();
      else setSession({ customerId: info.customerId, maskedPhone: info.maskedPhone, customerToken: "", expiresAt: info.expiresAt ?? "" });
    } catch {
      // 结果未明：按未恢复处理，令牌不删。
    }
  }, [service, clearLocalSession]);

  useEffect(() => {
    let cancelled = false;
    void restore().finally(() => {
      if (!cancelled) setRestoring(false);
    });
    const off = platform.onResume(() => {
      if (!sessionRef.current) void restore();
    });
    return () => {
      cancelled = true;
      off();
    };
  }, [restore]);

  // 登录或注册成功（CustomerAuthShell 内完成取码、登录、未注册转注册）。
  const onAuthenticated = async (result: CustomerLoginResponse) => {
    tokenRef.current = result.customerToken;
    await platform.session.save(result.customerToken);
    setNotice(null);
    // 页面状态里不放令牌（customerToken 置空），令牌只在 tokenRef 与本端安全会话里。
    setSession({ customerId: result.customerId, maskedPhone: result.maskedPhone, customerToken: "", expiresAt: result.expiresAt });
  };

  const onLogout = async () => {
    // 先带令牌撤销服务端会话，再清本端令牌。
    await service.logout();
    await clearLocalSession();
    setSession(null);
    setStack([HOME]);
  };

  const entryList = (entries: PageKey[]) => (
    <List
      className="customer-entry-list"
      bordered
      dataSource={entries.filter(visible)}
      renderItem={(page) => (
        <List.Item>
          <Button type="link" block style={{ textAlign: "left" }} onClick={() => open(page)} data-testid={`customer-entry-${page}`}>
            {PAGES[page].title}
          </Button>
        </List.Item>
      )}
    />
  );

  const tabOf = (page: PageKey) => TABS.find((tab) => tab.key === page);

  const renderPage = (route: Route): ReactNode => {
    if (!session) return null;
    switch (route.page) {
      case "home":
        return (
          <>
            <HomePage service={service} />
            {entryList(tabOf("home")!.entries)}
          </>
        );
      case "tickets":
      case "bar":
        return entryList(tabOf(route.page)!.entries);
      case "me":
        return (
          <>
            <p data-testid="customer-phone">已登录：{session.maskedPhone}</p>
            {entryList(tabOf("me")!.entries)}
            <Button block style={{ marginTop: 12 }} onClick={() => void onLogout()} data-testid="customer-logout">
              退出登录
            </Button>
          </>
        );
      case "buy":
        return <BuyTicketPage service={service} lockedBarId={route.params?.bar_id ?? null} onProject={setPageProject} />;
      case "parking":
        return <ParkingPage service={service} initialProjectId={route.params?.project_id} initialCode={route.params?.code}
          projectOpen={projectOpen} onProject={setPageProject} />;
      case "orders":
        return <MyOrdersPage service={service} />;
      case "transfer":
        return <TicketTransferPage service={service} initialClaimCode={route.params?.claim_code} />;
      case "reserve":
        return <BarReservationPage service={service} onProject={setPageProject} />;
      case "reservations":
        return <MyReservationsPage service={service} />;
      case "invitation":
        return <InvitationAcceptPage service={service} initialLink={route.params} />;
      case "scan":
        return <BarScanOrderPage service={service} initialTableId={route.params?.table_id} />;
      case "points":
        return <PointsPage service={service} />;
      case "wallet":
        return <WalletPage service={service} />;
      case "vouchers":
        return <VoucherWalletPage service={service} initialClaimCode={route.params?.claim_code} />;
      case "bottles":
        return <SavedBottlesPage service={service} />;
      case "complaints":
        return <ComplaintsPage service={service} projectOpen={projectOpen} onProject={setPageProject} />;
      case "student":
        return <StudentCertificationPage service={service} />;
      case "account":
        return <AccountPage service={service} maskedPhone={session.maskedPhone} />;
      case "deregister":
        return <DeregisterPage service={service} onDeregistered={() => void onLogout().then(() => setNotice("账号已注销"))} />;
    }
  };

  const activeTab = visibleStack[0]?.page ?? "home";

  // 未登录也可看的页（非会员扫码点单；从登录页进的首页）：只有标题栏与该页，返回即回登录页。
  const anonymousPage = !session && !restoring && (current.page === "scan" || (current.page === "home" && visibleStack.length > 1));

  const body = switches?.paused ? (
    <div data-testid="customer-paused">
      <Result status="warning" title="本端暂停服务" subTitle="请稍后再试，或到现场服务台咨询。" />
    </div>
  ) : anonymousPage ? (
    <div className="customer-shell" data-testid={`customer-anonymous-${current.page}-page`}>
      <header className="customer-header">
        <Button type="text" icon={<LeftOutlined />} aria-label="返回" onClick={back} data-testid="customer-back" />
        <h1 className="customer-page-title">{PAGES[current.page].title}</h1>
      </header>
      <main id="main-content" className="customer-content" tabIndex={-1}>
        {current.page === "scan" ? (
          <BarScanOrderPage service={service} initialTableId={current.params?.table_id} />
        ) : (
          <>
            <HomePage service={service} />
            {entryList(tabOf("home")!.entries)}
          </>
        )}
      </main>
    </div>
  ) : restoring || !session ? (
    <>
      <CustomerAuthShell service={service} notice={notice} restoring={restoring} onAuthenticated={onAuthenticated} />
      {!restoring ? (
        <Space direction="vertical" style={{ width: "100%", padding: "0 16px 24px" }}>
          <Button block onClick={() => open("home")} data-testid="customer-anonymous-home">不登录，先看首页</Button>
          {visible("scan") ? (
            <Button block onClick={() => open("scan")} data-testid="customer-anonymous-scan">不登录，扫桌台码点单</Button>
          ) : null}
        </Space>
      ) : null}
    </>
  ) : (
    <div className="customer-shell" data-testid="customer-app">
      <header className="customer-header">
        {visibleStack.length > 1 ? (
          <Button type="text" icon={<LeftOutlined />} aria-label="返回" onClick={back} data-testid="customer-back" />
        ) : null}
        <h1 className="customer-page-title">{PAGES[current.page].title}</h1>
      </header>
      <main id="main-content" className="customer-content" tabIndex={-1}>
        {/* M725-F03：重复提交返回第一次的结果时提示已完成。 */}
        <ReplayedNotice />
        {linkPaused ? (
          <div data-testid="customer-project-paused">
            <Result status="warning" title="该综合体本端暂停服务" subTitle="请稍后再试，或到现场服务台咨询。" />
          </div>
        ) : renderPage(current)}
        <TicketExpiryReminder service={service} />
      </main>
      <nav className="customer-tabbar" aria-label="底部导航">
        {TABS.map((tab) => (
          <button
            type="button"
            key={tab.key}
            aria-current={activeTab === tab.key ? "page" : undefined}
            onClick={() => switchTab(tab.key)}
            data-testid={`customer-tab-${tab.key}`}
          >
            {tab.icon}
            <span>{PAGES[tab.key].title}</span>
          </button>
        ))}
      </nav>
    </div>
  );

  return (
    <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: "#0958d9", colorPrimaryHover: "#1c64d4", colorPrimaryActive: "#0a4fb8" } }}>
      {body}
    </ConfigProvider>
  );
}
