#!/bin/bash
# 文件版本：V0.01
# 客人 iPhone App 发行包外部资料检查：Xcode「发行包外部资料检查」构建阶段在编译前执行（构建设置作为环境变量传入），只在 Release 配置下检查，
# Debug（模拟器构建与开发调试）不拦。缺任一项或仍为开发值即逐项列出中文原因并退出 1，不产出假的正式包：
#   正式 Bundle ID（PRODUCT_BUNDLE_IDENTIFIER，工程缺省为开发值 com.complex.customer.dev）、正式中文应用名（CUSTOMER_IOS_DISPLAY_NAME，不得带「开发」）、
#   Apple 开发者团队（DEVELOPMENT_TEAM）、正式接口地址（VITE_CUSTOMER_API_BASE_URL：https、非私网与本机，且页面资源须按它打包）、
#   客人深链主机（VITE_CUSTOMER_LINK_HOST：非私网与本机，同时写进通用链接 applinks）。
# 发行构建示例（外部资料到位后）：先带同样的 VITE_* 环境变量构建客人页面并 cap sync ios，再
#   xcodebuild -project App.xcodeproj -scheme App -configuration Release PRODUCT_BUNDLE_IDENTIFIER=… CUSTOMER_IOS_DISPLAY_NAME=… DEVELOPMENT_TEAM=…
#     VITE_CUSTOMER_API_BASE_URL=… VITE_CUSTOMER_LINK_HOST=… archive
# 只用 macOS 自带 bash 3.2 与 BSD 工具也能跑的写法。

[ "${CONFIGURATION:-}" = "Release" ] || exit 0

DEV_BUNDLE_ID="com.complex.customer.dev"
PUBLIC_DIR="${SRCROOT:-$(cd "$(dirname "$0")" && pwd)}/App/public"
problems=""
add() { problems="${problems}error: - $1"$'\n'; }

# 发行包不许连的私网与本机地址：localhost、*.localhost、*.local、10/8、127/8、169.254/16、172.16/12、192.168/16、IPv6 字面量。
is_private_host() {
  local host
  host=$(printf '%s' "$1" | tr 'A-Z' 'a-z')
  case "$host" in
    localhost | *.localhost | *.local | \[*) return 0 ;;
  esac
  printf '%s' "$host" | grep -Eq '^(10|127)(\.[0-9]{1,3}){3}$|^169\.254(\.[0-9]{1,3}){2}$|^172\.(1[6-9]|2[0-9]|3[01])(\.[0-9]{1,3}){2}$|^192\.168(\.[0-9]{1,3}){2}$'
}

bundle_id="${PRODUCT_BUNDLE_IDENTIFIER:-}"
if [ -z "$bundle_id" ]; then
  add "缺正式 Bundle ID（PRODUCT_BUNDLE_IDENTIFIER）"
elif [ "$bundle_id" = "$DEV_BUNDLE_ID" ]; then
  add "正式 Bundle ID 不能用开发值 ${DEV_BUNDLE_ID}（构建时传 PRODUCT_BUNDLE_IDENTIFIER=正式值）"
fi

app_name="${CUSTOMER_IOS_DISPLAY_NAME:-}"
if [ -z "$app_name" ]; then
  add "缺正式中文应用名（CUSTOMER_IOS_DISPLAY_NAME）"
else
  case "$app_name" in
    *开发*) add "正式中文应用名不能带「开发」：${app_name}" ;;
  esac
fi

[ -n "${DEVELOPMENT_TEAM:-}" ] || add "缺 Apple 开发者团队（DEVELOPMENT_TEAM；发行证书与描述文件由发布主体办理）"

api="${VITE_CUSTOMER_API_BASE_URL:-}"
if [ -z "$api" ]; then
  add "缺正式接口地址（VITE_CUSTOMER_API_BASE_URL）"
else
  api_host=$(printf '%s' "$api" | sed -nE 's|^https://([^/:?#]+)(:[0-9]+)?(/.*)?$|\1|p')
  if [ -z "$api_host" ]; then
    add "正式接口地址须为 https：${api}"
  elif is_private_host "$api_host"; then
    add "正式接口地址不能是私网或本机地址：${api}"
  elif ! grep -rqsF --include='*.js' -- "$api" "$PUBLIC_DIR"; then
    add "页面资源不是按正式接口地址打包的（请带发行环境变量重新构建客人页面并 cap sync ios）"
  fi
fi

link_host="${VITE_CUSTOMER_LINK_HOST:-}"
if [ -z "$link_host" ]; then
  add "缺客人深链主机（VITE_CUSTOMER_LINK_HOST）"
elif ! printf '%s' "$link_host" | grep -Eq '^[A-Za-z0-9.-]+$' || is_private_host "$link_host"; then
  add "客人深链主机不能是私网或本机地址（须为正式域名）：${link_host}"
fi

if [ -n "$problems" ]; then
  echo "error: 客人 iPhone App 发行包不能构建（外部资料到位前只能做模拟器调试构建 Debug）："
  printf '%s' "$problems"
  exit 1
fi
