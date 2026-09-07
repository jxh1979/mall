#!/usr/bin/env bash
# 自检（ponytail 运行检查）：临时目录模拟 U 盘，走 生成→provision→校验 往返。
# 不碰真实系统 / 真实密钥 / 真实 U 盘（全在 mktemp 目录，退出即删）。
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

export COMPLEX_KEYVAULT_PASS="selftest-pass-nonsecret-0000"

GEN_OUT="$(bash "$HERE/generate-keystore.sh" "$TMP/usb" 2>&1)"
[ -f "$TMP/usb/complex-keys/keystore.env.enc" ] || { echo "FAIL: keystore 未生成"; exit 1; }
grep -qa 'Salted' "$TMP/usb/complex-keys/keystore.env.enc" || { echo "FAIL: 密文无 salt 头（可能未加密）"; exit 1; }
# M008-G2-01（PGM-1.15.1「不预制备用钥匙，丢失即按本闭环重制」）：脚本输出不得教操作员预制备用 / 备份钥匙。
if printf '%s' "$GEN_OUT" | grep -qE '备份|备用|复制一份'; then
    echo "FAIL: generate-keystore 输出仍提示预制备用钥匙（PGM-1.15.1 不预制备用钥匙）"; exit 1
fi

bash "$HERE/provision-from-usb.sh" "$TMP/usb" "$TMP/secrets.env" >/dev/null
bash "$HERE/verify-provisioned.sh" "$TMP/secrets.env" >/dev/null

# M008-F-I3-01（BSM-1.26.1 / R-004「运行时不依赖 U 盘、断电重启无需插盘」）：失败的插盘不得清空已有运行态密钥文件。
# 预置非空目标 → 错口令 provision 必须退出非 0 且目标字节不变 → 正确口令再 provision 成功。
printf 'EXISTING_KEY=keep-me-intact-0123456789abcdef0123456789\n' > "$TMP/secrets2.env"
BEFORE="$(sha256sum "$TMP/secrets2.env" | cut -c1-64)"
if COMPLEX_KEYVAULT_PASS="wrong-pass-nonsecret" bash "$HERE/provision-from-usb.sh" "$TMP/usb" "$TMP/secrets2.env" >/dev/null 2>&1; then
    echo "FAIL: 错口令 provision 竟成功"; exit 1
fi
AFTER="$(sha256sum "$TMP/secrets2.env" | cut -c1-64)"
[ "$BEFORE" = "$AFTER" ] || { echo "FAIL: 错口令 provision 改动了已有 secrets.env（运行态密钥被截空 / 覆盖）"; exit 1; }
[ -z "$(ls -A "$TMP"/.secrets.env.* 2>/dev/null)" ] || { echo "FAIL: 失败后残留临时文件"; exit 1; }
bash "$HERE/provision-from-usb.sh" "$TMP/usb" "$TMP/secrets2.env" >/dev/null
bash "$HERE/verify-provisioned.sh" "$TMP/secrets2.env" >/dev/null

# 断言：明文 secrets.env 恢复出的每个密钥非空、>=32，且与 manifest 指纹一致。
while read -r k; do
    v="$(grep -E "^$k=" "$TMP/secrets.env" | cut -d= -f2-)"
    [ "${#v}" -ge 32 ] || { echo "FAIL: $k 长度不足"; exit 1; }
    fp_actual="$(printf '%s' "$v" | sha256sum | cut -c1-8)"
    grep -q "\"$k\": \"$fp_actual\"" "$TMP/usb/complex-keys/manifest.json" || { echo "FAIL: $k 指纹与 manifest 不一致"; exit 1; }
done < <(grep -vE '^\s*(#|$)' "$HERE/keys.list")

# ── 总钥匙（破窗最终验证器）Ed25519 签验往返：sign-challenge 签名 → openssl 公钥验签。──
CH="challenge-selftest-0001"
SIG_B64="$(bash "$HERE/sign-challenge.sh" "$TMP/usb" "$CH")"
base64 -d "$TMP/usb/complex-keys/master_authority.pub.b64" > "$TMP/pub.der"
openssl pkey -pubin -inform DER -in "$TMP/pub.der" -out "$TMP/pub.pem" 2>/dev/null
printf '%s' "$SIG_B64" | base64 -d > "$TMP/sig.bin"
printf '%s' "$CH" > "$TMP/ch.txt"
openssl pkeyutl -verify -pubin -inkey "$TMP/pub.pem" -rawin -in "$TMP/ch.txt" -sigfile "$TMP/sig.bin" >/dev/null \
    || { echo "FAIL: 总钥匙 Ed25519 签验往返"; exit 1; }
# 反例：改一个字节的挑战必须验签失败。
printf '%s' "${CH}X" > "$TMP/ch2.txt"
if openssl pkeyutl -verify -pubin -inkey "$TMP/pub.pem" -rawin -in "$TMP/ch2.txt" -sigfile "$TMP/sig.bin" >/dev/null 2>&1; then
    echo "FAIL: 篡改挑战竟通过验签"; exit 1
fi

echo "[keyvault selftest] 生成→provision→校验 往返 OK，密文加密、指纹一致，总钥匙 Ed25519 签验往返 OK，生成输出不含预制备用钥匙提示，错口令 provision 不改动已有文件"
