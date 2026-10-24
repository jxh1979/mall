package com.complex.platform.customer;

// 文件版本：V0.01

/** ROOT-03-R23 申请列入（list）或解除（release）黑名单；原因必填留痕，项目＝申请所在综合体（定第一级批准的夜班值班经理）。 */
public record CustomerBlacklistRequestCommand(String idempotencyKey, String action, String reason, String projectId) {
}
