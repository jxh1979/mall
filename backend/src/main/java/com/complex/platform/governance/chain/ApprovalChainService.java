package com.complex.platform.governance.chain;

// 文件版本：V0.02

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.ApprovalLevel;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static com.complex.platform.common.Strings.requireText;

/**
 * M087 审批链引擎（PGM-1.7.1「审批链不由发起人选择档位：发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到该
 * 最终审批人之间的全部直接上级节点，逐级审批；需要平行线审批的另行指定该条线的最终审批人……向下自动补全，每个部门或业务线须预先设定
 * 其平行审批的最低起点层级」；PGM-1.10.1「审批链按……只指定最终审批人、系统自动补全中间层级……生成，不由发起人选择档位」）。
 *
 * <ul>
 *   <li>主线节点 = 发起人的直接上级链；既有档位（上一级 / 上两级 / 最高权限）只作节点数下限，最高权限在链尾追加主会话节点；</li>
 *   <li>指定最终审批人时其必须在发起人上级链上且不低于档位下限，主线 = 发起人到该人之间的全部直接上级；</li>
 *   <li>平行线自该线预设起点人沿直接上级向上补全至该线最终审批人，低于该线最低起点层级（level_rank）的节点不生成；</li>
 *   <li>specific_role 策略（R-003「退款等角色审批也走组织链」）同样走直接上级链：未指定最终审批人时为直接上级 + 链尾按角色解析的节点，
 *       指定时补全到该人（该人须经角色持有审批权限，由服务层在发起与末节点裁决时把关）；申请人与直接受益人不得成为节点
 *       （PGM-1.7.1「申请人与直接受益人不得审批与自己相关的申请」）；</li>
 *   <li>组织层级不可解析（发起人无直接上级）时不生成节点：单据保持待审且无人可裁决（fail-closed），层级补齐后可重生成。</li>
 * </ul>
 */
public class ApprovalChainService {

    private static final int MAX_DEPTH = 20;

    /** 平行线：另行指定该条线的最终审批人。 */
    public record ParallelLine(String lineCode, String finalApproverId) {
    }

    private final SupervisorReviewRepository repository;
    private final AuthorizationService authorizationService;

    public ApprovalChainService(SupervisorReviewRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    public List<ApprovalChainNode> generate(String reviewId, String initiatedBy, String beneficiaryId, ApprovalLevel level,
                                            String finalApproverId, List<ParallelLine> parallelLines) {
        List<ApprovalChainNode> nodes = new ArrayList<>();
        if (level == ApprovalLevel.SPECIFIC_ROLE) {
            // R-003（PGM-1.7.1「只需指定该事项的最终审批人，系统按组织层级自动补全全部直接上级节点」）：角色审批事项也走组织链。
            // 未指定最终审批人：直接上级 → 链尾角色节点（任一经角色持有审批权限者裁决）；指定：补全到该人，无独立角色节点；
            // 无组织层级：不生成节点（fail-closed，与档位链一致）。
            List<String> path = superiorPath(initiatedBy, initiatedBy, beneficiaryId);
            boolean designated = finalApproverId != null && !finalApproverId.isBlank();
            List<String> main;
            if (designated) {
                int index = path.indexOf(finalApproverId);
                if (index < 0) {
                    throw ApiException.badRequest("FINAL_APPROVER_NOT_IN_CHAIN", "指定的最终审批人不在发起人的直接上级链上");
                }
                main = path.subList(0, index + 1);
            } else {
                main = path.subList(0, Math.min(1, path.size()));
            }
            int seq = 1;
            for (String approver : main) {
                nodes.add(node(reviewId, ApprovalChainNode.MAIN_LINE, seq++, ApprovalChainNode.KIND_USER, approver));
            }
            if (!designated && !main.isEmpty()) {
                nodes.add(node(reviewId, ApprovalChainNode.MAIN_LINE, seq, ApprovalChainNode.KIND_ROLE, null));
            }
        } else {
            List<String> path = superiorPath(initiatedBy, initiatedBy, beneficiaryId);
            int floor = level == ApprovalLevel.ONE_LEVEL_UP ? 1 : level == ApprovalLevel.TWO_LEVELS_UP ? 2 : path.size();
            List<String> main;
            if (finalApproverId != null && !finalApproverId.isBlank()) {
                int index = path.indexOf(finalApproverId);
                if (index < 0) {
                    throw ApiException.badRequest("FINAL_APPROVER_NOT_IN_CHAIN", "指定的最终审批人不在发起人的直接上级链上");
                }
                if (level != ApprovalLevel.TOP_AUTHORITY && index + 1 < Math.min(floor, path.size())) {
                    throw ApiException.badRequest("FINAL_APPROVER_BELOW_POLICY_MINIMUM", "指定的最终审批人低于该事项要求的审批层级下限");
                }
                main = path.subList(0, index + 1);
            } else {
                main = path.subList(0, Math.min(floor, path.size()));
            }
            int seq = 1;
            for (String approver : main) {
                nodes.add(node(reviewId, ApprovalChainNode.MAIN_LINE, seq++, ApprovalChainNode.KIND_USER, approver));
            }
            if (level == ApprovalLevel.TOP_AUTHORITY) {
                nodes.add(node(reviewId, ApprovalChainNode.MAIN_LINE, seq, ApprovalChainNode.KIND_MASTER, null));
            }
        }
        for (ParallelLine line : parallelLines == null ? List.<ParallelLine>of() : parallelLines) {
            nodes.addAll(parallelNodes(reviewId, line, initiatedBy, beneficiaryId));
        }
        return nodes;
    }

    /** 自 start 向上的直接上级序列（不含 start；跳过申请人与直接受益人；无上级或成环即到顶）。 */
    List<String> superiorPath(String start, String initiatedBy, String beneficiaryId) {
        List<String> path = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        seen.add(start);
        String current = start;
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            Optional<String> superior = current == null ? Optional.empty() : repository.findDirectSuperior(current);
            if (superior.isEmpty() || !seen.add(superior.get())) {
                break;
            }
            current = superior.get();
            if (!Objects.equals(current, initiatedBy) && !Objects.equals(current, beneficiaryId)) {
                path.add(current);
            }
        }
        return path;
    }

