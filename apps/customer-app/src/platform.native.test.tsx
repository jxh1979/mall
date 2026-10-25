// 文件版本：V0.01
// 原生壳（Android 客人 App）里的原生能力适配：令牌只进系统安全存储（Android Keystore 加密），扫码只交出码内原文、
// 系统分享只带深链、支付页交系统浏览器、照片／视频可拍可选、语音现场录成 wav、切后台清动态码、深链与返回键走系统事件。
// Capacitor 插件在 jsdom 里没有原生实现，这里用替身记录调用与派发系统事件。
import { act, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  listeners: [] as Array<{ event: string; cb: (data?: unknown) => void }>,
  launchUrl: null as string | null,
}));

vi.mock("@capacitor/core", () => ({ Capacitor: { isNativePlatform: () => true, getPlatform: () => "android" } }));
vi.mock("@capacitor/app", () => ({
  App: {
    addListener: vi.fn(async (event: string, cb: (data?: unknown) => void) => {
      const entry = { event, cb };
      h.listeners.push(entry);
      return { remove: vi.fn(async () => void h.listeners.splice(h.listeners.indexOf(entry), 1)) };
    }),
    getLaunchUrl: vi.fn(async () => (h.launchUrl ? { url: h.launchUrl } : undefined)),
    exitApp: vi.fn(async () => undefined),
  },
}));
vi.mock("@capacitor/browser", () => ({ Browser: { open: vi.fn(async () => undefined) } }));
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

import { SecureStorage } from "@aparajita/capacitor-secure-storage";
import { ActionSheet } from "@capacitor/action-sheet";
import { App } from "@capacitor/app";
import { CapacitorBarcodeScanner } from "@capacitor/barcode-scanner";
import { Browser } from "@capacitor/browser";
import { Share } from "@capacitor/share";

import { DynamicCode } from "./components/DynamicCode";
import { platform } from "./platform";

const has = (event: string) => h.listeners.some((entry) => entry.event === event);
const emit = (event: string, data?: unknown) => {
  for (const entry of [...h.listeners]) if (entry.event === event) entry.cb(data);
};

/** 拦下文件选择框：记下页面造的 input，由用例决定选中哪个文件。 */
function interceptFileInput(): { current: HTMLInputElement | null } {
  const ref: { current: HTMLInputElement | null } = { current: null };
  vi.spyOn(HTMLInputElement.prototype, "click").mockImplementation(function (this: HTMLInputElement) {
    ref.current = this;
  });
  return ref;
}

function choose(input: HTMLInputElement, file: File): void {
  Object.defineProperty(input, "files", { configurable: true, value: [file] });
  input.dispatchEvent(new Event("change"));
}

/** 网页相机权限（原生壳把它转成系统相机权限申请）：granted＝客人点了允许。 */
function cameraPermission(granted: boolean): { stop: ReturnType<typeof vi.fn>; getUserMedia: ReturnType<typeof vi.fn> } {
  const stop = vi.fn();
  const getUserMedia = vi.fn(async () => {
    if (!granted) throw new DOMException("Permission denied", "NotAllowedError");
    return { getTracks: () => [{ stop }] };
  });
  Object.defineProperty(navigator, "mediaDevices", { configurable: true, value: { getUserMedia } });
  return { stop, getUserMedia };
}

