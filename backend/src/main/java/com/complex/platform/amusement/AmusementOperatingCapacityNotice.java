package com.complex.platform.amusement;

// 文件版本：V0.01

import org.springframework.stereotype.Component;

/**
 * 第十八批 CF 段第 409 条 409-R3（BSM:1267「运营容量（软上限）只是经营建议线，超过时仅告警提示、仍可放行」「「仅提示」（只在现场提示、不生成异常事件）」）：
 * 核销放行后项目在玩人数是否已达运营容量，供核销结果带回现场提示；「记录异常事件」与「仅提示」两种处置方式都提示，未配置运营容量为假。
 */
@Component
public class AmusementOperatingCapacityNotice {

    private final AmusementRepository repository;

    AmusementOperatingCapacityNotice(AmusementRepository repository) {
        this.repository = repository;
    }

    public boolean reached(String projectId) {
        AmusementOverCapacityPolicy policy = repository.findOverCapacityPolicy(projectId);
        return policy.hasOperatingCapacity() && repository.findProjectInPlayCount(projectId).orElse(0) >= policy.operatingCapacity();
    }
}
