// 文件版本：V0.02
// 写请求的请求编号（幂等键）跟着「一次操作」走：实现已移到三端共用的客人业务模块 packages/customer-core，后台从这里转出、用法不变。
export * from "@complex/customer-core/operationKey";
