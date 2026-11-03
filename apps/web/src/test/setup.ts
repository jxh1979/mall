import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// jsdom 不实现 matchMedia；antd 响应式组件（Layout.Sider、Grid 等）依赖它。
if (typeof window !== "undefined" && typeof window.matchMedia !== "function") {
  window.matchMedia = (query: string): MediaQueryList =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => undefined,
      removeListener: () => undefined,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false,
    }) as MediaQueryList;
}

// 第十八批 N 段第 123 条 123-R3（BSM:1876「使用 Web 端必须打开地理识别」）：jsdom 没有定位接口，默认按已授权、能取到位置模拟，
// 既有用例不受网页端整页准入影响；个别用例用 vi.spyOn 改授权状态或位置。
if (typeof navigator !== "undefined" && !navigator.geolocation) {
  Object.defineProperty(navigator, "geolocation", {
    configurable: true,
    value: {
      getCurrentPosition: (success: PositionCallback) =>
        success({ coords: { latitude: 31.2304, longitude: 121.4737, accuracy: 10 }, timestamp: Date.now() } as GeolocationPosition),
      watchPosition: () => 0,
      clearWatch: () => undefined,
    },
  });
}
if (typeof navigator !== "undefined" && !navigator.permissions) {
  Object.defineProperty(navigator, "permissions", {
    configurable: true,
    value: { query: async () => ({ state: "granted", onchange: null }) as unknown as PermissionStatus },
  });
}

afterEach(() => {
  cleanup();
});
