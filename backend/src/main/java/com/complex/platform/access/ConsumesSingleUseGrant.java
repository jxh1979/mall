package com.complex.platform.access;

// 文件版本：V0.01

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * M723：标在查看请求（GET / HEAD）的控制器处理方法上——列出的权限码在这个接口上仍然看一次（导出一次）扣一次单次授权，
 * 与 M723 之前的行为相同；同一请求里核验的其他权限码照「查看不算一次使用」不扣。扣减并入请求事务，成功才扣。
 *
 * <p>操作员 2026-10-03 答复：导出「算用掉，导一次扣一次」（治理地图写导出「须单独授权、逐次留痕」，想再导就要再申请一次）；
 * 敏感明文查看「算用掉，看一次扣一次」（敏感信息的明文查看按次授权，想再看就要再申请一次；普通的商户列表（脱敏）仍然不扣）。
 * 操作员 2026-10-04 答复：核销预检附带显示的人脸照「不算用掉」（取人脸照的接口不标，与其他查看请求一样不扣）。
 * 写请求不需要本标记（本来就扣）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsumesSingleUseGrant {

    /** 在本接口上仍然扣单次授权的权限码。 */
    String[] value();
}
