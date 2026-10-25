// 文件版本：V0.01
// 原生能力适配层（客人 App 里唯一接原生能力的文件）：安全会话、扫码、分享深链、支付拉起、媒体选取、现场录音、生命周期、深链与返回键。
// 原生壳（Capacitor）里用系统能力实现；浏览器调试与 e2e 用 Web 内存实现。页面只认本接口。
// 令牌只放内存与系统安全存储（Android 为 Keystore 加密，iPhone 为钥匙串），不进 URL、localStorage、日志或分享内容。
import { SecureStorage } from "@aparajita/capacitor-secure-storage";
import { ActionSheet, ActionSheetButtonStyle } from "@capacitor/action-sheet";
import { App } from "@capacitor/app";
import { Browser } from "@capacitor/browser";
import { Capacitor, type PluginListenerHandle } from "@capacitor/core";
import { Share } from "@capacitor/share";

export type MediaKind = "image" | "video";
/** 一段进行中的现场录音：stop() 结束并交出录好的语音文件。 */
export type AudioRecording = { stop(): Promise<File> };
/** canceled：客人自己关掉了系统分享面板（不算失败、不提示）。 */
export type ShareResult = "shared" | "copied" | "unavailable" | "canceled";
type Listener = () => void;

export interface CustomerPlatform {
  /** 会话令牌：原生端存系统安全存储；Web 内存实现只放内存（刷新页面即需重新登录）。 */
  session: {
    load(): Promise<string | null>;
    save(token: string): Promise<void>;
    clear(): Promise<void>;
  };
  /** 扫码：返回码内原文，由调用方用 parseCustomerLink 校验；取消或本端不支持返回 null。 */
  scanCode(): Promise<string | null>;
  /** 分享深链（链接里只有业务参数，不带令牌）。 */
  share(input: { title: string; link: string }): Promise<ShareResult>;
  /** 打开支付页（launchDecision 判定为 open_url 时）；付完回到前台由 onResume 触发查单。打不开返回 false。 */
  openPayment(url: string): Promise<boolean>;
  /** 选取照片或视频（投诉等上传用）；取消返回 null。 */
  pickMedia(kind: MediaKind): Promise<File | null>;
  /** 开始现场录音（投诉语音）；客人未允许使用麦克风返回 null。 */
  startRecording(): Promise<AudioRecording | null>;
  /** 切到后台。 */
  onPause(listener: Listener): () => void;
  /** 回到前台。 */
  onResume(listener: Listener): () => void;
  /** 系统把客人深链交给本应用时回调原始链接。 */
  onDeepLink(listener: (url: string) => void): () => void;
  /** Android 返回键。 */
  onBackButton(listener: Listener): () => void;
  /** 退出应用（首页再按返回键）。 */
  exitApp(): void;
}

/** 系统文件选择框；capture＝直接调相机拍摄（原生壳把带 capture 的选择交给系统相机）。 */
function pickFile(kind: MediaKind, capture: boolean): Promise<File | null> {
  return new Promise((resolve) => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = `${kind}/*`;
    if (capture) input.setAttribute("capture", "environment");
    input.addEventListener("change", () => resolve(input.files?.[0] ?? null));
    input.addEventListener("cancel", () => resolve(null));
    input.click();
  });
}

/** 采样按 16 位单声道写成 wav（服务端投诉语音收 m4a/aac/mp3/amr/wav，网页录音器出的 webm 会被当成视频）。 */
function encodeWav(chunks: Float32Array[], sampleRate: number): ArrayBuffer {
  const samples = chunks.reduce((sum, chunk) => sum + chunk.length, 0);
  const view = new DataView(new ArrayBuffer(44 + samples * 2));
  const ascii = (offset: number, text: string) => [...text].forEach((char, i) => view.setUint8(offset + i, char.charCodeAt(0)));
  ascii(0, "RIFF");
  view.setUint32(4, 36 + samples * 2, true);
  ascii(8, "WAVE");
  ascii(12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true); // PCM
  view.setUint16(22, 1, true); // 单声道
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  ascii(36, "data");
  view.setUint32(40, samples * 2, true);
  let offset = 44;
  for (const chunk of chunks) {
    for (const value of chunk) {
      const clamped = Math.max(-1, Math.min(1, value));
      view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true);
      offset += 2;
    }
  }
  return view.buffer;
}

/**
 * 现场录音：经网页麦克风权限（原生壳把它转成系统麦克风权限框；iPhone 的用途说明取 Info.plist 的中文），逐段收采样，停止时写成 wav。
 * 文件名只用数字（原生请求通道按单字节写表单文件名，中文会乱码）。
 */
async function startWavRecording(): Promise<AudioRecording | null> {
  let stream: MediaStream;
  try {
    stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  } catch {
    return null;
  }
  const context = new AudioContext();
  const source = context.createMediaStreamSource(stream);
  // ponytail: ScriptProcessor 已不推荐但两端网页容器都支持；要换 AudioWorklet 时只改这里。
  const processor = context.createScriptProcessor(4096, 1, 1);
  const chunks: Float32Array[] = [];
  processor.onaudioprocess = (event) => chunks.push(new Float32Array(event.inputBuffer.getChannelData(0)));
  source.connect(processor);
  processor.connect(context.destination);
  return {
    stop: async () => {
      processor.disconnect();
      source.disconnect();
      stream.getTracks().forEach((track) => track.stop());
      await context.close();
      const name = `${new Date().toISOString().replace(/\D/g, "").slice(0, 14)}.wav`;
      return new File([encodeWav(chunks, context.sampleRate)], name, { type: "audio/wav" });
    },
  };
}

