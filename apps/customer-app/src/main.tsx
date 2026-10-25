// 文件版本：V0.01
// 客人专用构建入口：只挂客人端（不引用后台、经销商门户等员工端代码）。
import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";

import CustomerApp from "./CustomerApp";
import "./styles.css";

const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error("找不到页面根节点 #root");
}

createRoot(rootElement).render(
  <React.StrictMode>
    <CustomerApp />
  </React.StrictMode>,
);
