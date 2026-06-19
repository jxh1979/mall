package com.complex.platform.gate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * R5 T2/T3 动作前置闸口链注解。标注在 Spring 托管 bean（@Service/@RestController）的 public 方法上，
 * 由 {@link ActionGateAspect} 在方法执行前依次过三道闸：
 * <ol>
 *   <li>{@link #capability()}：终端能力开关闸（T2），空=不校验。</li>
 *   <li>{@link #admissionAction()}：合同/基础定义准入闸（T3），空=不校验。</li>
 *   <li>{@link #permission()}：权限/职级闸（既有 AuthorizationService），空=不校验。</li>
 * </ol>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GuardedAction {

    /** T2：能力开关 capability_code，空=不校验开关。 */
    String capability() default "";

    /** T3：准入 action_type（与 supervisor_review_policies.action_type 对应），空=不校验准入。 */
    String admissionAction() default "";

    /** 既有权限点 permission_code，空=不校验权限。 */
    String permission() default "";

    /** T3：准入对象类型（与 supervisor_review_policies.object_type 对应）。 */
    String objectType() default "";
}
