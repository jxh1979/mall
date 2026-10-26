// 文件版本：V0.02
// 客人专用构建入口：只挂客人端（不引用后台、经销商门户等员工端代码）。
// antd v5 官方 React 19 兼容补丁（须在入口最先引入）：不接则 message／Modal／notification 静态方法在 React 19 下弹不出来。
import "@ant-design/v5-patch-for-react-19";
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
