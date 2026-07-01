# U 盘密钥托管 Runbook（模型①：安装/备份/验证用 U 盘，运行态不依赖 U 盘）

> 状态：slice B（安装期 provision + 备份/迁移）。运行态密钥读取由后端 `LocalKeyVault`（`common/crypto`）
> 从本地 env 取，slice A 已落地；本 runbook 只覆盖「密钥怎么进到运行态本地」。
> slice C（U 盘挑战签名作最终验证器）另行设计。

## 决策（当前默认，可改）
| 项 | 默认 | 备选 |
|---|---|---|
| keystore 加密 | `openssl AES-256-CBC + PBKDF2(20 万轮)` + 口令（双因子：有盘 + 知口令） | 明文（纯 possession，丢盘即全泄，不推荐） |
| 纳管范围 | keys.list 中 **app 自有密钥**自动随机生成；DB/admin 等**外部既定**凭证由 DBA/IT 手动写入 | 全随机（会破坏既有 DB role/账号，禁用） |
| 机器绑定 | manifest 仅记录主机名/指纹，**不强制** | 绑机器指纹（防异地 provision，迁移换机需重签） |

## 密钥分类（重要）
- **app 自有（随机生成，见 `ops/keyvault/keys.list`）**：`COMPLEX_KEYVAULT_PII_KEY`、`COMPLEX_TICKET_SIGNING_SECRET`、`COMPLEX_BAR_INVITATION_SIGNING_SECRET`、`JWT_SECRET`。
- **外部既定（勿随机生成，DBA/IT 按实际值追加到 `secrets.env`）**：`COMPLEX_DATABASE_PASSWORD`、`COMPLEX_DATABASE_APP_PASSWORD`（须与 Postgres role 实际密码一致）、`P0_ADMIN_PASSWORD`（如启用）。
- 新增 app 自有密钥：在 `keys.list` 加一行即被 generate/provision/verify 覆盖。

## 首次安装（有 U 盘的机器上）
1. 插入 U 盘，记其挂载点（如 `/media/usb`）。
2. `bash ops/keyvault/generate-keystore.sh /media/usb`，按提示设 keystore 口令。
3. 得到 `/media/usb/complex-keys/{keystore.env.enc, manifest.json}`。**离线复制一份 U 盘备份，两地保险柜保管。**

## provision 到运行态（.160）
1. 插入 U 盘。
2. `sudo bash ops/keyvault/provision-from-usb.sh /media/usb`（默认写 `/etc/complex/secrets.env`，600）。
3. DBA/IT 把外部既定凭证（DB/admin 实际值）**追加**进 `/etc/complex/secrets.env`。
4. 后端 systemd unit 加 `EnvironmentFile=/etc/complex/secrets.env`，重启服务。
5. `bash ops/keyvault/verify-provisioned.sh`（PASS = 齐全且非弱）。
6. **拔出 U 盘离线保管**——运行态此后只读本地，断电重启无需插盘。

## 迁移到新机
新机 = 插 U 盘 → `provision-from-usb.sh` → DBA 追加外部凭证 → 配 systemd → 拔盘。母本即 U 盘。

## 密钥轮换
在有 U 盘的机器重跑 `generate-keystore.sh`（生成新值 + 新 manifest）→ 各运行机重新 provision → 更新用到旧密钥版本的下游（如门票签名 version）。轮换证据按安全基线台账登记。

## 灾备
U 盘=母本；备份=离线 U 盘副本（两地）。运行态 `secrets.env` 丢失 → 重新 provision 即可。

## 边界（P0-A：agent 不装系统级配置）
本目录脚本由 **OPS 人工执行**；仓库内 agent 只产出脚本 + 本 runbook，不自动写 `/etc`、不自动改 systemd。

## 自检
`bash ops/keyvault/selftest.sh` —— 临时目录模拟 U 盘走完整往返（不碰真实系统/密钥），验证生成/加密/provision/校验链路正常。
