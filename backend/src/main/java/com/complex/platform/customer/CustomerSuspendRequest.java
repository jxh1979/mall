package com.complex.platform.customer;

// 文件版本：V0.01

/** ROOT-03-R23 置入受限请求：project_id＝按哪个项目的受限时长算截止（项目账号不传即本项目）；reason 必填留痕。 */
public record CustomerSuspendRequest(String projectId, String reason) {
}
