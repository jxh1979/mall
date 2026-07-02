#!/usr/bin/env bash
# U 盘破窗登录：用 U 盘私钥对服务端挑战码 Ed25519 签名，输出 base64 签名（贴回 POST /api/v1/auth/master/login）。
# 用法: sign-challenge.sh <usb_dir> <challenge>
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/lib.sh"

USB_DIR="${1:?用法: sign-challenge.sh <usb_dir> <challenge>}"
CHALLENGE="${2:?缺少 challenge（先调 POST /api/v1/auth/master/challenge 取）}"
ENC="$USB_DIR/complex-keys/master_authority.key.enc"
[ -f "$ENC" ] || { echo "[keyvault] 找不到 $ENC（该 U 盘未生成总钥匙？）" >&2; exit 1; }

PRIV="$(mktemp)"
CHFILE="$(mktemp)"
trap 'rm -f "$PRIV" "$CHFILE"' EXIT
PASS="$(kv_get_pass '输入 keystore 口令: ')"
printf '%s' "$PASS" | openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass stdin -in "$ENC" -out "$PRIV"

# 对挑战原文（UTF-8 bytes）做 Ed25519 raw 签名 → base64（与后端 Signature("Ed25519") 校验一致）。
# 注：EdDSA 是 oneshot，openssl 需可定大小的 -in 文件（管道 stdin 会报 "unable to determine file size"）。
printf '%s' "$CHALLENGE" > "$CHFILE"
openssl pkeyutl -sign -inkey "$PRIV" -rawin -in "$CHFILE" | base64 -w0
echo
