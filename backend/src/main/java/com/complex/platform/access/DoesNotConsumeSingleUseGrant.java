package com.complex.platform.access;

// 文件版本：V0.01

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * M723：标在以提交方式（POST 等非 GET / HEAD）实现的预览、查询接口的控制器处理方法上——这个接口只预览或只查询、不改业务数据（可以落幂等记录、留痕与试运行台账），
 * 权限核验照常，但不消耗单次授权（与查看请求同样处理），真正执行的那个接口才消耗。与 {@link ConsumesSingleUseGrant} 对称。
 *
 * <p>操作员 2026-10-03 答复：「预览、查询不扣一次性授权，只有真正核销、真正执行的那一次才扣。李四可以先预览再核销。」
 * 操作员 2026-10-04 答复：价格仿真「不算用掉」（动态定价价格仿真接口据此标记）。
 * 只标确属预览、查询的接口；拿不准的不标（照旧消耗）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DoesNotConsumeSingleUseGrant {
}
