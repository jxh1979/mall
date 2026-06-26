// 一次性注入 合同租赁全生命周期（V162 重建）OpenAPI：16 ops + 10 requestBodies + 10 schemas。镜像 inject-leasing 范式。
// 用法：node inject-contract-openapi.mjs docs/openapi/openapi-p0.json
import { readFileSync, writeFileSync } from 'node:fs';

const path = process.argv[2];
const spec = JSON.parse(readFileSync(path, 'utf8'));

const err = { $ref: '#/components/responses/Error' };
const ok = { '200': { $ref: '#/components/responses/Success' }, '400': err, '403': err, '404': err, '409': err, '500': err };
const page = { '200': { $ref: '#/components/responses/Page' }, '403': err, '500': err };
const web = ['web'];
const idem = { type: 'string', description: '操作幂等键。', maxLength: 128 };
const str = (d, max = 255) => ({ type: 'string', description: d, maxLength: max });
const pathParam = (name, d) => ({ name, in: 'path', required: true, description: d, schema: { type: 'string', maxLength: 64 } });
const projectQuery = { name: 'project_id', in: 'query', required: false, description: '综合体 id(空=平台默认)。', schema: { type: 'string', maxLength: 64 } };

function op({ id, perm, summary, desc, idemReq, evOk, evFail, body, params, list }) {
  const o = {
    tags: ['contract'], operationId: id, summary,
    'x-permission': perm, 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': !!idemReq, 'x-audit-event-success': evOk, 'x-audit-event-failure': evFail,
    'x-auth-context': 'staff-token',
  };
  if (desc) o.description = desc;
  if (params) o.parameters = params;
  if (body) o.requestBody = { $ref: '#/components/requestBodies/' + body };
  o.responses = list ? page : ok;
  return o;
}

