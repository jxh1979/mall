import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

// 三壳重构（P2）：默认壳切换为 admin（完整后台）。
// - 根路径 "/" 与 "/admin*" → AdminApp（主力后台，默认壳）。
// - "/customer*" → CustomerApp（客人端，按现有约定）。
// - "/legacy*" → LegacyConsoleApp（已退役/降只读，仅显式前缀进入；写能力已被 redirectToAdmin 抽空）。
// 历史上根路径默认进入 legacy，造成「主力 admin 反需 /admin 前缀」的心智倒置，此处修正。
const AdminApp = React.lazy(() => import("./admin/AdminApp"));
const CustomerApp = React.lazy(() => import("./customer/CustomerApp"));
const LegacyConsoleApp = React.lazy(() => import("./legacy-console/LegacyConsoleApp"));

function RootApp(): React.ReactElement {
  const { pathname } = window.location;

  if (pathname.startsWith("/customer")) {
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

  // 默认壳（含根路径 "/" 与 "/admin*"）：admin 完整后台。
  return (
    <React.Suspense fallback={<div className="loading-state">加载完整后台...</div>}>
      <AdminApp />
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
