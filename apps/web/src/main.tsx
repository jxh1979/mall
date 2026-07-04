import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";
import { selectShell, stripBase } from "./basePath";
import "./styles.css";

// 三壳入口：
// - 根路径 "/" 与 "/customer*" → CustomerApp（客人/前台入口）。
// - "/admin*" → AdminApp（完整后台，必须显式进入后台路径）。
// 根路径不再直接进入后台，避免普通访问误入员工管理端。
// 子路径部署：先 stripBase 剥掉 /complex 前缀再判，/complex/admin→/admin→后台、/complex/→/→客人端。
const AdminApp = React.lazy(() => import("./admin/AdminApp"));
const CustomerApp = React.lazy(() => import("./customer/CustomerApp"));

function RootApp(): React.ReactElement {
  const path = stripBase(window.location.pathname);

  if (selectShell(path) === "admin") {
    return (
      <React.Suspense fallback={<div className="loading-state">加载完整后台...</div>}>
        <AdminApp />
      </React.Suspense>
    );
  }

  return (
    <React.Suspense fallback={<div className="loading-state">加载客人端...</div>}>
      <CustomerApp />
    </React.Suspense>
  );
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
