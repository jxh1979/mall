# 文件版本：V0.01
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

# ROOT-06 安装包用：列出插着的 U 盘目录。先看系统已挂载的 media 目录，再列已挂在别处（如 /mnt）的可移动盘；服务器不自动挂载时，
# 把未挂载的可移动盘按 $1（ro|rw）挂到 ${COMPLEX_INSTALL_ROOT}/run/complex-usb/<名> 下。用完调 kv_usb_release 卸载。
kv_usb_dirs() {
    local root="${COMPLEX_INSTALL_ROOT:-}" d name rm type mnt
    for d in "$root"/media/*/* "$root"/run/media/*/*; do
        [ -d "$d" ] && printf '%s\n' "$d"
    done
    command -v lsblk >/dev/null 2>&1 || return 0
    lsblk -nrpo NAME,RM,TYPE,MOUNTPOINT | while read -r name rm type mnt; do
        [ "$rm" = 1 ] && { [ "$type" = part ] || [ "$type" = disk ]; } || continue
        [ -n "$mnt" ] && { printf '%s\n' "$root$mnt"; continue; }
        d="$root/run/complex-usb/$(basename "$name")"
        mkdir -p "$d" && mount -o "$1" "$name" "$d" 2>/dev/null && printf '%s\n' "$d"
    done
}

# 授权 U 盘＝含 complex-keys/keystore.env.enc 的 U 盘（由 U盘首次制钥.sh 制成）。全平台只有一把钥匙（不预制备用），取找到的第一个。
kv_find_usb() {
    local d
    while read -r d; do
        [ -f "$d/complex-keys/keystore.env.enc" ] && { printf '%s\n' "$d"; return 0; }
    done < <(kv_usb_dirs ro)
    return 0
}

kv_usb_release() {
    local d
    for d in "${COMPLEX_INSTALL_ROOT:-}"/run/complex-usb/*; do
        [ -d "$d" ] || continue
        umount "$d" 2>/dev/null || true
        rmdir "$d" 2>/dev/null || true
    done
    return 0
}
