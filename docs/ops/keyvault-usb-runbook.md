# U 盘密钥托管 Runbook（模型①：安装/更换密钥/破窗用 U 盘，运行态不依赖 U 盘）

> 裁决 R-004（2026-09-07，操作员）：系统启动、破窗登录（应急直接登录），以及手动清理留存数据等最高权限动作相关密钥
> **都不保留在服务器上**；服务器只留破窗验签用的公钥（无法执行任何最高权限操作）。运行时不依赖 U 盘、断电重启无需插盘，
> 因为服务器运行用的是普通业务密钥（keys.list 纳管的 app 自有密钥）而非最高权限密钥。
> 对应 HTML：业务地图 BSM-1.26.1、治理地图 PGM-1.15.1（2026-09-07 同步措辞）。
>
> 状态：slice B（安装期 provision + 迁移）+ slice C（U 盘挑战签名作最终验证器）。运行态普通业务密钥由后端 `LocalKeyVault`
> （`common/crypto`）从本地 env 取；本 runbook 覆盖「普通业务密钥怎么进到运行态本地」与「总钥匙怎么用、怎么换」。

## 决策（当前默认，可改）
| 项 | 默认 | 备选 |
|---|---|---|
| keystore 加密 | `openssl AES-256-CBC + PBKDF2(20 万轮)` + 口令（双因子：有盘 + 知口令） | 明文（纯 possession，丢盘即全泄，不推荐） |
| 纳管范围 | keys.list 中 **app 自有密钥**自动随机生成；DB/admin 等**外部既定**凭证由 DBA/IT 手动写入 | 全随机（会破坏既有 DB role/账号，禁用） |
| 机器绑定 | manifest 仅记录主机名/指纹，**不强制** | 绑机器指纹（防异地 provision，迁移换机需重签） |
| 总钥匙（破窗私钥） | **只在授权 U 盘**，口令加密；服务器只有验签公钥 | — （HTML 与 R-004 不允许其他形态） |
| 备用钥匙 | **不预制**（PGM-1.15.1）；丢失 / 损坏 / 保管人变动即按下方「钥匙更换闭环」作废重制 | — |

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §B-30，本文不再重复记载。
## 首次安装（有 U 盘的机器上）
1. 插入 U 盘，记其挂载点（如 `/media/usb`）。
2. `bash ops/keyvault/generate-keystore.sh /media/usb`，按提示设 keystore 口令。
3. 得到 `/media/usb/complex-keys/{keystore.env.enc, manifest.json, master_authority.key.enc, master_authority.pub.b64}`。**不复制第二份可独立使用的密钥材料**（PGM-1.15.1「不预制备用钥匙」）；U 盘由指定负责人保管，做到「人机分离、钥匙不落地」。
4. 本次「启用 U 盘托管」的时间、操作人、见证人记入安全基线台账（BSM-1.26.1 留痕）。

## provision 到运行态（.160）
1. 插入 U 盘（本次「插盘托管」记台账：谁、什么时间）。
2. `sudo bash ops/keyvault/provision-from-usb.sh /media/usb`（默认写 `/etc/complex/secrets.env`，600；只写 app 自有密钥与破窗**公钥**，私钥永不落服务器）。
3. DBA/IT 把外部既定凭证（DB/admin 实际值）**追加**进 `/etc/complex/secrets.env`。生产种子管理员口令（`complex.auth.production.seed-admin.password`）不得用开发默认值（`admin-pass` / `change-me` / 短口令），后端启动即拒绝（M008-A1-03）。
4. 后端 systemd unit 加 `EnvironmentFile=/etc/complex/secrets.env`，重启服务。生产缺 `COMPLEX_MASTER_AUTHORITY_PUBKEY` 时后端拒绝启动（M008-A1-02：应急恢复通道不可缺省）；启动成功即在审计流记录当前验签公钥指纹 `auth.master.public_key_configured`。
5. `bash ops/keyvault/verify-provisioned.sh`（PASS = 齐全且非弱；文件不存在即 FAIL，dev 机器要跳过须显式 `--allow-missing`）。
6. **拔出 U 盘离线保管**——运行态此后只读本地普通业务密钥，断电重启无需插盘。

## 破窗登录（应急直接登录）
管理员账号全部无法登录时：插入授权 U 盘 → `POST /api/v1/auth/master/challenge` 取一次性挑战（2 分钟）→ `bash ops/keyvault/sign-challenge.sh /media/usb <challenge>` 用 U 盘私钥签名 → `POST /api/v1/auth/master/login {challenge, signature}`。成功即签发保留超管 `master-authority` 会话（后台 Web 走 HttpOnly Cookie + CSRF，同日常登录；响应体令牌仅供非浏览器恢复工具）。限速按来源（60 秒 5 次），错签 / 过期 / 重放 / 未配置一律拒绝并留痕；破窗一旦使用即全程留痕，事后必须复盘（PGM-1.15.1）。操作完拔盘。

## 迁移到新机
新机 = 插 U 盘 → `provision-from-usb.sh` → DBA 追加外部凭证 → 配 systemd → 拔盘。母本即 U 盘。

## 钥匙更换闭环（只能主动更换，不设例行轮换）
由最高权限本人执行、平台公司管理层见证。丢失、损坏或保管人变动时由人主动发起；不按周期自动换。每一步记台账，事后复盘。
1. **插入旧钥匙**：开始前必须先插入旧钥匙。未插入旧钥匙不得继续。
2. **授权**：旧钥匙仍插着，完成授权。
3. **插入新钥匙，旧钥匙同时失效**：插入新钥匙做更换的那一刻，旧钥匙失效，不能再用于密钥托管、破窗登录、留痕清理和逐笔放行。不以最后一台机器重启完成作为作废时间。只换总钥匙，不重做、不下发其他密钥。不要用首装脚本 `generate-keystore.sh` 做更换，它会重造 `keys.list` 里的全部密钥。仍然不预制备用钥匙。
4. **当面验证**：新钥匙启用前，见证人在场，逐项验证破窗登录、密钥托管、留痕清理与逐笔放行。任一项不通过不得启用、不得交接。
5. **交接**：新盘交指定保管人，记台账。不改门票签名版本。

## 灾备
U 盘＝母本，**无备份副本**。旧钥匙还在手上时，损坏或保管人变动按上方闭环主动更换。旧钥匙已经插不进去的，不得开始更换。运行态 `secrets.env` 丢失 → 用当前 U 盘重新 provision 即可。

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §B-31，本文不再重复记载。
## 边界（P0-A：agent 不装系统级配置）
本目录脚本由 **OPS 人工执行**；仓库内 agent 只产出脚本 + 本 runbook，不自动写 `/etc`、不自动改 systemd。

## 自检
`bash ops/keyvault/selftest.sh` —— 临时目录模拟 U 盘走完整往返（不碰真实系统/密钥），验证生成/加密/provision/校验链路与总钥匙签验往返正常。
