import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";
import LegacyConsoleApp from "./legacy-console/LegacyConsoleApp";

const AdminApp = React.lazy(() => import("./admin/AdminApp"));
const CustomerApp = React.lazy(() => import("./customer/CustomerApp"));

function RootApp(): React.ReactElement {
  if (window.location.pathname.startsWith("/admin")) {
    return (
      <React.Suspense fallback={<div className="loading-state">加载完整后台...</div>}>
        <AdminApp />
      </React.Suspense>
    );
  }

  if (window.location.pathname.startsWith("/customer")) {
    return (
      <React.Suspense fallback={<div className="loading-state">加载客人端...</div>}>
        <CustomerApp />
      </React.Suspense>
    );
  }

  return <LegacyConsoleApp />;
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
