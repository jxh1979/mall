import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

// 三壳入口：
// - 根路径 "/" 与 "/customer*" → CustomerApp（客人/前台入口）。
// - "/admin*" → AdminApp（完整后台，必须显式进入后台路径）。
// 根路径不再直接进入后台，避免普通访问误入员工管理端。
const AdminApp = React.lazy(() => import("./admin/AdminApp"));
const CustomerApp = React.lazy(() => import("./customer/CustomerApp"));

function RootApp(): React.ReactElement {
  const { pathname } = window.location;

  if (
    pathname === "/" ||
    pathname.startsWith("/customer") ||
    pathname.startsWith("/ticketing") ||
    pathname.startsWith("/bar") ||
    pathname.startsWith("/amusement")
  ) {
    return (
      <React.Suspense fallback={<div className="loading-state">加载客人端...</div>}>
        <CustomerApp />
      </React.Suspense>
    );
  }

  if (pathname.startsWith("/admin")) {
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
