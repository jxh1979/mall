// 子路径部署统一路径模型（用户裁决"App 真正支持子路径"）。
// import.meta.env.BASE_URL: dev=/ , build=/complex/（见 vite.config.ts base）。
// 权威规则：读 window.location.pathname 前先 stripBase 剥掉部署前缀再判路由；
// 生成 pushState 的 url 时用 withBase 补回前缀，保证不跳出 /complex。

// BASE_URL 形如 "/" 或 "/complex/"，统一去掉尾部斜杠得到前缀（"" 或 "/complex"）。
function basePrefix(): string {
  const base = import.meta.env.BASE_URL ?? "/";
  return base === "/" ? "" : base.replace(/\/$/, "");
}

/** 剥掉部署前缀，返回 App 内根路径（"/complex/admin" → "/admin"，"/complex/" → "/"）。 */
export function stripBase(pathname: string): string {
  const prefix = basePrefix();
  if (prefix && (pathname === prefix || pathname.startsWith(`${prefix}/`))) {
    return pathname.slice(prefix.length) || "/";
  }
  return pathname;
}

/** 给 App 内根路径补上部署前缀（"/admin?x=1" → "/complex/admin?x=1"）。dev 下 prefix 为空即原样返回。 */
export function withBase(path: string): string {
  return `${basePrefix()}${path}`;
}
