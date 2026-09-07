#!/usr/bin/env bash
# 只读校验：EnvironmentFile 是否含 keys.list 全部密钥且非弱（>=32 且不含 change-me），以及破窗验签公钥已配置。
# M008-F-05（BSM-1.26.1「生产禁止弱密钥与开发默认凭据」）：目标文件不存在即 FAIL——未 provision 不等于通过；
# dev 机器要跳过须显式传 --allow-missing（第二参数），生产不得传。
# 用法: verify-provisioned.sh [target_env_file] [--allow-missing]
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/lib.sh"

TARGET="${1:-/etc/complex/secrets.env}"
ALLOW_MISSING="${2:-}"
if [ ! -f "$TARGET" ]; then
    if [ "$ALLOW_MISSING" = "--allow-missing" ]; then
        echo "[keyvault] 未 provision：$TARGET 不存在（--allow-missing 显式允许，仅 dev）"; exit 0
    fi
    echo "[keyvault] FAIL：$TARGET 不存在——未 provision 即视为安装未完成（生产禁止弱密钥 / 缺密钥）；dev 请显式传 --allow-missing" >&2
    exit 1
fi

fail=0
while read -r k; do
    v="$(grep -E "^$k=" "$TARGET" | head -1 | cut -d= -f2-)"
    if [ -z "$v" ]; then
        echo "[keyvault] 缺失: $k"; fail=1
    elif [ "${#v}" -lt 32 ] || printf '%s' "$v" | grep -q 'change-me'; then
        echo "[keyvault] 弱密钥: $k"; fail=1
    fi
done < <(kv_keys_list)

# 破窗验签公钥为「第一次设置必生成/必配置」项：缺失即视为安装未完成（非可选）。R-004：服务器只留验签公钥，私钥永不落服务器。
master_pub="$(grep -E '^COMPLEX_MASTER_AUTHORITY_PUBKEY=' "$TARGET" | head -1 | cut -d= -f2-)"
if [ -z "$master_pub" ]; then
    echo "[keyvault] 缺失: COMPLEX_MASTER_AUTHORITY_PUBKEY（破窗验签公钥未生成/未配置——第一次设置必须生成）"; fail=1
fi

if [ "$fail" -eq 0 ]; then
    echo "[keyvault] PASS：全部必需密钥齐全且非弱"
else
    echo "[keyvault] FAIL"; exit 1
fi
