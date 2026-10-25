// 文件版本：V0.01
import { Alert, Button, Card, Input, List, Space, Spin, Typography } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService, StudentCertification } from "@complex/customer-core";

import { platform } from "../platform";

const { Paragraph, Text } = Typography;

/** 证件采集单独同意（BSM「采集证件……前先单独取得同意，并留同意版本、时间和范围」）：同意的范围原文与版本；时间由服务端记。 */
export const STUDENT_CERT_CONSENT_VERSION = "学生证件采集同意 V0.01";
const CONSENT_SCOPE =
  "为申请学生资格，同意本平台采集本人学生证件照片与学信网《学籍在线验证报告》在线验证码，仅供票务运营岗人工审核学生资格使用；注销账号时一并删除。";
const MAX_PHOTOS = 9;

function statusLine(cert: StudentCertification): string {
  if (cert.status === "approved") return `${cert.statusLabel}：学生资格有效至 ${cert.validUntil ?? "—"}`;
  if (cert.status === "rejected") return `${cert.statusLabel}：${cert.rejectReason ?? "未注明原因"}`;
  return cert.statusLabel;
}

// 学生认证（BSM「学生认证必须在本人注册会员后单独申请，认证通过只增加学生资格，不自动创建会员身份」；BSM「学生票要票务运营岗人工审核证件 + 学信网核验
// 通过才可用」；第十三批裁决第 1 条「有效到客人提供的学信网《学籍在线验证报告》上的预计毕业日期，审核人通过时按报告填写」）：先单独同意证件采集
// （记同意编号与版本），才可选证件照片（1～9 张）并填学信网在线验证码提交；待审核期间不能再申请，被驳回的可重新申请。
export function StudentCertificationPage({ service }: { service: CustomerService }): React.ReactElement {
  const [current, setCurrent] = useState<StudentCertification | null>(null);
  const [loading, setLoading] = useState(true);
  const [consentId, setConsentId] = useState<string | null>(null);
  const [photos, setPhotos] = useState<File[]>([]);
  const [chsiCode, setChsiCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    service.getStudentCertification()
      .then((value) => setCurrent(value ?? null))
      .catch((err) => setError(err instanceof Error ? err.message : "学生认证加载失败"))
      .finally(() => setLoading(false));
  }, [service]);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败");
    } finally {
      setBusy(false);
    }
  };

  const agree = () => run(async () => {
    const consent = await service.recordConsent({ consentType: "student_certificate", accepted: true, version: STUDENT_CERT_CONSENT_VERSION });
    setConsentId(consent.id);
  });

  const addPhoto = async () => {
    const file = await platform.pickMedia("image");
    if (file) setPhotos((prev) => [...prev, file].slice(0, MAX_PHOTOS));
  };

  const submit = () => run(async () => {
    if (!consentId) return;
    setCurrent(await service.applyStudentCertification({ consentId, chsiVerificationCode: chsiCode.trim(), files: photos }));
    setConsentId(null);
    setPhotos([]);
    setChsiCode("");
  });

  if (loading) return <Spin />;
  const canApply = !current || current.status === "rejected";

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      {error ? <Alert type="error" showIcon message={error} /> : null}
      {current ? (
        <Alert type={current.status === "approved" ? "success" : current.status === "rejected" ? "warning" : "info"} showIcon
          data-testid="student-status" message={statusLine(current)} />
      ) : null}
      {canApply ? (
        consentId ? (
          <Card title="提交学生认证">
            <Space direction="vertical" style={{ width: "100%" }}>
              <Button disabled={photos.length >= MAX_PHOTOS} onClick={() => void addPhoto()} data-testid="student-add-photo">添加学生证件照片</Button>
              <div data-testid="student-files">
                {photos.length ? (
                  <List size="small" dataSource={photos} renderItem={(file, index) => <List.Item key={`${file.name}-${index}`}>{file.name}</List.Item>} />
                ) : <Text type="secondary">至少 1 张、最多 {MAX_PHOTOS} 张。</Text>}
              </div>
              <label htmlFor="student-chsi">学信网在线验证码</label>
              <Input id="student-chsi" value={chsiCode} onChange={(e) => setChsiCode(e.target.value)} />
              <Text type="secondary">填写学信网《学籍在线验证报告》上的在线验证码，审核通过后学生资格有效到报告上的预计毕业日期。</Text>
              <Button type="primary" block loading={busy} disabled={photos.length === 0 || !chsiCode.trim()} onClick={() => void submit()}
                data-testid="student-submit">
                提交认证
              </Button>
            </Space>
          </Card>
        ) : (
          <Card title="学生证件采集同意" data-testid="student-consent">
            <Paragraph>{CONSENT_SCOPE}</Paragraph>
            <Paragraph type="secondary">{STUDENT_CERT_CONSENT_VERSION}</Paragraph>
            <Button type="primary" block loading={busy} onClick={() => void agree()} data-testid="student-consent-agree">同意并继续</Button>
          </Card>
        )
      ) : null}
    </Space>
  );
}
