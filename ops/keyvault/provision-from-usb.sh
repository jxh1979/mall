#!/usr/bin/env bash
# provision：从 U 盘解密 keystore 写入运行态 EnvironmentFile(600)。运行态从此只读本地、不依赖 U 盘（R-004 模型①）。
# M008-F-I3-01（BSM-1.26.1 / R-004「运行时不依赖 U 盘、断电重启无需插盘」）：解密先写同目录临时文件，全部内容（app 密钥 + 破窗公钥）
# 就位后再原子替换目标；口令错误 / 密文损坏 / 中断时原 secrets.env 字节级不变，绝不把运行态密钥文件截空。
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
STAGING="$(mktemp "$(dirname "$TARGET")/.secrets.env.XXXXXX")"
trap 'rm -f "$STAGING"' EXIT

PASS="$(kv_get_pass '输入 keystore 口令: ')"
if ! printf '%s' "$PASS" | openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass stdin \
    -in "$SRC" -out "$STAGING"; then
    echo "[keyvault] FAIL：keystore 解密失败（口令错误或密文损坏）；$TARGET 未改动。" >&2
    exit 1
fi

# U 盘破窗验签公钥（公开信息，R-004：服务器只留公钥）→ 与 app 密钥一起写入临时文件，再整体替换。
PUB="$USB_DIR/complex-keys/master_authority.pub.b64"
if [ -f "$PUB" ]; then
    sed -i '/^COMPLEX_MASTER_AUTHORITY_PUBKEY=/d' "$STAGING"
    printf 'COMPLEX_MASTER_AUTHORITY_PUBKEY=%s\n' "$(cat "$PUB")" >> "$STAGING"
    echo "[keyvault] 已写入 U 盘破窗验签公钥 COMPLEX_MASTER_AUTHORITY_PUBKEY（启用破窗登录）。"
fi

chmod 600 "$STAGING"
mv -f "$STAGING" "$TARGET"
trap - EXIT

echo "[keyvault] 已写入 $TARGET (600)。"
echo "[keyvault] DB 角色密码 / admin 等外部凭证请由 DBA/IT 按实际值追加到该文件（勿随机生成）。"
echo "[keyvault] 后端 systemd 以 EnvironmentFile=$TARGET 引用后，可拔出 U 盘离线保管。"
