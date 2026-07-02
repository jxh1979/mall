#!/usr/bin/env bash
# 首装：为 keys.list 每个 app 自有密钥生成强随机值，口令加密成 keystore.env.enc 写入 U 盘。
# 用法: generate-keystore.sh <usb_dir>
# 决策默认（可改见 keyvault-usb-runbook.md）：openssl AES-256-CBC+PBKDF2 口令加密；
#   纳管 keys.list 全部 app 自有密钥；manifest 仅记录机器/指纹、不强制机器绑定。
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/lib.sh"

USB_DIR="${1:?用法: generate-keystore.sh <usb_dir>}"
DEST="$USB_DIR/complex-keys"
mkdir -p "$DEST"
umask 077

PLAIN="$(mktemp)"
MASTER_PRIV="$(mktemp)"
trap 'rm -f "$PLAIN" "$MASTER_PRIV"' EXIT

# 每个密钥：32 字节随机 → base64（44 字符，>=32 满足强度），url-safe 替换 / +。
while read -r k; do
    printf '%s=%s\n' "$k" "$(openssl rand -base64 32 | tr -d '\n' | tr '/+' '_-')"
done < <(kv_keys_list) > "$PLAIN"

PASS="$(kv_get_pass '设置 keystore 口令: ')"
printf '%s' "$PASS" | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass stdin \
    -in "$PLAIN" -out "$DEST/keystore.env.enc"

# manifest：仅记录生成时间/主机/各密钥指纹（sha256 前 8 位），不含任何明文。
{
    printf '{\n  "generated_at": "%s",\n  "host": "%s",\n  "fingerprints": {\n' \
        "$(date -u +%FT%TZ)" "$(hostname)"
    first=1
    while IFS= read -r line; do
        k="${line%%=*}"          # 键名（首个 = 前）
        v="${line#*=}"           # 完整值（首个 = 后，保留末尾 base64 padding =）
        fp="$(printf '%s' "$v" | sha256sum | cut -c1-8)"
        [ "$first" -eq 1 ] || printf ',\n'
        printf '    "%s": "%s"' "$k" "$fp"
        first=0
    done < "$PLAIN"
    printf '\n  }\n}\n'
} > "$DEST/manifest.json"

# ── U 盘总钥匙（破窗最终验证器）Ed25519 密钥对：私钥口令加密进 U 盘，公钥 SPKI DER base64 供 provision 到服务器。──
openssl genpkey -algorithm ed25519 -out "$MASTER_PRIV" 2>/dev/null
printf '%s' "$PASS" | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass stdin \
    -in "$MASTER_PRIV" -out "$DEST/master_authority.key.enc"
openssl pkey -in "$MASTER_PRIV" -pubout -outform DER | base64 -w0 > "$DEST/master_authority.pub.b64"

echo "[keyvault] 已写入 $DEST: keystore.env.enc + manifest.json + master_authority.key.enc + master_authority.pub.b64"
echo "[keyvault] 请核对后妥善保管 U 盘，并离线复制一份备份（两地保险柜）。破窗私钥永不落服务器，只在 U 盘。"