describe("platform（Android 原生壳）", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    h.listeners.length = 0;
    h.launchUrl = null;
    cameraPermission(true);
  });

  it("sessionTokenOnlyInKeystoreSecureStorage：令牌只存系统安全存储（Keystore 加密），不写 localStorage", async () => {
    const setLocal = vi.spyOn(Storage.prototype, "setItem");
    await platform.session.save("tok-abc");
    expect(SecureStorage.setItem).toHaveBeenCalledWith("customer_token", "tok-abc");
    vi.mocked(SecureStorage.getItem).mockResolvedValueOnce("tok-abc");
    expect(await platform.session.load()).toBe("tok-abc");
    await platform.session.clear();
    expect(SecureStorage.removeItem).toHaveBeenCalledWith("customer_token");
    expect(setLocal).not.toHaveBeenCalled();
  });

  it("scanReturnsQrTextOnlyAndCancelReturnsNull：扫码界面文字全中文，只交出二维码原文由页面按白名单解析，取消或失败返回空", async () => {
    const raw = "https://c.complex.test/c/table?table_id=t-1";
    vi.mocked(CapacitorBarcodeScanner.scanBarcode).mockResolvedValueOnce({ ScanResult: raw, format: 0 } as never);
    expect(await platform.scanCode()).toBe(raw);
    expect(CapacitorBarcodeScanner.scanBarcode).toHaveBeenCalledWith(expect.objectContaining({
      hint: 0,
      scanInstructions: "将二维码放入框内即可自动识别",
      cancelButtonAccessibilityLabel: "取消",
    }));
    vi.mocked(CapacitorBarcodeScanner.scanBarcode).mockRejectedValueOnce(new Error("cancelled"));
    expect(await platform.scanCode()).toBeNull();
  });

  it("scanAsksCameraPermissionFirstWithoutEnglishDialog：先经系统相机权限框申请，未允许就不进扫码界面（扫码库自带的权限提示是写死的英文）", async () => {
    const granted = cameraPermission(true);
    vi.mocked(CapacitorBarcodeScanner.scanBarcode).mockResolvedValueOnce({ ScanResult: "https://c.complex.test/c/table?table_id=t-1", format: 0 } as never);
    expect(await platform.scanCode()).toBe("https://c.complex.test/c/table?table_id=t-1");
    expect(granted.getUserMedia).toHaveBeenCalledWith({ video: true });
    expect(granted.stop).toHaveBeenCalled();

    const denied = cameraPermission(false);
    expect(await platform.scanCode()).toBeNull();
    expect(denied.getUserMedia).toHaveBeenCalledTimes(1);
    expect(CapacitorBarcodeScanner.scanBarcode).toHaveBeenCalledTimes(1);
  });

  it("shareOpensSystemSheetWithLinkOnly：系统分享只带标题与深链，客人关掉分享面板不算失败", async () => {
    const link = "https://c.complex.test/c/ticket-claim?claim_code=c-1";
    expect(await platform.share({ title: "送你一张门票", link })).toBe("shared");
    expect(Share.share).toHaveBeenCalledWith({ title: "送你一张门票", text: "送你一张门票", url: link, dialogTitle: "分享给朋友" });
    vi.mocked(Share.share).mockRejectedValueOnce(new Error("Share canceled"));
    expect(await platform.share({ title: "送你一张门票", link })).toBe("canceled");
    vi.mocked(Share.share).mockRejectedValueOnce(new Error("Unsupported url"));
    expect(await platform.share({ title: "送你一张门票", link })).toBe("unavailable");
  });

  it("paymentPageOpensInSystemBrowser：支付页交系统浏览器打开（客人包不在应用内加载远程网页），打不开返回 false", async () => {
    expect(await platform.openPayment("https://pay.example.com/cashier?no=1")).toBe(true);
    expect(Browser.open).toHaveBeenCalledWith({ url: "https://pay.example.com/cashier?no=1" });
    vi.mocked(Browser.open).mockRejectedValueOnce(new Error("Unable to display URL"));
    expect(await platform.openPayment("https://pay.example.com/cashier?no=2")).toBe(false);
  });

  it("lifecycleBackButtonAndDeepLinksUseSystemEvents：切后台、回前台、返回键、深链（冷启动与运行中）走系统事件，退订即移除", async () => {
    const paused = vi.fn();
    const resumed = vi.fn();
    const back = vi.fn();
    const offPause = platform.onPause(paused);
    const offResume = platform.onResume(resumed);
    const offBack = platform.onBackButton(back);
    await waitFor(() => expect(has("pause") && has("resume") && has("backButton")).toBe(true));
    emit("pause");
    emit("resume");
    emit("backButton", { canGoBack: false });
    expect([paused.mock.calls.length, resumed.mock.calls.length, back.mock.calls.length]).toEqual([1, 1, 1]);
    offPause();
    offResume();
    offBack();
    await waitFor(() => expect(has("pause") || has("resume") || has("backButton")).toBe(false));

    h.launchUrl = "https://c.complex.test/c/table?table_id=t-9";
    const urls: string[] = [];
    platform.onDeepLink((url) => urls.push(url));
    await waitFor(() => expect(urls).toEqual(["https://c.complex.test/c/table?table_id=t-9"]));
    await waitFor(() => expect(has("appUrlOpen")).toBe(true));
    emit("appUrlOpen", { url: "https://c.complex.test/c/voucher-claim?claim_code=v-1" });
    expect(urls).toEqual(["https://c.complex.test/c/table?table_id=t-9", "https://c.complex.test/c/voucher-claim?claim_code=v-1"]);

    platform.exitApp();
    expect(App.exitApp).toHaveBeenCalledTimes(1);
  });

  it("pickPhotoOrVideoAsksCameraOrLibraryInChinese：选照片／视频先问拍摄还是从相册选，拍摄走相机；取消不弹选择框", async () => {
    const input = interceptFileInput();
    vi.mocked(ActionSheet.showActions).mockResolvedValueOnce({ index: 0, canceled: false });
    const photo = new File(["p"], "JPEG_1.jpg", { type: "image/jpeg" });
    const pickedPhoto = platform.pickMedia("image");
    await waitFor(() => expect(input.current).not.toBeNull());
    expect(ActionSheet.showActions).toHaveBeenCalledWith({
      title: "添加照片",
      options: [{ title: "拍照" }, { title: "从相册选择" }, { title: "取消", style: "CANCEL" }],
      cancelable: true,
    });
    expect(input.current!.accept).toBe("image/*");
    expect(input.current!.getAttribute("capture")).toBe("environment");
    choose(input.current!, photo);
    expect(await pickedPhoto).toBe(photo);

    input.current = null;
    vi.mocked(ActionSheet.showActions).mockResolvedValueOnce({ index: 1, canceled: false });
    const clip = new File(["v"], "VID_1.mp4", { type: "video/mp4" });
    const pickedVideo = platform.pickMedia("video");
    await waitFor(() => expect(input.current).not.toBeNull());
    expect(vi.mocked(ActionSheet.showActions).mock.calls[1]?.[0]).toEqual({
      title: "添加视频",
      options: [{ title: "录像" }, { title: "从相册选择" }, { title: "取消", style: "CANCEL" }],
      cancelable: true,
    });
    expect(input.current!.accept).toBe("video/*");
    expect(input.current!.hasAttribute("capture")).toBe(false);
    choose(input.current!, clip);
    expect(await pickedVideo).toBe(clip);

    input.current = null;
    vi.mocked(ActionSheet.showActions).mockResolvedValueOnce({ index: -1, canceled: true });
    expect(await platform.pickMedia("image")).toBeNull();
    expect(input.current).toBeNull();
  });

  // BSM L869「客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」：语音在 App 内现场录（与小程序同口径）——经网页麦克风权限
  // （原生壳转成系统麦克风权限框），录成服务端收的 wav（16 位单声道）；客人不允许使用麦克风返回空。
  it("recordVoiceAsWavAfterMicrophonePermission：现场录音经系统麦克风授权、录成 wav；不允许返回空", async () => {
    const recorder = platform as typeof platform & { startRecording?: () => Promise<{ stop(): Promise<File> } | null> };
    expect(typeof recorder.startRecording).toBe("function");
    const stopTrack = vi.fn();
    const getUserMedia = vi.fn(async () => ({ getTracks: () => [{ stop: stopTrack }] }));
    Object.defineProperty(navigator, "mediaDevices", { configurable: true, value: { getUserMedia } });
    const nodes: { processor?: { onaudioprocess: ((event: unknown) => void) | null; disconnect: ReturnType<typeof vi.fn> } } = {};
    const close = vi.fn(async () => undefined);
    class FakeAudioContext {
      sampleRate = 8000;
      destination = {};
      close = close;
      createMediaStreamSource() {
        return { connect: vi.fn(), disconnect: vi.fn() };
      }
      createScriptProcessor() {
        nodes.processor = { onaudioprocess: null, connect: vi.fn(), disconnect: vi.fn() } as never;
        return nodes.processor;
      }
    }
    vi.stubGlobal("AudioContext", FakeAudioContext);

    const recording = await recorder.startRecording!();
    expect(getUserMedia).toHaveBeenCalledWith({ audio: true });
    expect(recording).not.toBeNull();
    nodes.processor!.onaudioprocess!({ inputBuffer: { getChannelData: () => new Float32Array([0, 0.5, -1, 1]) } });
    const file = await recording!.stop();
    expect(file.type).toBe("audio/wav");
    expect(file.name).toMatch(/^\d{14}\.wav$/);
    const buffer = await file.arrayBuffer();
    const view = new DataView(buffer);
    expect(String.fromCharCode(...new Uint8Array(buffer, 0, 4))).toBe("RIFF");
    expect(String.fromCharCode(...new Uint8Array(buffer, 8, 4))).toBe("WAVE");
    expect(view.getUint16(22, true)).toBe(1);
    expect(view.getUint32(24, true)).toBe(8000);
    expect(view.getUint16(34, true)).toBe(16);
    expect(view.getUint32(40, true)).toBe(8);
    expect([0, 1, 2, 3].map((i) => view.getInt16(44 + i * 2, true))).toEqual([0, 16383, -32768, 32767]);
    expect(stopTrack).toHaveBeenCalled();
    expect(nodes.processor!.disconnect).toHaveBeenCalled();
    expect(close).toHaveBeenCalled();

    getUserMedia.mockRejectedValueOnce(new DOMException("Permission denied", "NotAllowedError"));
    expect(await recorder.startRecording!()).toBeNull();
    vi.unstubAllGlobals();
  });

  it("pauseClearsDynamicCodeAndResumeRefetchesFirst：切到后台（系统 pause 事件）即清掉动态码，回前台先取新码再显示", async () => {
    const credential = () => {
      const issued = Date.now();
      return { credentialToken: `tk-${issued}`, issuedAt: new Date(issued).toISOString(), expiresAt: new Date(issued + 30_000).toISOString() };
    };
    const fetchCode = vi.fn(async () => credential());
    render(<DynamicCode title="验票二维码" fetchCode={fetchCode} valueOf={(c) => c.credentialToken} />);
    await waitFor(() => expect(screen.queryByTestId("qr-code")).not.toBeNull());
    await waitFor(() => expect(has("pause") && has("resume")).toBe(true));

    act(() => emit("pause"));
    await waitFor(() => expect(screen.queryByTestId("qr-code")).toBeNull());
    expect(fetchCode).toHaveBeenCalledTimes(1);

    act(() => emit("resume"));
    await waitFor(() => expect(fetchCode).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByTestId("qr-code")).not.toBeNull());
  });
});
