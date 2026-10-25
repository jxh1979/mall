// 文件版本：V0.01
// 原生壳（iPhone 客人 App）里与 Android 不同的两处原生能力（其余与 Android 同一实现，见 platform.native.test.tsx）：
// - 支付页由系统浏览器页盖在应用之上打开，应用不进后台，客人关掉它时系统不发「回到前台」；按回到前台处理以便立即查单（与 Android 关掉支付页即回到前台同口径）；
// - 扫码库自己申请系统相机权限（用途说明取 Info.plist 的中文），不先经网页相机申请。
// Capacitor 插件在 jsdom 里没有原生实现，这里用替身记录监听与派发系统事件。
import { waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => {
  const listeners: Array<{ plugin: string; event: string; cb: () => void }> = [];
  const addListener = (plugin: string) => async (event: string, cb: () => void) => {
    const entry = { plugin, event, cb };
    listeners.push(entry);
    return { remove: async () => void listeners.splice(listeners.indexOf(entry), 1) };
  };
  return { listeners, addListener };
});

vi.mock("@capacitor/core", () => ({ Capacitor: { isNativePlatform: () => true, getPlatform: () => "ios" } }));
vi.mock("@capacitor/app", () => ({
  App: { addListener: vi.fn(h.addListener("App")), getLaunchUrl: vi.fn(async () => undefined), exitApp: vi.fn(async () => undefined) },
}));
vi.mock("@capacitor/browser", () => ({ Browser: { open: vi.fn(async () => undefined), addListener: vi.fn(h.addListener("Browser")) } }));
vi.mock("@capacitor/share", () => ({ Share: { share: vi.fn(async () => ({})) } }));
vi.mock("@capacitor/barcode-scanner", () => ({
  CapacitorBarcodeScanner: { scanBarcode: vi.fn() },
  CapacitorBarcodeScannerTypeHint: { QR_CODE: 0 },
}));
vi.mock("@capacitor/action-sheet", () => ({
  ActionSheet: { showActions: vi.fn() },
  ActionSheetButtonStyle: { Default: "DEFAULT", Destructive: "DESTRUCTIVE", Cancel: "CANCEL" },
}));
vi.mock("@aparajita/capacitor-secure-storage", () => ({
  SecureStorage: { getItem: vi.fn(async () => null), setItem: vi.fn(async () => undefined), removeItem: vi.fn(async () => undefined) },
}));

import { CapacitorBarcodeScanner } from "@capacitor/barcode-scanner";

import { platform } from "./platform";

const has = (plugin: string, event: string) => h.listeners.some((entry) => entry.plugin === plugin && entry.event === event);
const emit = (plugin: string, event: string) => {
  for (const entry of [...h.listeners]) if (entry.plugin === plugin && entry.event === event) entry.cb();
};

describe("platform（iPhone 原生壳）", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    h.listeners.length = 0;
  });

  it("closingPaymentPageCountsAsReturnToForeground：iPhone 关掉支付页（系统浏览器页）即按回到前台处理、立即查单；切回应用照常；退订即移除", async () => {
    const resumed = vi.fn();
    const off = platform.onResume(resumed);
    await waitFor(() => expect(has("Browser", "browserFinished")).toBe(true));
    emit("Browser", "browserFinished");
    expect(resumed).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(has("App", "resume")).toBe(true));
    emit("App", "resume");
    expect(resumed).toHaveBeenCalledTimes(2);
    off();
    await waitFor(() => expect(h.listeners).toHaveLength(0));
  });

  it("scanUsesScannerOwnCameraPermission：iPhone 扫码由扫码库自己申请系统相机权限，不先经网页相机申请；未允许或取消返回空", async () => {
    const getUserMedia = vi.fn(async () => ({ getTracks: () => [] }));
    Object.defineProperty(navigator, "mediaDevices", { configurable: true, value: { getUserMedia } });
    const raw = "https://c.complex.test/c/table?table_id=t-1";
    vi.mocked(CapacitorBarcodeScanner.scanBarcode).mockResolvedValueOnce({ ScanResult: raw, format: 0 } as never);
    expect(await platform.scanCode()).toBe(raw);
    expect(getUserMedia).not.toHaveBeenCalled();
    expect(CapacitorBarcodeScanner.scanBarcode).toHaveBeenCalledWith(expect.objectContaining({ scanInstructions: "将二维码放入框内即可自动识别" }));
    vi.mocked(CapacitorBarcodeScanner.scanBarcode).mockRejectedValueOnce(new Error("camera access denied"));
    expect(await platform.scanCode()).toBeNull();
  });
});
