import { Alert, Button, Card, Checkbox, DatePicker, Form, Input, InputNumber, Result, Space, Select, Typography } from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { PaymentIntent, TicketOrder, TicketProduct } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;

// 人脸照拍摄参数：长边 480 像素、JPEG 质量 0.8。够核销员肉眼比对，又不至于把高清人脸传上去、存下来。
// 生物识别信息越少越安全，分辨率也是「最小必要」的一部分。
const FACE_PHOTO_MAX_EDGE = 480;
const FACE_PHOTO_QUALITY = 0.8;

// ② 官方购票下单：创建票务订单 -> 为订单金额发起聚合支付意图（支付通道真实渠道占位）
// AUD-076 单店票挂酒吧：公共入口=通票置顶+按酒吧分组；专属入口（?bar=<barId>）锁定并显示该酒吧。
// V359 人脸照：要求拍照的票种（游乐票默认要）在本页拍，照片随下单请求走，出票时绑到票上。
export function BuyTicketPage({ service }: { service: CustomerService }): React.ReactElement {
  // 专属入口：酒吧二维码/分享链接带 ?bar=<barId> 预锁定该酒吧（锁定也必须显示，不能看不见就买）。
  const lockedBarId = useMemo(() => new URLSearchParams(window.location.search).get("bar"), []);
  const [form] = Form.useForm();
  const [products, setProducts] = useState<TicketProduct[]>([]);
  const [order, setOrder] = useState<TicketOrder | null>(null);
  const [intent, setIntent] = useState<PaymentIntent | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // 人脸照相关状态：同意书勾选、已拍的照片（base64，只在内存里）、摄像头开关与报错。
  const [selectedTicketTypeId, setSelectedTicketTypeId] = useState<string | undefined>();
  const [quantity, setQuantity] = useState(1);
  const [faceConsent, setFaceConsent] = useState(false);
  const [facePhotos, setFacePhotos] = useState<string[]>([]);
  const [cameraOn, setCameraOn] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  useEffect(() => {
    // 3.17：票种加载失败上抛到 error 态，不再静默吞成空下拉。锁定入口只拉该酒吧单店票。
    service.listTicketProducts(lockedBarId ?? undefined)
      .then((items) => setProducts(items ?? []))
      .catch((err) => setError(err instanceof Error ? err.message : "票种加载失败"));
  }, [service, lockedBarId]);

  const selectedProduct = products.find((item) => item.ticketTypeId === selectedTicketTypeId);
  const needFacePhoto = !!selectedProduct?.requireFacePhoto;
  const retentionDays = selectedProduct?.facePhotoRetentionDays ?? 30;
  const facePhotosReady = !needFacePhoto || (faceConsent && facePhotos.length >= quantity);

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    setCameraOn(false);
  }, []);

  // 离开本页/换票种就关摄像头、清掉已拍的照片：人脸照不留在没在用它的页面上。
  useEffect(() => stopCamera, [stopCamera]);
  useEffect(() => {
    setFacePhotos([]);
    setFaceConsent(false);
    setCameraError(null);
    stopCamera();
  }, [selectedTicketTypeId, stopCamera]);

  // 买的张数改小了，多拍的照片立刻丢掉，不留下用不上的人脸。
  useEffect(() => {
    setFacePhotos((prev) => (prev.length > quantity ? prev.slice(0, quantity) : prev));
  }, [quantity]);

  async function openCamera() {
    setCameraError(null);
    const media = typeof navigator === "undefined" ? undefined : navigator.mediaDevices;
    if (!media || typeof media.getUserMedia !== "function") {
      setCameraError("当前浏览器不支持调用摄像头，请换用微信或手机自带浏览器打开本页面。");
      return;
    }
    try {
      const stream = await media.getUserMedia({ video: { facingMode: "user" } });
      streamRef.current = stream;
      setCameraOn(true);
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play().catch(() => undefined);
      }
    } catch {
      setCameraError("没能打开摄像头，请在浏览器里允许本页面使用相机后重试。");
    }
  }

  function capturePhoto() {
    const video = videoRef.current;
    if (!video) return;
    const width = video.videoWidth || FACE_PHOTO_MAX_EDGE;
    const height = video.videoHeight || FACE_PHOTO_MAX_EDGE;
    const scale = Math.min(1, FACE_PHOTO_MAX_EDGE / Math.max(width, height));
    const canvas = document.createElement("canvas");
    canvas.width = Math.round(width * scale);
    canvas.height = Math.round(height * scale);
    const context = canvas.getContext("2d");
    if (!context) {
      setCameraError("当前浏览器无法完成拍照，请换用微信或手机自带浏览器打开本页面。");
      return;
    }
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    // 只留 base64 本体，data URI 前缀由后端/展示侧各自拼，传输里不带多余信息。
    const base64 = canvas.toDataURL("image/jpeg", FACE_PHOTO_QUALITY).split(",")[1] ?? "";
    if (!base64) return;
    setFacePhotos((prev) => (prev.length >= quantity ? prev : [...prev, base64]));
  }

  function retakeAll() {
    setFacePhotos([]);
  }

  const onSubmit = async (values: {
    ticketTypeId: string;
    quantity: number;
    visitDate?: { format: (f: string) => string };
    contactPhone?: string;
  }) => {
    const product = products.find((p) => p.ticketTypeId === values.ticketTypeId);
    if (product?.requireFacePhoto) {
      if (!faceConsent) {
        setError("本票需要入场人脸照，请先阅读并同意人脸照拍摄说明。");
        return;
      }
      if (facePhotos.length < values.quantity) {
        setError(`本票每张都要一张入场人脸照，请拍满 ${values.quantity} 张后再提交。`);
        return;
      }
    }
    setSubmitting(true);
    setError(null);
    setOrder(null);
    setIntent(null);
    try {
      // 单店票带其所属酒吧 bar_id（服务端据票种校验）；通票不带。
      const created = await service.createTicketOrder({
        ticketTypeId: values.ticketTypeId,
        quantity: values.quantity,
        visitDate: values.visitDate ? values.visitDate.format("YYYY-MM-DD") : undefined,
        contactPhone: values.contactPhone,
        barId: product?.barId ?? undefined,
        facePhotos: product?.requireFacePhoto ? facePhotos.slice(0, values.quantity) : undefined,
      });
      setOrder(created);
      // 照片已经交给后端加密保存，本页内存里不再留着。
      setFacePhotos([]);
      stopCamera();
      const payment = await service.createPaymentIntent({
        businessOrderType: "ticket_order",
        businessOrderId: created.id,
        amountCent: created.amountCent,
        currency: created.currency || "CNY",
      });
      setIntent(payment);
    } catch (err) {
      setError(err instanceof Error ? err.message : "下单失败");
    } finally {
      setSubmitting(false);
    }
  };

  // 锁定酒吧名（从商品实时解析；商品未含则回退 id，仍须显示出来）。
  const lockedBarName = lockedBarId
    ? products.find((p) => p.barId === lockedBarId)?.barName ?? lockedBarId
    : null;

  // 分组下拉：通票置顶“综合体通票”，其余按酒吧分组；label 带酒吧名，同名不同店天然可分。
  const groupedOptions = useMemo(() => {
    const passItems = products.filter((p) => !p.barId);
    const byBar = new Map<string, { name: string; items: TicketProduct[] }>();
    for (const p of products) {
      if (!p.barId) continue;
      const group = byBar.get(p.barId) ?? { name: p.barName ?? p.barId, items: [] };
      group.items.push(p);
      byBar.set(p.barId, group);
    }
    const groups: { label: string; options: { value: string; label: string }[] }[] = [];
    if (passItems.length > 0) {
      groups.push({
        label: "综合体通票",
        options: passItems.map((p) => ({ value: p.ticketTypeId, label: p.ticketName })),
      });
    }
    for (const group of byBar.values()) {
      groups.push({
        label: group.name,
        options: group.items.map((p) => ({ value: p.ticketTypeId, label: `${p.ticketName} · ${group.name}` })),
      });
    }
    return groups;
  }, [products]);

  return (
    <div data-testid="buy-ticket-page">
      <Card title="官方购票">
        {lockedBarId ? (
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message={<span data-testid="buy-ticket-locked-bar">当前酒吧：{lockedBarName}</span>}
            description="您正通过该酒吧的专属入口购票，仅展示本酒吧单店票。"
          />
        ) : null}
        {error ? <Alert type="error" message="下单失败" description={error} style={{ marginBottom: 12 }} /> : null}
        <Form form={form} layout="vertical" onFinish={onSubmit} initialValues={{ quantity: 1 }}>
          <Form.Item name="ticketTypeId" label="票种" rules={[{ required: true, message: "请选择票种" }]}>
            <Select
              data-testid="buy-ticket-type"
              placeholder="请选择票种"
              options={groupedOptions}
              onChange={(value: string) => setSelectedTicketTypeId(value)}
            />
          </Form.Item>
          <Form.Item name="quantity" label="数量" rules={[{ required: true }]}>
            <InputNumber
              min={1}
              max={20}
              style={{ width: "100%" }}
              data-testid="buy-ticket-quantity"
              onChange={(value) => setQuantity(Number(value ?? 1))}
            />
          </Form.Item>
          <Form.Item name="visitDate" label="游玩日期">
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="contactPhone" label="联系手机号">
            <Input placeholder="可选" />
          </Form.Item>

          {needFacePhoto ? (
            <div data-testid="buy-ticket-face-photo" style={{ marginBottom: 16 }}>
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 12 }}
                message="本票需要入场人脸照"
                description={
                  <div data-testid="buy-ticket-face-notice">
                    <p>为什么要拍：入场核销时工作人员会拿这张照片和您本人比对，确认用票的就是买票的人，防止门票被转给他人使用。</p>
                    <p>怎么保存：照片加密保存，只有核销和运营人员在核销这张票时才能查看，每一次查看都会留下记录。</p>
                    <p>留多久：门票失效后满 {retentionDays} 天自动销毁，不再保留。</p>
                    <p>不拍行不行：本票种必须拍才能出票；如不同意，请选择其他不需要人脸照的票种。</p>
                  </div>
                }
              />
              <Checkbox
                data-testid="buy-ticket-face-consent"
                checked={faceConsent}
                onChange={(event) => setFaceConsent(event.target.checked)}
              >
                我已阅读上述说明，同意为本次购票拍摄并按上述期限留存入场人脸照
              </Checkbox>
              <div style={{ marginTop: 12 }}>
                {cameraError ? (
                  <Alert type="error" showIcon message="摄像头不可用" description={cameraError} style={{ marginBottom: 8 }} data-testid="buy-ticket-face-camera-error" />
                ) : null}
                <Text data-testid="buy-ticket-face-progress">
                  已拍 {facePhotos.length} / {quantity} 张（每张票一位入场人，请依次拍摄）
                </Text>
                <div style={{ marginTop: 8 }}>
                  {/* 摄像头默认不开：没勾同意之前，本页不碰您的相机。 */}
                  <video
                    ref={videoRef}
                    data-testid="buy-ticket-face-video"
                    playsInline
                    muted
                    style={{ width: "100%", maxWidth: 320, display: cameraOn ? "block" : "none", background: "#000" }}
                  />
                </div>
                <Space wrap style={{ marginTop: 8 }}>
                  {!cameraOn ? (
                    <Button
                      data-testid="buy-ticket-face-open-camera"
                      disabled={!faceConsent}
                      onClick={() => void openCamera()}
                    >
                      开启摄像头
                    </Button>
                  ) : (
                    <>
                      <Button
                        type="primary"
                        data-testid="buy-ticket-face-capture"
                        disabled={facePhotos.length >= quantity}
                        onClick={capturePhoto}
                      >
                        拍摄第 {Math.min(facePhotos.length + 1, quantity)} 张
                      </Button>
                      <Button data-testid="buy-ticket-face-close-camera" onClick={stopCamera}>关闭摄像头</Button>
                    </>
                  )}
                  {facePhotos.length > 0 ? (
                    <Button data-testid="buy-ticket-face-retake" onClick={retakeAll}>全部重拍</Button>
                  ) : null}
                </Space>
                {facePhotos.length > 0 ? (
                  <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap" }} data-testid="buy-ticket-face-thumbs">
                    {facePhotos.map((photo, index) => (
                      <img
                        key={index}
                        src={`data:image/jpeg;base64,${photo}`}
                        alt={`第 ${index + 1} 张入场人脸照`}
                        style={{ width: 72, height: 72, objectFit: "cover", border: "1px solid #d9d9d9" }}
                      />
                    ))}
                  </div>
                ) : null}
              </div>
            </div>
          ) : null}

          <Button
            type="primary"
            htmlType="submit"
            loading={submitting}
            block
            disabled={!facePhotosReady}
            data-testid="buy-ticket-submit"
          >
            提交订单并支付
          </Button>
        </Form>
      </Card>
      {order ? (
        <Card style={{ marginTop: 16 }} data-testid="buy-ticket-result">
          <Result
            status="success"
            title="订单已创建"
            subTitle={`订单号 ${order.orderNo} · 金额 ¥${(order.amountCent / 100).toFixed(2)}`}
          >
            <Space direction="vertical">
              <Text>支付状态：{formatBusinessStatus(order.payStatus)}</Text>
              {intent ? (
                <Paragraph type="secondary">
                  已发起聚合支付，当前支付状态：{formatBusinessStatus(String(intent.status ?? "pending"))}。
                  真实支付渠道跳转为占位，客人端仅展示支付结果。
                </Paragraph>
              ) : null}
            </Space>
          </Result>
        </Card>
      ) : null}
    </div>
  );
}
