// 子路径部署统一路径模型（用户裁决"App 真正支持子路径"）。
// 部署形态：门户 portal.py 把 App 挂在 /complex/ 下，并用 rewrite_html 给
// href/src 资源自动补 /complex 前缀——故 vite base 保持 "/"（根路径构建），
// 资源前缀由门户负责；本模块只处理"路由"层的前缀剥离/补回。

const DEPLOY_PREFIX = "/complex";

/** 当前是否运行在 /complex 子路径下（生产=是，dev 根路径=否）。 */
function isDeployed(): boolean {
  return typeof window !== "undefined" && window.location.pathname.startsWith(DEPLOY_PREFIX);
}

/**
 * 剥掉部署前缀，返回 App 内根路径（"/complex/admin" → "/admin"，"/complex/" → "/"）。
 * 纯函数：直接自检入参前缀，dev 根路径（"/admin"）原样返回。
 */
export function stripBase(pathname: string): string {
  if (pathname === DEPLOY_PREFIX || pathname.startsWith(`${DEPLOY_PREFIX}/`)) {
    return pathname.slice(DEPLOY_PREFIX.length) || "/";
  }
  return pathname;
}

/** 给 App 内根路径补部署前缀（pushState 用，不跳出 /complex）。dev 下原样返回。 */
export function withBase(path: string): string {
  return isDeployed() ? `${DEPLOY_PREFIX}${path}` : path;
}

/**
 * 入口壳选择（纯逻辑，便于单测）。入参为已 stripBase 剥掉部署前缀的 App 内路径。
 * "/admin*" → 后台；"/dealer*" → 独立经销商门户；根路径与客人入口 → 客人端；其余兜底客人端。
 */
export function selectShell(path: string): "admin" | "customer" | "dealer" {
  if (path.startsWith("/admin")) return "admin";
  // CG-11d：独立经销商 Web 门户（受控第十一端 dealer_portal）单独成壳，与后台、客人端隔离。
  if (path.startsWith("/dealer")) return "dealer";
  return "customer";
}
