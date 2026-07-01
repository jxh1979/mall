#!/usr/bin/env bash
# 只读校验：EnvironmentFile 是否含 keys.list 全部密钥且非弱（>=32 且不含 change-me）。
# dev 未 provision（文件不存在）时 PASS 跳过。用法: verify-provisioned.sh [target_env_file]
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/lib.sh"

TARGET="${1:-/etc/complex/secrets.env}"
[ -f "$TARGET" ] || { echo "[keyvault] 未 provision：$TARGET 不存在（dev 环境正常）"; exit 0; }

fail=0
while read -r k; do
    v="$(grep -E "^$k=" "$TARGET" | head -1 | cut -d= -f2-)"
    if [ -z "$v" ]; then
        echo "[keyvault] 缺失: $k"; fail=1
    elif [ "${#v}" -lt 32 ] || printf '%s' "$v" | grep -q 'change-me'; then
        echo "[keyvault] 弱密钥: $k"; fail=1
    fi
done < <(kv_keys_list)

# 破窗总钥匙公钥为「第一次设置必生成/必配置」项：缺失即视为安装未完成（非可选）。
master_pub="$(grep -E '^COMPLEX_MASTER_AUTHORITY_PUBKEY=' "$TARGET" | head -1 | cut -d= -f2-)"
if [ -z "$master_pub" ]; then
    echo "[keyvault] 缺失: COMPLEX_MASTER_AUTHORITY_PUBKEY（破窗总钥匙未生成/未配置——第一次设置必须生成）"; fail=1
fi

if [ "$fail" -eq 0 ]; then
    echo "[keyvault] PASS：全部必需密钥齐全且非弱"
else
    echo "[keyvault] FAIL"; exit 1
fi
