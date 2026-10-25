// 文件版本：V0.01
import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";
import { selectShell, stripBase } from "./basePath";
import { CustomerNoticePage } from "./CustomerNoticePage";
import "./styles.css";

// 入口（PGM「受控的十一类终端：……Web 前端（业务人员，客人无网页端）」）：
// - "/admin*" → AdminApp（完整后台，必须显式进入后台路径）。
// - "/dealer*" → DealerPortalApp（独立经销商 Web 门户，受控第十一端，与后台隔离账号/权限/数据）。
// - 根路径与其余路径（含在浏览器里打开的客人链接）→ 客人引导提示页：只有静态提示，不含任何客人功能、不调接口；
//   客人功能在官方 App 与小程序（apps/customer-app、小程序工程）。根路径不直接进入后台，避免普通访问误入员工管理端。
// 子路径部署：先 stripBase 剥掉 /complex 前缀再判，/complex/admin→/admin→后台。
const AdminApp = React.lazy(() => import("./admin/AdminApp"));
const DealerPortalApp = React.lazy(() => import("./dealer/DealerPortalApp"));

function RootApp(): React.ReactElement {
  const shell = selectShell(stripBase(window.location.pathname));

  if (shell === "admin") {
    return (
      <React.Suspense fallback={<div className="loading-state">加载完整后台...</div>}>
        <AdminApp />
      </React.Suspense>
    );
  }

  if (shell === "dealer") {
    return (
      <React.Suspense fallback={<div className="loading-state">加载经销商门户...</div>}>
        <DealerPortalApp />
      </React.Suspense>
    );
  }

  return <CustomerNoticePage />;
}

const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error("Root element #root not found");
}

createRoot(rootElement).render(
  <React.StrictMode>
    <RootApp />
  </React.StrictMode>,
);