// ---- paths ----
spec.paths['/api/v1/contracts'] = {
  get: op({ id: 'listContracts', perm: 'contract.draft.manage', summary: '合同清单', list: true,
    evOk: 'contract.viewed', evFail: 'contract.view_failed' }),
  post: op({ id: 'createContractDraft', perm: 'contract.draft.manage', summary: '起草合同(草拟)', idemReq: true,
    desc: 'V162 重建：手工或招商签约带入。租金/递增/免租/物业费放 rent_terms；抽成 4 模式可空、能源费分摊 4 模式。状态默认 drafting。',
    body: 'ContractDraftRequest', evOk: 'contract.drafted', evFail: 'contract.draft_failed' }),
};
spec.paths['/api/v1/contracts/{contract_id}/submit'] = {
  post: op({ id: 'submitContract', perm: 'contract.draft.manage', summary: '提交进审批链(→待确认)', idemReq: true,
    desc: '按所属综合体生效链物化第一并行阶段。链未配则拒。',
    params: [pathParam('contract_id', '合同 id。')], body: 'ContractSubmitRequest',
    evOk: 'contract.submitted', evFail: 'contract.submit_failed' }),
};
spec.paths['/api/v1/contracts/{contract_id}/approve'] = {
  post: op({ id: 'approveContractStage', perm: 'contract.approve.business', summary: '审批当前阶段并行槽(会签)', idemReq: true,
    desc: '分组并行：实际权限按 body.approver_permission_code 动态校验(业务/财务/法务/总经理…)。当前阶段全过进下一阶段，任一驳回整单退回起草。',
    params: [pathParam('contract_id', '合同 id。')], body: 'ContractApproveRequest',
    evOk: 'contract.approved_slot', evFail: 'contract.approve_failed' }),
};
spec.paths['/api/v1/contracts/{contract_id}/approvals'] = {
  get: op({ id: 'listContractApprovals', perm: 'contract.draft.manage', summary: '合同审批留痕(按阶段)', list: true,
    params: [pathParam('contract_id', '合同 id。')], evOk: 'contract.approvals_viewed', evFail: 'contract.approvals_view_failed' }),
};
spec.paths['/api/v1/contracts/{contract_id}/activate'] = {
  post: op({ id: 'activateContract', perm: 'contract.lifecycle.activate', summary: '生效(固化快照+绑铺位)', idemReq: true,
    desc: '须待生效态。固化 4 项收益快照 + 绑定空间(防重复出租，重叠生效合同占用即拒)。',
    params: [pathParam('contract_id', '合同 id。')], body: 'ContractActivateRequest',
    evOk: 'contract.activated', evFail: 'contract.activate_failed' }),
};
spec.paths['/api/v1/contracts/{contract_id}/changes'] = {
  get: op({ id: 'listContractChanges', perm: 'contract.change.manage', summary: '合同变更清单', list: true,
    params: [pathParam('contract_id', '合同 id。')], evOk: 'contract.changes_viewed', evFail: 'contract.changes_view_failed' }),
  post: op({ id: 'raiseContractChange', perm: 'contract.change.manage', summary: '发起合同变更(6 类)', idemReq: true,
    desc: '6 类：续租/退租/面积/租金/经营范围/提前终止(无转租)。须 effective 合同。',
    params: [pathParam('contract_id', '合同 id。')], body: 'ContractChangeRequest',
    evOk: 'contract.change_raised', evFail: 'contract.change_raise_failed' }),
};
spec.paths['/api/v1/contracts/changes/{change_id}/approve'] = {
  post: op({ id: 'approveContractChange', perm: 'contract.approve.business', summary: '变更审批(走同一并行链,全过即应用+新版本快照)', idemReq: true,
    desc: '变更走与新签同一可配分组并行链；实际权限按 body.approver_permission_code 动态校验。全阶段通过即应用变更，任一驳回整变更退回。',
    params: [pathParam('change_id', '变更 id。')], body: 'ContractChangeApproveRequest',
    evOk: 'contract.change_approved', evFail: 'contract.change_approve_failed' }),
};
spec.paths['/api/v1/contracts/{contract_id}/settle'] = {
  post: op({ id: 'settleContract', perm: 'contract.settlement.manage', summary: '退租/到期结算(逐例顺序)', idemReq: true,
    desc: '按合同 settlement_rules 逐例算；带 dispute_reason 则挂起「有争议」等主管裁决。结清→合同终止/到期。',
    params: [pathParam('contract_id', '合同 id。')], body: 'ContractSettleRequest',
    evOk: 'contract.settled', evFail: 'contract.settle_failed' }),
};
spec.paths['/api/v1/contracts/{contract_id}/settlements'] = {
  get: op({ id: 'listContractSettlements', perm: 'contract.settlement.manage', summary: '退租结算清单', list: true,
    params: [pathParam('contract_id', '合同 id。')], evOk: 'contract.settlements_viewed', evFail: 'contract.settlements_view_failed' }),
};
spec.paths['/api/v1/contracts/settlements/{settlement_id}/resolve-dispute'] = {
  post: op({ id: 'resolveContractDispute', perm: 'contract.settlement.dispute.resolve', summary: '裁决退租争议→结清', idemReq: true,
    params: [pathParam('settlement_id', '结算 id。')], body: 'ContractResolveDisputeRequest',
    evOk: 'contract.dispute_resolved', evFail: 'contract.dispute_resolve_failed' }),
};
spec.paths['/api/v1/contracts/approval-flow'] = {
  get: op({ id: 'listContractApprovalFlow', perm: 'contract.approval.flow.manage', summary: '查审批链(生效链：项目覆盖优先)',
    params: [projectQuery], evOk: 'contract.approval_flow_viewed', evFail: 'contract.approval_flow_view_failed' }),
  post: op({ id: 'saveContractApprovalFlow', perm: 'contract.approval.flow.manage', summary: '配置审批链(分组并行,按综合体覆盖)', idemReq: true,
    desc: '同 stage_seq 多步 = 一个并行会签阶段；阶段间串行。project_id 空=改平台默认链。',
    body: 'ContractApprovalFlowRequest', evOk: 'contract.approval_flow_saved', evFail: 'contract.approval_flow_save_failed' }),
};
spec.paths['/api/v1/contracts/expiry-color-rules'] = {
  get: op({ id: 'listSpaceExpiryColorRules', perm: 'contract.space.color.manage', summary: '查到期着色分类',
    params: [projectQuery], evOk: 'contract.color_rules_viewed', evFail: 'contract.color_rules_view_failed' }),
  post: op({ id: 'saveSpaceExpiryColorRule', perm: 'contract.space.color.manage', summary: '配置到期着色分类(颜色+提前天数)', idemReq: true,
    body: 'SpaceExpiryColorRuleRequest', evOk: 'contract.color_rule_saved', evFail: 'contract.color_rule_save_failed' }),
};
spec.paths['/api/v1/contracts/space-rental-status'] = {
  get: op({ id: 'listSpaceRentalStatus', perm: 'contract.draft.manage', summary: '铺位平面图着色数据源(可租/已租/即将到期)',
    params: [projectQuery], evOk: 'contract.space_rental_viewed', evFail: 'contract.space_rental_view_failed' }),
};

