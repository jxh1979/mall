package com.complex.platform.access;

// 文件版本：V0.01

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * M724-F06：标在没有请求体、服务层把路径里的单号当提交编号交给统一幂等层的写接口的控制器处理方法上（调拨单发货、积分兑换领取）——
 * 请求事务切面（{@link SingleUseGrantRequestTransactionAspect}）据此把所列路径变量的值当作本次请求的提交编号，而且只取这一处：
 * 这类入口的服务层只认单号，请求体、请求头、查询串里另写的编号不参与去重。
 *
 * <p>治理地图第 656 行（V0.20）：「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减。」
 * 切面读得到这个编号，重复点同一张单才认得出是重复提交：响应提示已完成；重复提交时新用掉的单次来源随请求事务回滚、不算用掉；
 * 本人唯一那份单次来源已用掉后再点，返回第一次的结果而不是 403。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotencyKeyFromPath {

    /** 当作提交编号的路径变量名（处理方法参数上 {@code @PathVariable} 的名字）。 */
    String value();
}
