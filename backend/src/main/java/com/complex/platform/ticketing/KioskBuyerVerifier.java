package com.complex.platform.ticketing;

// 文件版本：V0.01

/**
 * ROOT-03-R29（BSM 第 986 行「启用售票能力的一体机可由授权服务人员登录后辅助客人购票……客人用手机号短信验证绑定，在官方 App/小程序查看门票」）：
 * 一体机辅助购票时核验客人手机号短信验证码（实现在客人域，验证码一次性消费随下单事务）。
 */
public interface KioskBuyerVerifier {

    /** 核验通过返回该手机号已注册的会员编号（尚未注册返回 null，注册时再按手机号归集）；验证码不对抛 ApiException。 */
    String verify(String phone, String smsCode);
}
