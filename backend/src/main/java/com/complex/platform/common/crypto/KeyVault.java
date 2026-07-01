package com.complex.platform.common.crypto;

import javax.crypto.SecretKey;

/**
 * 按用途取数据加密密钥。运行态密钥来源可插拔：当前为本地 provision（{@link LocalKeyVault}，模型①——
 * 安装期由 U 盘写入本地/环境，运行态不依赖 U 盘）；后续 U 盘 provider 可无缝替换此接口实现。
 */
public interface KeyVault {

    /** 返回指定用途的 AES 密钥（如 "pii"）。缺失/弱密钥（prod）应 fail-closed 抛错。 */
    SecretKey dataKey(String purpose);
}