export function createWebPlatform(): CustomerPlatform {
  let token: string | null = null;
  const onVisibility = (wanted: DocumentVisibilityState) => (listener: Listener) => {
    const handler = () => {
      if (document.visibilityState === wanted) listener();
    };
    document.addEventListener("visibilitychange", handler);
    return () => document.removeEventListener("visibilitychange", handler);
  };
  return {
    session: {
      load: async () => token,
      save: async (next) => {
        token = next;
      },
      clear: async () => {
        token = null;
      },
    },
    // 浏览器调试环境没有扫码能力。
    scanCode: async () => null,
    share: async ({ link }) => {
      try {
        await navigator.clipboard.writeText(link);
        return "copied";
      } catch {
        return "unavailable";
      }
    },
    openPayment: async (url) => {
      window.open(url, "_blank", "noopener");
      return true;
    },
    pickMedia: (kind) => pickFile(kind, false),
    startRecording: startWavRecording,
    onPause: onVisibility("hidden"),
    onResume: onVisibility("visible"),
    // 浏览器里没有系统深链与返回键。
    onDeepLink: () => () => undefined,
    onBackButton: () => () => undefined,
    exitApp: () => undefined,
  };
}

const TOKEN_KEY = "customer_token";

// Android 的网页选择框只给「从文件选」，拍摄要先问；iPhone 的系统选择框自带拍摄与相册，不再另问。语音见 startWavRecording（现场录）。
const CAPTURE_SHEETS: Record<MediaKind, { title: string; capture: string }> = {
  image: { title: "添加照片", capture: "拍照" },
  video: { title: "添加视频", capture: "录像" },
};

const unsubscribe = (handle: Promise<PluginListenerHandle>) => () => void handle.then((h) => h.remove());

// Android：扫码前先经网页相机权限申请（原生壳转成系统相机权限框）；客人不允许就不进扫码界面——Android 扫码库在无权限时弹的「去设置」提示是写死的英文。
// iPhone 的扫码库自己申请系统相机权限（用途说明取 Info.plist 的中文），未允许即返回失败、不弹提示，不再先经网页申请。
async function cameraAllowed(): Promise<boolean> {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true });
    stream.getTracks().forEach((track) => track.stop());
    return true;
  } catch {
    return false;
  }
}

export function createNativePlatform(): CustomerPlatform {
  return {
    session: {
      load: () => SecureStorage.getItem(TOKEN_KEY),
      save: (token) => SecureStorage.setItem(TOKEN_KEY, token),
      clear: () => SecureStorage.removeItem(TOKEN_KEY),
    },
    scanCode: async () => {
      if (Capacitor.getPlatform() === "android" && !(await cameraAllowed())) return null;
      try {
        // 扫码插件连带一个网页端扫码库（原生壳用不到），扫码时才加载，不进首屏包。
        const { CapacitorBarcodeScanner, CapacitorBarcodeScannerTypeHint } = await import("@capacitor/barcode-scanner");
        const { ScanResult } = await CapacitorBarcodeScanner.scanBarcode({
          hint: CapacitorBarcodeScannerTypeHint.QR_CODE,
          scanInstructions: "将二维码放入框内即可自动识别",
          scanButton: false,
          cancelButtonAccessibilityLabel: "取消",
          torchButtonOnAccessibilityLabel: "打开手电筒",
          torchButtonOffAccessibilityLabel: "关闭手电筒",
        });
        return ScanResult || null;
      } catch {
        // 客人取消、未给相机权限或识别失败：返回空，由页面提示。
        return null;
      }
    },
    share: async ({ title, link }) => {
      try {
        await Share.share({ title, text: title, url: link, dialogTitle: "分享给朋友" });
        return "shared";
      } catch (err) {
        return err instanceof Error && /cancel/i.test(err.message) ? "canceled" : "unavailable";
      }
    },
    // 支付页交系统浏览器（Android 为 Custom Tabs，iPhone 为盖在应用之上的系统浏览器页）打开，客人包不在应用内加载远程网页。
    openPayment: async (url) => {
      try {
        await Browser.open({ url });
        return true;
      } catch {
        return false;
      }
    },
    pickMedia: async (kind) => {
      const sheet = CAPTURE_SHEETS[kind];
      if (Capacitor.getPlatform() !== "android") return pickFile(kind, false);
      const { index, canceled } = await ActionSheet.showActions({
        title: sheet.title,
        options: [{ title: sheet.capture }, { title: "从相册选择" }, { title: "取消", style: ActionSheetButtonStyle.Cancel }],
        cancelable: true,
      });
      if (canceled || (index !== 0 && index !== 1)) return null;
      return pickFile(kind, index === 0);
    },
    startRecording: startWavRecording,
    onPause: (listener) => unsubscribe(App.addListener("pause", listener)),
    // iPhone 的支付页盖在应用之上、应用不进后台，客人关掉它时系统不发 resume；按回到前台处理以便立即查单（Android 关掉支付页即发 resume，同口径）。
    onResume: (listener) => {
      const offs = [unsubscribe(App.addListener("resume", listener))];
      if (Capacitor.getPlatform() === "ios") offs.push(unsubscribe(Browser.addListener("browserFinished", () => listener())));
      return () => offs.forEach((off) => off());
    },
    // 冷启动的深链取启动链接；运行中收到的走 appUrlOpen。
    onDeepLink: (listener) => {
      void App.getLaunchUrl().then((launch) => {
        if (launch?.url) listener(launch.url);
      });
      return unsubscribe(App.addListener("appUrlOpen", (event) => listener(event.url)));
    },
    onBackButton: (listener) => unsubscribe(App.addListener("backButton", () => listener())),
    exitApp: () => void App.exitApp(),
  };
}

export const platform: CustomerPlatform = Capacitor.isNativePlatform() ? createNativePlatform() : createWebPlatform();
