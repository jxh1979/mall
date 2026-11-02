// 文件版本：V0.02
// 投诉（照片/视频/语音上传）与满意度（BSM「投诉与反馈：客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」；BSM「满意度在回访/验收时
// 由提单方评分」）：附件随文字一次提交（OpenAPI multipart，幂等键头与字段同值）；写了处理结论、尚未评过的才可评 1～5 分。
// 学生认证（BSM「学生认证必须在本人注册会员后单独申请」；BSM「采集证件、人脸、支付账号、精确位置等敏感信息前先单独取得同意，并留同意版本、时间和范围」；
// 第十三批裁决第 1 条「有效到客人提供的学信网《学籍在线验证报告》上的预计毕业日期，审核人通过时按报告填写」）：先单独同意采集证件，才可选证件照片。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, header, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const complaint = (over) => ({
  work_order_id: "wo1", code: "TS-001", title: "包厢空调不制冷", description: null, status: "pending_visit", status_label: "待回访",
  submitted_at: "2026-10-24T08:00:00Z", resolution: null, satisfaction: null, media_count: 0, ...over,
});

/** multipart 原文里名为 name 的文本字段值。 */
function field(form, name) {
  return new RegExp(`name="${name}"\\r\\n\\r\\n([^\\r]*)\\r\\n`).exec(form)?.[1];
}

beforeEach(() => freshModules());

test("complaintUploadsPhotoVideoVoice", async () => {
  const { wx, requests } = installWx({
    "GET /api/v1/customer/projects": ok([{ project_id: "p1", project_name: "长沙综合体", address: null, intro: null, announcement: null, business_hours: [] }]),
    "GET /api/v1/customer/complaints": ok([]),
    "POST /api/v1/customer/complaints": ok(complaint({ media_count: 3 })),
  }, TOKEN);
  const page = mount("pages/complaints/index");
  await flush();
  fire(page, ["#complaint-title"], "input", { value: "包厢空调不制冷" });
  fire(page, ["#complaint-description"], "input", { value: "晚上十点后一直不制冷" });
  fire(page, ["#add-image"], "tap");
  fire(page, ["#add-video"], "tap");
  fire(page, ["#record-start"], "tap");
  await flush();
  expect(wx.getRecorderManager().start).toHaveBeenCalledWith(expect.objectContaining({ format: "mp3", duration: 600000 }));
  fire(page, ["#record-stop"], "tap");
  await flush();
  const files = text(q(page, "#files"));
  for (const name of ["image-1.jpg", "video-1.mp4", "voice-1.mp3"]) expect(files).toContain(name);
  fire(page, ["#complaint-submit"], "tap");
  await flush();
  const req = requests.find((r) => r.key === "POST /api/v1/customer/complaints");
  expect(req).toBeTruthy();
  expect(header(req, "content-type")).toMatch(/^multipart\/form-data; boundary=/);
  expect(field(req.form, "project_id")).toBe("p1");
  // PGM「可以按全局、综合体……开关」：投诉请求带所选综合体，后端按综合体拒绝。
  expect(header(req, "x-project-id")).toBe("p1");
  expect(field(req.form, "title")).toBe("包厢空调不制冷");
  expect(field(req.form, "description")).toBe("晚上十点后一直不制冷");
  expect(field(req.form, "idempotency_key")).toBe(header(req, "idempotency-key"));
  for (const name of ["image-1.jpg", "video-1.mp4", "voice-1.mp3"]) {
    expect(req.form).toContain(`name="files"; filename="${name}"`);
    expect(req.form).toContain(`文件:http://tmp/${name}`);
  }
  expect(text(q(page, "#complaint-submitted"))).toContain("投诉已提交，编号 TS-001");
});

test("recordingFailureLeavesRecordingStateAndSaysWhy", async () => {
  const { wx } = installWx({ "GET /api/v1/customer/projects": ok([]), "GET /api/v1/customer/complaints": ok([]) }, TOKEN);
  const page = mount("pages/complaints/index");
  await flush();
  fire(page, ["#record-start"], "tap");
  await flush();
  expect(q(page, "#record-stop")).not.toBeNull();
  const recorder = wx.getRecorderManager();
  expect(recorder.errorListener, "未登记录音失败回调").toBeTypeOf("function");
  recorder.errorListener({ errMsg: "operateRecorder:fail auth deny" });
  await flush();
  expect(q(page, "#record-start")).not.toBeNull();
  expect(text(q(page, "#complaint-error"))).toContain("录音失败，请允许使用麦克风后重试");
});

