// 文件版本：V0.01
// 三端（Android/iPhone 官方客人 App、微信小程序）共用的客人业务模块入口（BSM「客人端包括 App 和小程序，两者同步建设。
// 订单、票、权益、二维码、积分摘要、等级和订台记录必须是一套口径」）：纯 TS，不依赖 DOM/window/fetch，请求由各端注入。
export * from "./apiError";
export * from "./client";
export * from "./dynamicCode";
export * from "./links";
export * from "./operationKey";
export * from "./payments";
export * from "./service";
export * from "./statusLabels";
export * from "./switches";
export * from "./types";
