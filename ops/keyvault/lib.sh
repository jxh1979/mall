# keyvault ops 脚本共享助手（只 source，不直接执行）。
# 口令来源：优先 env COMPLEX_KEYVAULT_PASS（非交互/自检），否则从 /dev/tty 交互读取。

kv_get_pass() {
    # $1 = 交互提示语
    if [ -n "${COMPLEX_KEYVAULT_PASS:-}" ]; then
        printf '%s' "$COMPLEX_KEYVAULT_PASS"
    else
        local p
        read -rsp "$1" p </dev/tty
        printf '\n' >&2
        printf '%s' "$p"
    fi
}

# 输出 keys.list 中的密钥名（跳过注释与空行）。
kv_keys_list() {
    grep -vE '^\s*(#|$)' "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/keys.list"
}
