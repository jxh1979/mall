export const switchSourcePath = "docs/integration-capability-switches-latest.json";

export const capabilitySwitchDefinitions = [
  {
    capability_id: "aggregate_payment",
    label: "聚合支付",
    default_enabled: false,
    category: "payment",
    evidence_item_ids: ["PRD-PAY-001", "PRD-PAY-002"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-17", "REQ-21", "REQ-25"],
    disabled_behavior: "关闭后不读取聚合支付接口，不要求商户号、回调、退款或对账文件；支付仅允许现金、线下 POS 刷卡或人工收款确认，并写入审计留痕。",
    enabled_requirements: "开启后必须补齐商户号、回调验签、退款权限、对账文件、credential_ref、失败补偿和生产证据。",
  },
  {
    capability_id: "groupbuy_ticket_channel",
    label: "团购/渠道售票",
    default_enabled: false,
    category: "ticket_channel",
    evidence_item_ids: ["PRD-3P-001"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-40"],
    disabled_behavior: "关闭后不接美团/大众点评/抖音等渠道订单、票码、退款和对账接口；仅支持平台自出票和后台手动补录渠道信息。",
    enabled_requirements: "开启后必须补齐渠道 API/SDK、券码核销、订单回流、退款同步、对账口径和生产证据。",
  },
  {
    capability_id: "wecom",
    label: "企业微信",
    default_enabled: false,
    category: "wecom",
    evidence_item_ids: ["PRD-WECOM-001"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: [],
    disabled_behavior: "关闭后不读取企业微信通讯录、会话存档、客户联系或回调；员工账号由后台手动录入，质检仅使用平台内数据。",
    enabled_requirements: "开启后必须补齐企业微信应用、通讯录、会话存档、回调、同步任务、告警和 credential_ref。",
  },
  {
    capability_id: "bar_face_age",
    label: "酒吧人脸/年龄核验",
    default_enabled: false,
    category: "face_age",
    evidence_item_ids: ["PRD-FACE-001"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-17", "REQ-23b"],
    disabled_behavior: "关闭后不调用第三方人脸/年龄接口；酒吧入场必须走人工证件核验、人工复核和审计留痕，不能宣称已完成自动人脸核验。",
    enabled_requirements: "开启后必须补齐供应商 SDK/API、成功/失败样例、错误码、合规材料、测试账号和 credential_ref。",
  },
  {
    capability_id: "managed_work_devices",
    label: "工作手机/平板/一体机 MDM 纳管",
    default_enabled: false,
    category: "MDM",
    evidence_item_ids: ["PRD-MDM-001"],
    credential_item_ids: [],
    security_finding_ids: ["SEC-P0-MDM-DEVICE-001"],
    requirement_ids: ["REQ-44"],
    disabled_behavior: "关闭后不允许工作手机、平板、一体机执行高风险核销、出酒、支付相关动作；相关操作只能在受控后台或人工流程内完成。",
    enabled_requirements: "开启后必须补齐 Android Enterprise/商业 MDM 选型、enroll、策略下发、远程锁定/擦除测试和设备绑定证据。",
  },
  {
    capability_id: "object_storage",
    label: "对象存储/附件证据桶",
    default_enabled: false,
    category: "object_storage",
    evidence_item_ids: ["PRD-CRED-OBJ-001"],
    credential_item_ids: ["PRD-CRED-OBJ-001"],
    security_finding_ids: [],
    requirement_ids: ["REQ-17", "REQ-32"],
    disabled_behavior: "关闭后不要求 MinIO/S3 等对象存储接口；附件和证据只允许记录外部受控证据编号或手工 evidence_link，不提供平台内文件上传/签名 URL 能力。",
    enabled_requirements: "开启后必须补齐桶、权限、Access Key 引用、上传下载、签名 URL、备份和轮换证据。",
  },
  {
    capability_id: "open_platform",
    label: "开放平台/第三方应用接入",
    default_enabled: false,
    category: "open_platform",
    evidence_item_ids: ["PRD-CRED-OPEN-001", "PRD-3P-001"],
    credential_item_ids: ["PRD-CRED-OPEN-001", "PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-40"],
    disabled_behavior: "关闭后不开放第三方应用 app_key/app_secret、动态 endpoint、Webhook 入站或供应商主动调用平台；仅保留内部 Adapter/mock。",
    enabled_requirements: "开启后必须补齐开放应用、allowed scopes、签名密钥、nonce、防重放、Webhook 验签和轮换证据。",
  },
  {
    capability_id: "catering_pos",
    label: "餐饮 POS",
    default_enabled: false,
    category: "catering",
    evidence_item_ids: ["PRD-3P-001"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-40"],
    disabled_behavior: "关闭后不接餐饮 POS 点单、收银、券核销、退款、订单回流或对账接口；餐饮相关数据只支持后台手工登记或不启用。",
    enabled_requirements: "开启后必须补齐餐饮 POS API/SDK、订单/退款/券核销字段、Webhook/轮询、对账文件和 credential_ref。",
  },
  {
    capability_id: "retail_pos",
    label: "零售 POS",
    default_enabled: false,
    category: "retail",
    evidence_item_ids: ["PRD-3P-001"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-40"],
    disabled_behavior: "关闭后不接零售 POS 商品/SKU、收银、券核销、退款、订单回流或对账接口；零售相关数据只支持后台手工登记或不启用。",
    enabled_requirements: "开启后必须补齐零售 POS API/SDK、SKU/订单/退款/券核销字段、Webhook/轮询、对账文件和 credential_ref。",
  },
  {
    capability_id: "robot_platform",
    label: "机器人平台",
    default_enabled: false,
    category: "robot",
    evidence_item_ids: ["PRD-3P-001"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-44"],
    disabled_behavior: "关闭后不下发机器人任务、不接机器人状态、不要求机器人 API；配送和交接按人工流程留痕。",
    enabled_requirements: "开启后必须补齐机器人任务 API、状态回调、签名、异常补偿、测试账号和生产证据。",
  },
  {
    capability_id: "video_platform",
    label: "视频监控平台",
    default_enabled: false,
    category: "video",
    evidence_item_ids: ["PRD-3P-001"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-32", "REQ-41"],
    disabled_behavior: "关闭后不读取摄像头、录像、截图、片段链接或播放 token；系统只记录人工 evidence_link，不做视频内嵌播放或自动关联。",
    enabled_requirements: "开启后必须补齐摄像头点位、播放鉴权、截图/片段接口、事件关联、审计回调和 credential_ref。",
  },
  {
    capability_id: "gate_nfc_scan_device",
    label: "闸机/NFC/扫码设备",
    default_enabled: false,
    category: "device",
    evidence_item_ids: ["PRD-3P-001"],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: ["SEC-P1-WEBHOOK-REPLAY-001"],
    requirement_ids: ["REQ-44"],
    disabled_behavior: "关闭后不接闸机、NFC 读卡器、扫码枪或设备网关接口；票务/权益核销只能走后台或人工扫码录入。",
    enabled_requirements: "开启后必须补齐设备协议/API、核销校验、开闸结果、离线队列、设备事件、签名和补偿证据。",
  },
  {
    capability_id: "local_ai_llm",
    label: "160 本地 AI 大模型",
    default_enabled: true,
    category: "ai",
    evidence_item_ids: [],
    credential_item_ids: [],
    security_finding_ids: [],
    requirement_ids: [],
    disabled_behavior: "关闭后只使用 Java 内置规则处理器。",
    enabled_requirements: "开启后使用 160 本地大模型作为辅助分析，不调用外部云 AI，不允许 AI 直接做付款、核销、处罚、退款或员工绩效最终决策。",
  },
  {
    capability_id: "nas_smb_backup",
    label: "NAS SMB 备份恢复",
    default_enabled: true,
    category: "nas",
    evidence_item_ids: ["PRD-CRED-NAS-001", "PRD-NAS-AUTOMOUNT-001", "PRD-NAS-SCHEDULE-001", "PRD-NAS-ALERT-001", "PRD-NAS-DRILL-CADENCE-001"],
    credential_item_ids: ["PRD-CRED-NAS-001"],
    security_finding_ids: ["SEC-P0-OPS-RECOVERY-001"],
    requirement_ids: [],
    disabled_behavior: "不建议关闭。若关闭，必须先指定等效备份恢复目标；否则不能宣称生产可恢复。",
    enabled_requirements: "开启后必须固化 //192.168.88.113/bn 到 160 本地挂载点、定时备份、容量/失败告警和周期性恢复演练。",
  },
  {
    capability_id: "sms_notification",
    label: "短信通知",
    default_enabled: false,
    category: "notification",
    evidence_item_ids: [],
    credential_item_ids: ["PRD-CRED-3P-001"],
    security_finding_ids: [],
    requirement_ids: [],
    disabled_behavior: "关闭后不调用真实短信供应商；登录验证码走 mock OTP loopback（非生产固定码 000000），生产无供应商时 fail-closed 拒绝登录；催缴、告警等短信能力降级为人工或站内留痕。",
    enabled_requirements: "开启后必须补齐短信供应商签名/模板报备、API/SDK、credential_ref、发送回执、限频与失败补偿和生产证据。",
  },
];

export function defaultSwitchState() {
  return Object.fromEntries(capabilitySwitchDefinitions.map((item) => [item.capability_id, item.default_enabled]));
}

export function disabledSwitches(state = defaultSwitchState()) {
  return capabilitySwitchDefinitions.filter((item) => state[item.capability_id] === false);
}

export function enabledSwitches(state = defaultSwitchState()) {
  return capabilitySwitchDefinitions.filter((item) => state[item.capability_id] === true);
}

export function disabledItemIds(state = defaultSwitchState()) {
  const enabledIds = new Set(enabledSwitches(state).flatMap((item) => [...item.evidence_item_ids, ...item.credential_item_ids]));
  return new Set(disabledSwitches(state)
    .flatMap((item) => [...item.evidence_item_ids, ...item.credential_item_ids])
    .filter((itemId) => !enabledIds.has(itemId)));
}

export function disabledFindingIds(state = defaultSwitchState()) {
  const enabledIds = new Set(enabledSwitches(state).flatMap((item) => item.security_finding_ids));
  return new Set(disabledSwitches(state)
    .flatMap((item) => item.security_finding_ids)
    .filter((findingId) => !enabledIds.has(findingId)));
}

export function disabledRequirementIds(state = defaultSwitchState()) {
  return new Set(disabledSwitches(state).flatMap((item) => item.requirement_ids));
}

export function switchReasonsByItemId(state = defaultSwitchState()) {
  const reasons = new Map();
  for (const item of disabledSwitches(state)) {
    for (const itemId of [...item.evidence_item_ids, ...item.credential_item_ids]) {
      if (!reasons.has(itemId)) reasons.set(itemId, []);
      reasons.get(itemId).push(`${item.label} 关闭：${item.disabled_behavior}`);
    }
  }
  return reasons;
}

export function switchReasonsByFindingId(state = defaultSwitchState()) {
  const reasons = new Map();
  for (const item of disabledSwitches(state)) {
    for (const findingId of item.security_finding_ids) {
      if (!reasons.has(findingId)) reasons.set(findingId, []);
      reasons.get(findingId).push(`${item.label} 关闭：${item.disabled_behavior}`);
    }
  }
  return reasons;
}

export function filterEvidenceItemIdsForSwitches(itemIds, state = defaultSwitchState()) {
  const disabled = disabledItemIds(state);
  return itemIds.filter((itemId) => !disabled.has(itemId));
}

export function capabilitySwitchDocument() {
  const state = defaultSwitchState();
  return {
    switch_matrix_version: "v0.1",
    policy: {
      default_external_integrations_off: true,
      disabled_means_no_interface_required: true,
      enabled_means_real_interface_and_evidence_required: true,
      disabled_capability_must_have_business_fallback: true,
      local_ai_llm_replaces_external_ai_for_now: true,
      nas_backup_remains_required_until_equivalent_recovery_target_exists: true,
    },
    switches: capabilitySwitchDefinitions.map((item) => ({
      ...item,
      enabled: state[item.capability_id],
      production_interface_required: state[item.capability_id] === true && item.capability_id !== "local_ai_llm",
    })),
  };
}
