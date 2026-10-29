// 文件版本：V0.01
import React from "react";
import { createPortal } from "react-dom";
import { QRCodeSVG } from "qrcode.react";

// 与后端 WatermarkQr 同值（反查按这组参数解码，后端测试按它合成截屏）：平铺间距、二维码边长（21 模块×6 像素，纠错 H、无静区）、格内偏移、不透明度。
const TILE_PX = 200;
const QR_PX = 126;
const OFFSET_PX = 24;
const OPACITY = 0.08;

/**
 * 第 23 条 23-R2 在线页面暗纹（PGM:687「后台、商户专区和员工手机端的所有在线页面都带有按同一规则生成的暗纹；暗纹可反向推算：把页面的截屏图片、
 * 或对着屏幕拍摄的照片上传到系统，即可算出该页面是由哪个账号、在哪一台设备上、于什么时间打开的」）：把这次打开的暗纹码平铺成二维码加小字，
 * 浅色主题黑色模块、深色主题白色模块，挂在 body 最上层（盖住 antd 弹窗与抽屉浮层），不接收鼠标、读屏跳过。
 */
export function PageWatermark({ code, theme }: { code: string; theme: "light" | "dark" }) {
  const color = theme === "dark" ? "#fff" : "#000";
  const columns = Math.ceil(Math.max(window.screen?.width ?? 0, window.innerWidth) / TILE_PX) + 1;
  const rows = Math.ceil(Math.max(window.screen?.height ?? 0, window.innerHeight) / TILE_PX) + 1;
  return createPortal(
    <div
      data-testid="admin-watermark"
      aria-hidden="true"
      style={{
        position: "fixed", inset: 0, zIndex: 2000, pointerEvents: "none", userSelect: "none", overflow: "hidden", opacity: OPACITY,
        display: "grid", gridTemplateColumns: `repeat(${columns}, ${TILE_PX}px)`, gridAutoRows: `${TILE_PX}px`, color, fontSize: 12,
      }}
    >
      {Array.from({ length: columns * rows }, (_, index) => (
        <div key={index} style={{ padding: `${OFFSET_PX}px 0 0 ${OFFSET_PX}px` }}>
          <QRCodeSVG value={code} level="H" size={QR_PX} marginSize={0} fgColor={color} bgColor="transparent" style={{ display: "block" }} />
          <div>{code}</div>
        </div>
      ))}
    </div>,
    document.body,
  );
}