    private List<ApprovalChainNode> parallelNodes(String reviewId, ParallelLine line, String initiatedBy, String beneficiaryId) {
        String code = requireText(line == null ? null : line.lineCode(), "line_code");
        SupervisorReviewRepository.ApprovalLine settings = repository.findApprovalLine(code)
                .filter(l -> "active".equalsIgnoreCase(l.status()))
                .orElseThrow(() -> ApiException.badRequest("APPROVAL_LINE_NOT_CONFIGURED", "平行审批线未配置或已停用：" + code));
        String finalId = requireText(line.finalApproverId(), "final_approver_id");
        List<String> chain;
        if (settings.entryUserId() == null || settings.entryUserId().isBlank()) {
            chain = List.of(finalId);
        } else {
            List<String> candidates = new ArrayList<>();
            candidates.add(settings.entryUserId());
            candidates.addAll(superiorPath(settings.entryUserId(), null, null));
            int end = candidates.indexOf(finalId);
            if (end < 0) {
                throw ApiException.badRequest("FINAL_APPROVER_NOT_IN_CHAIN", "该线最终审批人不在起点人的直接上级链上：" + code);
            }
            chain = candidates.subList(0, end + 1);
        }
        List<ApprovalChainNode> nodes = new ArrayList<>();
        int seq = 1;
        for (String approver : chain) {
            if (approver.equals(initiatedBy) || approver.equals(beneficiaryId)) {
                continue;
            }
            int rank = authorizationService.levelRankOf(approver);
            if (rank > 0 && rank < settings.minStartRank()) {
                continue; // 低于该线预设最低起点层级的不生成节点
            }
            nodes.add(node(reviewId, code, seq++, ApprovalChainNode.KIND_USER, approver));
        }
        if (nodes.isEmpty()) {
            throw ApiException.badRequest("APPROVAL_LINE_EMPTY", "平行审批线无可用节点：" + code);
        }
        return nodes;
    }

    /** 每条线当前待裁决的节点（该线序号最小的 pending 节点）。 */
    public static List<ApprovalChainNode> currentNodes(List<ApprovalChainNode> nodes) {
        Map<String, ApprovalChainNode> current = new LinkedHashMap<>();
        for (ApprovalChainNode node : nodes) {
            if (node.pending()) {
                current.merge(node.lineCode(), node, (a, b) -> a.seq() <= b.seq() ? a : b);
            }
        }
        return new ArrayList<>(current.values());
    }

    /** 主线与各平行线全部节点通过（PGM-1.7.1「全部通过……才生效」）。 */
    public static boolean allApproved(List<ApprovalChainNode> nodes) {
        return !nodes.isEmpty() && nodes.stream().allMatch(node -> ApprovalChainNode.APPROVED.equals(node.status()));
    }

    private static ApprovalChainNode node(String reviewId, String lineCode, int seq, String kind, String approverId) {
        return new ApprovalChainNode("node_" + UUID.randomUUID(), reviewId, lineCode, seq, kind, approverId,
                ApprovalChainNode.PENDING, null, null, null, null, 0);
    }
}
