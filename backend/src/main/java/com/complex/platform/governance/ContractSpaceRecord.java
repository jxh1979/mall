package com.complex.platform.governance;

import java.time.Instant;

/**
 * 合同-空间租赁绑定（contract_spaces，V080 表 + V161 project_id）。
 * ② 履约闭环 S1：合同生效时写入"该合同租了哪些空间"，准入闸 over_space 据此真实判定。
 */
public record ContractSpaceRecord(
        String contractId,
        String spaceId,
        String projectId,
        Instant createdAt
) {
}
