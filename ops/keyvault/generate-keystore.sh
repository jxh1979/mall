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
trap 'rm -f "$PLAIN"' EXIT

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

echo "[keyvault] 已写入 $DEST: keystore.env.enc + manifest.json"
echo "[keyvault] 请核对后妥善保管 U 盘，并离线复制一份备份（两地保险柜）。"
