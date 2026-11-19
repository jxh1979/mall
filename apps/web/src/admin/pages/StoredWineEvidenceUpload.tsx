// 文件版本：V0.01
// 第十八批 AY 段 269-R1（BSM:1072「存酒作废需照片+视频双证据+主管复核」；BSM:1940「……必须记录原因、员工、时间、点位、照片、视频和主管复核结果。」；
// BSM:1170「存酒登记必须拍照……」；PGM:287 字段名全中文）：存酒页作废、补存、结案与追收、交接的照片视频改为拍照或录像真实上传，
// 写法同存酒登记照片 uploadRegisterPhotos；提交时只发服务端回的附件编号，界面只显示文件名。
import React from "react";
import type { AdminApiClient } from "../types";

export type StoredWineEvidenceFile = { id: string; fileName: string };

type StoredWineEvidenceUploadProps = {
  api: AdminApiClient;
  label: string;
  businessType: string;
  attachmentRole: string;
  accept: string;
  files: StoredWineEvidenceFile[];
  onChange: (files: StoredWineEvidenceFile[]) => void;
  testId: string;
};

export function StoredWineEvidenceUpload({ api, label, businessType, attachmentRole, accept, files, onChange, testId }: StoredWineEvidenceUploadProps) {
  const [uploading, setUploading] = React.useState(false);
  const [error, setError] = React.useState("");

  async function upload(list: FileList | null) {
    if (!list || list.length === 0) return;
    setUploading(true);
    setError("");
    try {
      const uploaded: StoredWineEvidenceFile[] = [];
      for (const file of Array.from(list)) {
        const form = new FormData();
        form.append("file", file);
        form.append("business_type", businessType);
        form.append("attachment_role", attachmentRole);
        if (!api.postForm) throw new Error("当前客户端不支持文件上传。");
        const saved = await api.postForm<{ id: string; fileName?: string }>("/api/v1/evidence/attachments/upload", form);
        uploaded.push({ id: saved.id, fileName: saved.fileName ?? file.name });
      }
      onChange([...files, ...uploaded]);
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : `${label}失败`);
    } finally {
      setUploading(false);
    }
  }

  return (
    <>
      <label>{label}<input data-testid={testId} type="file" multiple accept={accept} disabled={uploading} onChange={(event) => void upload(event.target.files)} /></label>
      {files.length > 0 ? <div className="admin-muted-action" data-testid={`${testId}-files`}>{files.map((file) => file.fileName).join("、")}</div> : null}
      {error ? <div role="alert" className="admin-inline-error" data-testid={`${testId}-error`}>{error}</div> : null}
    </>
  );
}
