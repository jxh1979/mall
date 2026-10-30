// 文件版本：V0.01
// 商户手机应用入口（第 25 条；ENUM「终端十一端：……商户手机应用……」、BSM 商户 App「商户侧的手机端」）：
// 以商户手机应用模式挂 apps/web 的后台外壳 AdminApp（不复制页面）；令牌与设备编号只存 App 的系统安全存储（Android Keystore 加密、
// iPhone 钥匙串），不写 localStorage；退出与会话过期只清令牌、设备编号保留（第十八批裁决 19「卸载重装或清除应用数据算新设备」）。
import { SecureStorage } from "@aparajita/capacitor-secure-storage";
import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";

import AdminApp from "../../web/src/admin/AdminApp";
import { configureMerchantAppMode, type MerchantAppStore, type MerchantAppTokens } from "../../web/src/admin/client/adminClient";
import "../../web/src/styles.css";

const INSTALLED_KEY = "merchant_app_installed";
const SESSION_KEY = "merchant_app_session";
const DEVICE_KEY = "merchant_app_device_no";

async function readSecure(key: string): Promise<string | null> {
  try {
    return await SecureStorage.getItem(key);
  } catch {
    return null;
  }
}

function parseTokens(raw: string | null): MerchantAppTokens | null {
  try {
    const value = raw ? (JSON.parse(raw) as MerchantAppTokens | null) : null;
    return value?.accessToken ? value : null;
  } catch {
    return null;
  }
}

async function start(): Promise<void> {
  // iPhone 卸载应用不清钥匙串：没有安装标记（新装，或清除了应用数据——标记随之清掉）就先清空安全存储，按新设备处理（裁决 19）。
  let installed = false;
  try {
    installed = window.localStorage.getItem(INSTALLED_KEY) !== null;
  } catch {
    installed = false;
  }
  if (!installed) {
    await SecureStorage.clear().catch(() => undefined);
    try {
      window.localStorage.setItem(INSTALLED_KEY, "1");
    } catch {
      // 写不进标记只会让下次启动再清一次，不影响本次使用。
    }
  }

  let tokens = parseTokens(await readSecure(SESSION_KEY));
  let deviceId = await readSecure(DEVICE_KEY);
  const store: MerchantAppStore = {
    tokens: () => tokens,
    deviceId: () => deviceId,
    save: async (next, nextDeviceId) => {
      tokens = next;
      await SecureStorage.setItem(SESSION_KEY, JSON.stringify(next));
      if (nextDeviceId) {
        deviceId = nextDeviceId;
        await SecureStorage.setItem(DEVICE_KEY, nextDeviceId);
      }
    },
    clearTokens: async () => {
      tokens = null;
      await SecureStorage.removeItem(SESSION_KEY);
    },
  };
  configureMerchantAppMode(store, import.meta.env.VITE_MERCHANT_APP_VERSION as string);

  const rootElement = document.getElementById("root");
  if (!rootElement) throw new Error("找不到页面根节点 #root");
  createRoot(rootElement).render(
    <React.StrictMode>
      <AdminApp />
    </React.StrictMode>,
  );
}

void start();
