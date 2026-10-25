// 文件版本：V0.01
import { Alert, Button, Card, Empty, Input, List, Select, Space, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useState } from "react";

import type { CustomerComplaint, CustomerProject, CustomerService } from "@complex/customer-core";

import { platform, type AudioRecording, type MediaKind } from "../platform";

const { Text } = Typography;

/** OpenAPI：附件合计不超过 9 个；类型由服务端按扩展名判定（照片 jpg/jpeg/png/webp/heic、视频 mp4/mov/webm、语音 m4a/aac/mp3/amr/wav）。 */
const MAX_FILES = 9;
const MEDIA_BUTTONS: Array<{ kind: MediaKind; label: string }> = [
  { kind: "image", label: "添加照片" },
  { kind: "video", label: "添加视频" },
];

// 投诉与反馈（BSM「客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」）＋我的投诉（BSM「消费客人的投诉，由客户关怀部记录问题解决方案
// 及客人反馈」；BSM「满意度在回访/验收时由提单方评分」）：只列本人提交的投诉；写了处理结论、尚未评过的才可评 1～5 分，同单只评一次。
// 语音在 App 内现场录（与小程序同口径），录音中不可提交。客人端已暂停服务的综合体不列（PGM「关闭后菜单不可见或不可点」），所选综合体报给外壳
// （请求带 X-Project-Id，后端按综合体拒绝）。
export function ComplaintsPage({ service, projectOpen = () => true, onProject }: { service: CustomerService;
  projectOpen?: (projectId: string) => boolean; onProject?: (projectId: string) => void }): React.ReactElement {
  const [projects, setProjects] = useState<CustomerProject[]>([]);
  const [projectId, setProjectId] = useState<string | undefined>();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState<CustomerComplaint | null>(null);
  const [mine, setMine] = useState<CustomerComplaint[]>([]);
  const [mineError, setMineError] = useState<string | null>(null);
  const [rating, setRating] = useState<string | null>(null);
  const [recording, setRecording] = useState<AudioRecording | null>(null);

  const choose = (id: string) => {
    setProjectId(id);
    onProject?.(id);
  };

  const loadMine = useCallback(() => {
    service.listMyComplaints()
      .then((rows) => { setMine(rows ?? []); setMineError(null); })
      .catch((err) => setMineError(err instanceof Error ? err.message : "我的投诉加载失败"));
  }, [service]);

  useEffect(() => {
    service.listProjects()
      .then((rows) => {
        const open = (rows ?? []).filter((row) => projectOpen(row.projectId));
        setProjects(open);
        if (open[0]) choose(open[0].projectId);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "综合体加载失败"));
    loadMine();
  }, [service, loadMine]);

  const addFile = (file: File) => setFiles((prev) => [...prev, file].slice(0, MAX_FILES));
  const addMedia = async (kind: MediaKind) => {
    const file = await platform.pickMedia(kind);
    if (file) addFile(file);
  };
  const startRecord = async () => {
    const next = await platform.startRecording().catch(() => null);
    if (next) setRecording(next);
    else setError("录音失败，请允许使用麦克风后重试");
  };
  const stopRecord = async () => {
    const current = recording;
    setRecording(null);
    if (current) addFile(await current.stop());
  };

  const submit = async () => {
    if (!projectId || !title.trim()) return;
    setBusy(true);
    setError(null);
    try {
      setSubmitted(await service.submitComplaint({ projectId, title: title.trim(), description: description.trim() || undefined, files }));
      setTitle("");
      setDescription("");
      setFiles([]);
      loadMine();
    } catch (err) {
      setError(err instanceof Error ? err.message : "投诉提交失败");
    } finally {
      setBusy(false);
    }
  };

  const rate = async (workOrderId: string, score: number) => {
    setRating(workOrderId);
    try {
      await service.rateComplaint(workOrderId, score);
      setMineError(null);
      loadMine();
    } catch (err) {
      setMineError(err instanceof Error ? err.message : "评价失败");
    } finally {
      setRating(null);
    }
  };

  return (
    <Space direction="vertical" style={{ width: "100%" }} size="large">
      <Card title="提交投诉">
        <Space direction="vertical" style={{ width: "100%" }}>
          {error ? <Alert type="error" showIcon message={error} /> : null}
          {submitted ? <Alert type="success" showIcon data-testid="complaint-submitted" message={`投诉已提交，编号 ${submitted.code}`} /> : null}
          {projects.length > 1 ? (
            <Select aria-label="投诉所在综合体" value={projectId} onChange={choose} style={{ width: "100%" }}
              options={projects.map((item) => ({ value: item.projectId, label: item.projectName }))} />
          ) : null}
          <label htmlFor="complaint-title">投诉标题</label>
          <Input id="complaint-title" maxLength={100} value={title} onChange={(e) => setTitle(e.target.value)} />
          <label htmlFor="complaint-description">文字留言</label>
          <Input.TextArea id="complaint-description" rows={4} value={description} onChange={(e) => setDescription(e.target.value)} />
          <Space wrap>
            {MEDIA_BUTTONS.map(({ kind, label }) => (
              <Button key={kind} disabled={files.length >= MAX_FILES} onClick={() => void addMedia(kind)} data-testid={`complaint-add-${kind}`}>
                {label}
              </Button>
            ))}
            {recording ? (
              <Button danger onClick={() => void stopRecord()} data-testid="complaint-record-stop">停止录音</Button>
            ) : (
              <Button disabled={files.length >= MAX_FILES} onClick={() => void startRecord()} data-testid="complaint-record-start">录语音</Button>
            )}
          </Space>
          <div data-testid="complaint-files">
            {files.length ? (
              <List size="small" dataSource={files} renderItem={(file, index) => (
                <List.Item key={`${file.name}-${index}`}
                  actions={[<Button key="remove" type="link" size="small" onClick={() => setFiles(files.filter((_, i) => i !== index))}>移除</Button>]}>
                  {file.name}
                </List.Item>
              )} />
            ) : <Text type="secondary">可添加照片、视频、语音，合计最多 {MAX_FILES} 个。</Text>}
          </div>
          <Button type="primary" block loading={busy} disabled={!projectId || !title.trim() || Boolean(recording)} onClick={() => void submit()}
            data-testid="complaint-submit">
            提交投诉
          </Button>
        </Space>
      </Card>

      <Card title="我的投诉">
        {mineError ? <Alert type="error" showIcon message={mineError} /> : null}
        {mine.length === 0 ? (
          <Empty description="暂无投诉" />
        ) : (
          <List dataSource={mine} renderItem={(item) => (
            <List.Item key={item.workOrderId} data-testid={`complaint-${item.workOrderId}`}>
              <Space direction="vertical" style={{ width: "100%" }}>
                <Space wrap><Text strong>{item.title}</Text><Tag>{item.statusLabel}</Tag></Space>
                <Text type="secondary">编号 {item.code} · 提交于 {dayjs(item.submittedAt).format("YYYY-MM-DD HH:mm")} · 附件 {item.mediaCount} 个</Text>
                {item.resolution ? <Text>处理结论：{item.resolution}</Text> : <Text type="secondary">处理中，有结论后可评价</Text>}
                {item.satisfaction != null ? (
                  <Text>满意度 {item.satisfaction} 分</Text>
                ) : item.resolution ? (
                  <Space wrap>
                    <Text>请评满意度：</Text>
                    {[1, 2, 3, 4, 5].map((score) => (
                      <Button key={score} size="small" disabled={rating !== null} onClick={() => void rate(item.workOrderId, score)}
                        data-testid={`complaint-rate-${item.workOrderId}-${score}`}>
                        {score} 分
                      </Button>
                    ))}
                  </Space>
                ) : null}
              </Space>
            </List.Item>
          )} />
        )}
      </Card>
    </Space>
  );
}
