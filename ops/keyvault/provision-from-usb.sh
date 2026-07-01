#!/usr/bin/env bash
# provision：从 U 盘解密 keystore 写入运行态 EnvironmentFile(600)。运行态从此只读本地、不依赖 U 盘。
# 用法: provision-from-usb.sh <usb_dir> [target_env_file]   # 默认 /etc/complex/secrets.env
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/lib.sh"

USB_DIR="${1:?用法: provision-from-usb.sh <usb_dir> [target_env_file]}"
TARGET="${2:-/etc/complex/secrets.env}"
SRC="$USB_DIR/complex-keys/keystore.env.enc"
[ -f "$SRC" ] || { echo "[keyvault] 找不到 $SRC" >&2; exit 1; }

umask 077
mkdir -p "$(dirname "$TARGET")"
PASS="$(kv_get_pass '输入 keystore 口令: ')"
printf '%s' "$PASS" | openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass stdin \
    -in "$SRC" -out "$TARGET"
chmod 600 "$TARGET"

# U 盘破窗公钥（公开信息）→ EnvironmentFile：后端配到公钥即启用破窗登录（默认关）。
PUB="$USB_DIR/complex-keys/master_authority.pub.b64"
if [ -f "$PUB" ]; then
    sed -i '/^COMPLEX_MASTER_AUTHORITY_PUBKEY=/d' "$TARGET"
    printf 'COMPLEX_MASTER_AUTHORITY_PUBKEY=%s\n' "$(cat "$PUB")" >> "$TARGET"
    echo "[keyvault] 已写入 U 盘破窗公钥 COMPLEX_MASTER_AUTHORITY_PUBKEY（启用破窗登录）。"
fi

echo "[keyvault] 已写入 $TARGET (600)。"
echo "[keyvault] DB 角色密码 / admin 等外部凭证请由 DBA/IT 按实际值追加到该文件（勿随机生成）。"
echo "[keyvault] 后端 systemd 以 EnvironmentFile=$TARGET 引用后，可拔出 U 盘离线保管。"
