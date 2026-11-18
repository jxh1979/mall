package com.complex.platform.dividend.dto;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import java.util.List;

/**
 * 第十八批 AQ 段第 243 条 243-R1 酒吧关停结算登记（BSM:1787「酒吧关闭须完成关停结算」）：merchantId 酒吧商户，closedBusinessDate 关店最后营业日，
 * reason 关店原因，均必填。
 */
public record DividendCloseoutRequest(String idempotencyKey, String merchantId, String closedBusinessDate, String reason) {

    /**
     * 提交财务、法务核实（BSM:1787「已确认应付及待释款项的法律性质和处理由财务、法务核实」；PGM:528「发起单据时只需指定该事项的最终审批人……需要平行线审批的
     * （如法务、财务），另行指定该条线的最终审批人」）：主线最终审批人、平行线与逐条款项的法律性质、处理方式。
     */
    public record Submit(String idempotencyKey, String finalApproverId, List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines,
                         List<Item> items) {
    }

    /** 一条款项：类别、计算单号、台账号对上列表里列出的那一条；法律性质与处理方式必填。 */
    public record Item(String itemType, String statementId, String arApItemId, String legalNature, String handling) {
    }

    /** 按核实结果登记处理（BSM:1787「相关款项须按核实结果处理并留痕后才能结案」）：处理说明必填。 */
    public record Handle(String idempotencyKey, String handledNote) {
    }
}
