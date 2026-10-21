#!/usr/bin/env bash
# 文件版本：V0.01
# 正式上线前由最高权限本人运行一次：在一个空 U 盘上做出授权钥匙（BSM:1966「密钥母本只在授权 U 盘里」）。
# 已有钥匙的 U 盘不能重做（全平台只有一把钥匙、不预制备用；更换走钥匙更换闭环）。口令至少 12 位、输两次一致。
# COMPLEX_INSTALL_ROOT 仅供沙箱演练，正式运行不设。
set -euo pipefail
PKG="$(cd "$(dirname "$0")" && pwd)"
. "$PKG/keyvault/lib.sh"

stop() { echo "制钥停止：$1" >&2; exit 1; }

[ "$(id -u)" = 0 ] || stop "请用 sudo 运行制钥（sudo ./U盘首次制钥.sh）"
trap kv_usb_release EXIT

mapfile -t DIRS < <(kv_usb_dirs rw)
[ "${#DIRS[@]}" -eq 1 ] || stop "请只插入一个要做钥匙的 U 盘后重新运行"
USB="${DIRS[0]}"
[ ! -e "$USB/complex-keys" ] || stop "此 U 盘已有钥匙，不能重做；更换钥匙请联系 IT 按钥匙更换闭环办理"

P1="$(kv_get_pass '请设置钥匙口令（至少 12 位）: ')"
P2="$(kv_get_pass '请再输入一次口令: ')"
[ "${#P1}" -ge 12 ] || stop "口令至少 12 位，请重新运行"
[ "$P1" = "$P2" ] || stop "两次输入的口令不一致，请重新运行"
COMPLEX_KEYVAULT_PASS="$P1" bash "$PKG/keyvault/generate-keystore.sh" "$USB" >/dev/null

echo "钥匙已做好。口令请保管人自己记住，不要写在 U 盘上。"
echo "请把本次启用 U 盘托管的时间、操作人、见证人记入安全基线台账，然后插着这个 U 盘运行 sudo ./安装.sh 安装生产环境。"