// ---- requestBodies ----
const rb = (name) => ({ required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/' + name } } } });
for (const n of ['ContractDraftRequest', 'ContractSubmitRequest', 'ContractApproveRequest', 'ContractActivateRequest',
  'ContractChangeRequest', 'ContractChangeApproveRequest', 'ContractSettleRequest', 'ContractResolveDisputeRequest',
  'ContractApprovalFlowRequest', 'SpaceExpiryColorRuleRequest']) {
  spec.components.requestBodies[n] = rb(n);
}

// ---- schemas ----
const S = spec.components.schemas;
const decision = { type: 'string', description: '决议。', enum: ['approve', 'reject'] };
S.ContractDraftRequest = {
  type: 'object', description: '请求载荷 ContractDraftRequest（V162 起草合同）。', additionalProperties: false,
  required: ['merchant_id', 'contract_subject_id', 'settlement_subject_id', 'effective_date', 'expiry_date'],
  properties: {
    idempotency_key: idem, merchant_id: str('商户 id。', 64), contract_subject_id: str('合同主体 id。', 64),
    operation_subject_id: str('运营主体 id(缺省=合同主体)。', 64), settlement_subject_id: str('结算主体 id(缺省=合同主体)。', 64),
    effective_date: { type: 'string', format: 'date', description: '生效日期。' },
    expiry_date: { type: 'string', format: 'date', description: '到期日期。' },
    rent_terms: { type: 'object', description: '租金条款：基础租金/递增/免租/物业费(自由结构)。', additionalProperties: true },
    commission_mode: { type: 'string', description: '抽成模式(空=无抽成)。', enum: ['guarantee_or_commission_higher', 'fixed_ratio', 'tiered_ratio', 'guarantee_plus_commission'] },
    energy_fee_basis: { type: 'string', description: '公区能耗分摊。', enum: ['none', 'by_contract', 'by_area', 'by_usage'] },
    deposit_cent: { type: 'integer', format: 'int64', description: '保证金(分)。', minimum: 0 },
    penalty_terms: { type: 'object', description: '违约金条款。', additionalProperties: true },
    settlement_rules: { type: 'array', description: '退租结算逐例顺序(每份可不同)。', items: { type: 'object', additionalProperties: true } },
    attachment_ids: { type: 'array', description: '附件 id(线下签扫描件)。', items: { type: 'string', maxLength: 64 } },
    project_id: str('综合体 id。', 64),
  },
};
S.ContractSubmitRequest = {
  type: 'object', description: '请求载荷 ContractSubmitRequest（提交进审批链）。', additionalProperties: false,
  properties: { idempotency_key: idem },
};
S.ContractApproveRequest = {
  type: 'object', description: '请求载荷 ContractApproveRequest（审批当前阶段并行槽）。', additionalProperties: false,
  required: ['approver_permission_code', 'decision'],
  properties: { idempotency_key: idem, approver_permission_code: str('审批岗位权限码(定位并行槽)。', 120), decision, comment: str('审批意见。', 1000) },
};
S.ContractActivateRequest = {
  type: 'object', description: '请求载荷 ContractActivateRequest（生效+绑铺位）。', additionalProperties: false,
  required: ['space_ids'],
  properties: { idempotency_key: idem, space_ids: { type: 'array', description: '所租空间 id(防重复出租)。', items: { type: 'string', maxLength: 64 } } },
};
S.ContractChangeRequest = {
  type: 'object', description: '请求载荷 ContractChangeRequest（发起变更 6 类）。', additionalProperties: false,
  required: ['change_type'],
  properties: {
    idempotency_key: idem,
    change_type: { type: 'string', description: '变更类型(无转租)。', enum: ['renewal', 'move_out', 'area_change', 'rent_change', 'business_scope_change', 'early_termination'] },
    payload: { type: 'object', description: '变更载荷(改对应条款细节)。', additionalProperties: true },
  },
};
S.ContractChangeApproveRequest = {
  type: 'object', description: '请求载荷 ContractChangeApproveRequest（变更审批，走与新签同一并行链）。', additionalProperties: false,
  required: ['approver_permission_code', 'decision'],
  properties: { idempotency_key: idem, approver_permission_code: str('审批岗位权限码(定位当前阶段并行槽)。', 120), decision, comment: str('审批意见。', 1000) },
};
S.ContractSettleRequest = {
  type: 'object', description: '请求载荷 ContractSettleRequest（退租/到期结算）。', additionalProperties: false,
  required: ['trigger_type'],
  properties: {
    idempotency_key: idem,
    trigger_type: { type: 'string', description: '触发。', enum: ['move_out', 'expiry', 'early_termination'] },
    dispute_reason: str('争议原因(填则挂起有争议)。', 500),
  },
};
S.ContractResolveDisputeRequest = {
  type: 'object', description: '请求载荷 ContractResolveDisputeRequest（裁决退租争议）。', additionalProperties: false,
  properties: { idempotency_key: idem, resolution_note: str('裁决结论。', 500) },
};
S.ContractApprovalFlowRequest = {
  type: 'object', description: '请求载荷 ContractApprovalFlowRequest（配置审批链，分组并行）。', additionalProperties: false,
  required: ['steps'],
  properties: {
    idempotency_key: idem, project_id: str('综合体 id(空=平台默认链)。', 64), name: str('链名。', 160),
    steps: {
      type: 'array', description: '步骤；同 stage_seq = 一个并行会签阶段。',
      items: {
        type: 'object', additionalProperties: false, required: ['stage_seq', 'stage_name', 'approver_permission_code'],
        properties: {
          stage_seq: { type: 'integer', description: '阶段序(升序串行)。' },
          stage_name: str('阶段名。', 80), approver_permission_code: str('审批岗位权限码。', 120),
        },
      },
    },
  },
};
S.SpaceExpiryColorRuleRequest = {
  type: 'object', description: '请求载荷 SpaceExpiryColorRuleRequest（配置到期着色分类）。', additionalProperties: false,
  required: ['category_name', 'color', 'threshold_days'],
  properties: {
    idempotency_key: idem, project_id: str('综合体 id(空=平台默认)。', 64), category_name: str('分类名。', 80),
    color: str('颜色(十六进制/色名)。', 32), threshold_days: { type: 'integer', description: '到期前 N 天亮起。', minimum: 1 },
    seq: { type: 'integer', description: '排序。' },
  },
};

writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected 合同: 13 paths(17 ops) + 10 requestBodies + 10 schemas');
