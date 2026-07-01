#!/usr/bin/env bash
# 自检（ponytail 运行检查）：临时目录模拟 U 盘，走 生成→provision→校验 往返。
# 不碰真实系统 / 真实密钥 / 真实 U 盘（全在 mktemp 目录，退出即删）。
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

export COMPLEX_KEYVAULT_PASS="selftest-pass-nonsecret-0000"

bash "$HERE/generate-keystore.sh" "$TMP/usb" >/dev/null
[ -f "$TMP/usb/complex-keys/keystore.env.enc" ] || { echo "FAIL: keystore 未生成"; exit 1; }
grep -qa 'Salted' "$TMP/usb/complex-keys/keystore.env.enc" || { echo "FAIL: 密文无 salt 头（可能未加密）"; exit 1; }

bash "$HERE/provision-from-usb.sh" "$TMP/usb" "$TMP/secrets.env" >/dev/null
bash "$HERE/verify-provisioned.sh" "$TMP/secrets.env" >/dev/null

# 断言：明文 secrets.env 恢复出的每个密钥非空、>=32，且与 manifest 指纹一致。
while read -r k; do
    v="$(grep -E "^$k=" "$TMP/secrets.env" | cut -d= -f2-)"
    [ "${#v}" -ge 32 ] || { echo "FAIL: $k 长度不足"; exit 1; }
    fp_actual="$(printf '%s' "$v" | sha256sum | cut -c1-8)"
    grep -q "\"$k\": \"$fp_actual\"" "$TMP/usb/complex-keys/manifest.json" || { echo "FAIL: $k 指纹与 manifest 不一致"; exit 1; }
done < <(grep -vE '^\s*(#|$)' "$HERE/keys.list")

echo "[keyvault selftest] 生成→provision→校验 往返 OK，密文加密、指纹一致"
