#!/usr/bin/env node
// 发布口径闸(2026-07-05,codex #8):一条命令回答"能否宣称生产可信"。
// 口径 = 治理一致性(predeploy 快子集)+ PII 落库加密 + 生产阻断台账零 open。
// 诚实优先:只要还有 open 生产阻断(NAS/凭证/160/CSRF 等待外部 IT 证据),一律 NO-GO —— 不因"能联调"就谎报可发布。
// 独立于日常部署闸(联调环境部署不受此闸阻断);本闸只服务"生产发布"决策。NO-GO → exit 1。
import { execSync } from "node:child_process";
import { readFileSync } from "node:fs";

function runGate(cmd) {
  try {
    execSync(cmd, { stdio: "pipe" });
    return { ok: true };
  } catch (e) {
    return { ok: false, out: (e.stdout?.toString() || "") + (e.stderr?.toString() || "") };
  }
}

const reasons = [];

// 1) 治理一致性(scope 登记/安全基线/矩阵/契约/阻断台账同步)
const pre = runGate("npm run verify:predeploy --silent");
if (!pre.ok) reasons.push("治理一致性门禁(verify:predeploy)未过 —— 存在治理漂移");

// 2) 消费者 PII 落库加密
const pii = runGate("npm run verify:pii-encryption --silent");
if (!pii.ok) reasons.push("消费者 PII 落库加密门禁(verify:pii-encryption)未过 —— 存在明文 PII 风险");

// 3) 生产阻断台账:任何 open 阻断都 = NO-GO(诚实反映外部 IT/运维待办)
const reg = JSON.parse(readFileSync("docs/production-readiness-evidence/production-blocker-register-latest.json", "utf8"));
const c = reg.counts || {};
const openBlockers = (c.readiness_blockers || 0) + (c.credential_blockers || 0) + (c.finding_blockers || 0);
if (reg.status !== "ready" && reg.status !== "clear" && openBlockers > 0) {
  reasons.push(`生产阻断未闭环:readiness=${c.readiness_blockers || 0} 凭证轮换=${c.credential_blockers || 0} 安全findings=${c.finding_blockers || 0}(需外部 IT/运维/安全补真实证据后关闭,代码无法自证)`);
}

console.log("=== 发布口径(release-ready)===");
console.log(`治理一致性: ${pre.ok ? "GREEN" : "RED"} | PII加密: ${pii.ok ? "GREEN" : "RED"} | 生产阻断: ${openBlockers} 项 open`);
if (reasons.length === 0) {
  console.log("RELEASE_READY: GO —— 生产口径全绿,可宣称生产可信。");
  process.exit(0);
}
console.log("RELEASE_READY: NO-GO —— 不可宣称生产可信。阻断:");
for (const r of reasons) console.log(`  - ${r}`);
console.log("说明:代码/治理项可由本仓修复;生产阻断项须外部 IT/运维/安全提交真实证据(NAS 安装、凭证轮换、160 systemd 固化、CSRF 生产复核),非代码能关闭。");
process.exit(1);