// 第十八批 M 段第 111 条 111-R4（第十八批裁决 28「客人投诉时自己选涉及的门店（选填），客户关怀部可以更正」）：列所选综合体的在营门店，
// 选了随投诉一并提交。
test("complaintStoreOptionalAndSubmitted", async () => {
  const { requests } = installWx({
    "GET /api/v1/customer/projects": ok([{ project_id: "p1", project_name: "长沙综合体", address: null, intro: null, announcement: null, business_hours: [] }]),
    "GET /api/v1/customer/complaints": ok([]),
    "GET /api/v1/customer/complaints/store-options": ok([{ merchant_id: "m1", merchant_name: "一号清吧" }]),
    "POST /api/v1/customer/complaints": ok(complaint()),
  }, TOKEN);
  const page = mount("pages/complaints/index");
  await flush();
  expect(q(page, "#store-picker")).not.toBeNull();
  expect(requests.find((r) => r.key === "GET /api/v1/customer/complaints/store-options")?.path).toContain("project_id=p1");
  fire(page, ["#store-picker"], "change", { value: "1" });
  await flush();
  expect(text(q(page, "#store-picker"))).toContain("一号清吧");
  fire(page, ["#complaint-title"], "input", { value: "服务差" });
  fire(page, ["#complaint-submit"], "tap");
  await flush();
  const req = requests.find((r) => r.key === "POST /api/v1/customer/complaints");
  expect(field(req.form, "store_merchant_id")).toBe("m1");
});

test("rateResolvedComplaintOnce", async () => {
  const { requests } = installWx({
    "GET /api/v1/customer/projects": ok([]),
    "GET /api/v1/customer/complaints": ok([
      complaint({ resolution: "已更换空调" }),
      complaint({ work_order_id: "wo2", code: "TS-002", status: "processing", status_label: "处理中" }),
      complaint({ work_order_id: "wo3", code: "TS-003", status: "closed", status_label: "已关闭", resolution: "已退款", satisfaction: 4 }),
    ]),
    "POST /api/v1/customer/complaints/wo1/satisfaction": ok(complaint({ resolution: "已更换空调", satisfaction: 5 })),
  }, TOKEN);
  const page = mount("pages/complaints/index");
  await flush();
  expect(text(q(page, "#complaint-wo1"))).toContain("处理结论：已更换空调");
  expect(q(page, "#rate-wo2-5")).toBeNull();
  expect(text(q(page, "#complaint-wo2"))).toContain("处理中，有结论后可评价");
  expect(text(q(page, "#complaint-wo3"))).toContain("满意度 4 分");
  expect(q(page, "#rate-wo3-5")).toBeNull();
  fire(page, ["#rate-wo1-5"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/complaints/wo1/satisfaction")?.body).toMatchObject({ score: 5 });
  expect(requests.filter((r) => r.key === "GET /api/v1/customer/complaints")).toHaveLength(2);
});

test("studentConsentRecordedBeforePhotosThenSubmits", async () => {
  const { requests } = installWx({
    "GET /api/v1/customer/student-certifications/current": ok(null),
    "POST /api/v1/customer/consents": ok({ id: "cs1", consent_type: "student_certificate", accepted: true, version: "学生证件采集同意 V0.01", accepted_at: "2026-10-24T08:00:00Z" }),
    "POST /api/v1/customer/student-certifications": ok({ id: "sc1", status: "pending", status_label: "待审核", valid_until: null, reject_reason: null,
      submitted_at: "2026-10-24T08:01:00Z", reviewed_at: null }),
  }, TOKEN);
  const page = mount("pages/student/index");
  await flush();
  expect(text(q(page, "#student-consent"))).toContain("学生证件照片与学信网《学籍在线验证报告》在线验证码");
  expect(q(page, "#add-photo")).toBeNull();
  fire(page, ["#consent-agree"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/consents")?.body)
    .toMatchObject({ consent_type: "student_certificate", accepted: true, version: "学生证件采集同意 V0.01" });
  fire(page, ["#add-photo"], "tap");
  await flush();
  expect(text(q(page, "#student-files"))).toContain("image-1.jpg");
  fire(page, ["#chsi-code"], "input", { value: "CHSI123" });
  fire(page, ["#student-submit"], "tap");
  await flush();
  const req = requests.find((r) => r.key === "POST /api/v1/customer/student-certifications");
  expect(field(req.form, "consent_id")).toBe("cs1");
  expect(field(req.form, "chsi_verification_code")).toBe("CHSI123");
  expect(req.form).toContain('name="files"; filename="image-1.jpg"');
  expect(text(q(page, "#student-status"))).toContain("待审核");
  expect(q(page, "#student-consent")).toBeNull();
});

test("approvedStudentShowsValidUntilAndCannotReapply", async () => {
  installWx({
    "GET /api/v1/customer/student-certifications/current": ok({ id: "sc1", status: "approved", status_label: "已通过", valid_until: "2028-06-30",
      reject_reason: null, submitted_at: "2026-10-24T08:01:00Z", reviewed_at: "2026-10-25T08:01:00Z" }),
  }, TOKEN);
  const page = mount("pages/student/index");
  await flush();
  expect(text(q(page, "#student-status"))).toContain("已通过：学生资格有效至 2028-06-30");
  expect(q(page, "#student-consent")).toBeNull();
});
