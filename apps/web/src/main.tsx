import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

// 三壳入口：
// - 根路径 "/" 与 "/customer*" → CustomerApp（客人/前台入口）。
// - "/admin*" → AdminApp（完整后台，必须显式进入后台路径）。
// - "/legacy*" → LegacyConsoleApp（已退役/降只读，仅显式前缀进入；写能力已被 redirectToAdmin 抽空）。
// 根路径不再直接进入后台，避免普通访问误入员工管理端。
const AdminApp = React.lazy(() => import("./admin/AdminApp"));
const CustomerApp = React.lazy(() => import("./customer/CustomerApp"));
const LegacyConsoleApp = React.lazy(() => import("./legacy-console/LegacyConsoleApp"));

function RootApp(): React.ReactElement {
  const { pathname } = window.location;

  if (pathname === "/" || pathname.startsWith("/customer")) {
    return (
      <React.Suspense fallback={<div className="loading-state">加载客人端...</div>}>
        <CustomerApp />
      </React.Suspense>
    );
  }

  // legacy 已退役：仅在显式 /legacy 前缀下进入，且入口提示「已迁移至管理后台」。
  if (pathname.startsWith("/legacy")) {
    return (
      <React.Suspense fallback={<div className="loading-state">加载退役控制台...</div>}>
        <LegacyConsoleApp />
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
