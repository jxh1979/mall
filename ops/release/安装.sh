#!/usr/bin/env bash
# 文件版本：V0.01
# 综合体管理系统安装向导（ROOT-06；BSM:1973「安装时自己识别当前是测试环境还是生产环境，并自动完成相应的配置」）。
# 环境识别（第十三批操作员裁决第 6 条「插了授权 U 盘即生产」）：检测到授权 U 盘就装生产，否则装测试；已装环境与本次识别不一致即停止。
# 生产首装从授权 U 盘换上强密钥（BSM:1965、1966，运行时不依赖 U 盘）；测试首装在本机生成测试密钥。升级沿用本机现有密钥。
# 每一步失败都停在原地并给出中文提示（对照见 教程/使用教程.html 第六章）。第 1、2 步不写任何文件。
# COMPLEX_INSTALL_ROOT 仅供沙箱演练（tools/release/release-package.test.mjs），正式安装不设。
set -euo pipefail
PKG="$(cd "$(dirname "$0")" && pwd)"
ROOT="${COMPLEX_INSTALL_ROOT:-}"
ETC="$ROOT/etc/complex"
OPT="$ROOT/opt/complex"
. "$PKG/keyvault/lib.sh"

stop() { echo "安装停止：$1" >&2; exit 1; }
step() { echo "[$1/9] $2"; }
label() { if [ "$1" = prod ]; then echo 生产环境; else echo 测试环境; fi; }

[ "$(id -u)" = 0 ] || stop "请用 sudo 运行安装（sudo ./安装.sh）"
trap kv_usb_release EXIT

step 1 校验安装包
(cd "$PKG" && sha256sum -c SHA256SUMS --quiet >/dev/null 2>&1) || stop "安装包已损坏或被改动，请重新拷贝"
. "$PKG/config/install.env"

step 2 识别环境
USB="$(kv_find_usb)"
if [ -n "$USB" ]; then ENV=prod; else ENV=test; fi
echo "本机识别为：$(label "$ENV")（$( [ -n "$USB" ] && echo "检测到授权 U 盘" || echo "未检测到授权 U 盘" )）"
# install-state.env：第 4 步换好密钥即写 INSTALLED_ENV（之后失败可原样重跑），装成功才写 INSTALLED_VERSION。
INSTALLED_ENV=""; INSTALLED_VERSION=""
if [ -f "$ETC/install-state.env" ]; then
    . "$ETC/install-state.env"
    if [ "$INSTALLED_ENV" != "$ENV" ]; then
        echo "本机已安装为：$(label "$INSTALLED_ENV")；本次识别为：$(label "$ENV")"
        stop "已装环境与本次识别不一致，不能直接改装"
    fi
elif [ -f "$ETC/secrets.env" ]; then
    stop "本机已有人工部署的系统，安装器不接管，请联系 IT"
fi
case " $SUPPORTED_ENVS " in
    *" $ENV "*) ;;
    *) reason="EXCLUDED_$ENV"; echo "本安装包不支持$(label "$ENV")：${!reason:-}"
       stop "本安装包不支持本机环境，请联系 IT 重新出包" ;;
esac
host_var="${ENV}_PUBLIC_HOST"; cert_var="${ENV}_TLS_CERT"; key_var="${ENV}_TLS_KEY"
PUBLIC_HOST="${!host_var}"; TLS_CERT="${!cert_var}"; TLS_KEY="${!key_var}"
[ -f "$ROOT$TLS_CERT" ] && [ -f "$ROOT$TLS_KEY" ] || stop "本机缺网站证书，请联系 IT 放好证书后重新运行"

step 3 检查必需软件
missing() {
    local c m=""
    for c in java nginx psql openssl systemctl sha256sum curl; do command -v "$c" >/dev/null 2>&1 || m="$m $c"; done
    java -version 2>&1 | grep -qE 'version "(2[1-9]|[3-9][0-9])' || m="$m java21"
    printf '%s' "$m"
}
if [ -n "$(missing)" ] && command -v apt-get >/dev/null 2>&1; then
    apt-get update -q >/dev/null 2>&1 || true
    apt-get install -y openjdk-21-jre-headless nginx postgresql openssl curl >/dev/null 2>&1 || true
fi
if [ -n "$(missing)" ]; then
    echo "缺少：$(missing)"
    stop "缺少必需软件且无法自动安装，请联系 IT"
fi

step 4 准备运行密钥
if [ -n "$INSTALLED_ENV" ]; then
    SECRETS_SOURCE="$INSTALLED_SECRETS_SOURCE"
    echo "沿用本机现有运行密钥"
elif [ "$ENV" = test ]; then
    mkdir -p "$ETC/test-keys"
    (umask 077; openssl rand -hex 24 > "$ETC/test-keys/passphrase")
    COMPLEX_KEYVAULT_PASS="$(cat "$ETC/test-keys/passphrase")" bash "$PKG/keyvault/generate-keystore.sh" "$ETC/test-keys" >/dev/null
    COMPLEX_KEYVAULT_PASS="$(cat "$ETC/test-keys/passphrase")" bash "$PKG/keyvault/provision-from-usb.sh" "$ETC/test-keys" "$ETC/secrets.env" >/dev/null
    SECRETS_SOURCE=test-local
else
    echo "请授权 U 盘保管人输入钥匙口令"
    bash "$PKG/keyvault/provision-from-usb.sh" "$USB" "$ETC/secrets.env" >/dev/null \
        || stop "U 盘钥匙口令不对或钥匙已损坏，请保管人重新运行并输入正确口令"
    SECRETS_SOURCE=usb
fi
bash "$PKG/keyvault/verify-provisioned.sh" "$ETC/secrets.env" >/dev/null || stop "运行密钥校验未通过，请联系 IT"
[ -n "$INSTALLED_ENV" ] || printf 'INSTALLED_ENV=%s\nINSTALLED_SECRETS_SOURCE=%s\n' "$ENV" "$SECRETS_SOURCE" > "$ETC/install-state.env"

step 5 准备数据库
if [ ! -f "$ETC/db.env" ]; then
    owner_pw="$(openssl rand -hex 16)"; app_pw="$(openssl rand -hex 16)"
    # 表权限由迁移 V140 授予 complex_app；不用开发默认 complex/complex_dev_pwd（ProductionDatabaseCredentialGuard）。
    runuser -u postgres -- psql -v ON_ERROR_STOP=1 -q <<SQL >/dev/null 2>&1 || stop "数据库准备失败，请联系 IT"
CREATE ROLE complex_owner LOGIN PASSWORD '$owner_pw';
CREATE ROLE complex_app LOGIN NOINHERIT PASSWORD '$app_pw';
CREATE DATABASE complex OWNER complex_owner;
SQL
    (umask 077; cat > "$ETC/db.env" <<EOF
COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex
COMPLEX_DATABASE_OWNER_USERNAME=complex_owner
COMPLEX_DATABASE_OWNER_PASSWORD=$owner_pw
COMPLEX_DATABASE_APP_USERNAME=complex_app
COMPLEX_DATABASE_APP_PASSWORD=$app_pw
EOF
    )
fi

step 6 写入系统配置
if [ -z "$INSTALLED_VERSION" ] && [ ! -f "$ETC/initial-admin.env" ]; then
    # 首装建第一个平台管理员：复用后端 seed-admin（口令只在首次写入）；装好后删除本文件。生产不靠破窗建账号。
    (umask 077; printf 'COMPLEX_AUTH_PRODUCTION_SEED_ADMIN_USERNAME=admin\nCOMPLEX_AUTH_PRODUCTION_SEED_ADMIN_PASSWORD=%s\n' \
        "$(openssl rand -base64 30 | tr -d '/+=\n' | cut -c1-20)" > "$ETC/initial-admin.env")
fi
cat > "$ETC/complex.env" <<EOF
SPRING_PROFILES_ACTIVE=prod
COMPLEX_API_HOST=127.0.0.1
COMPLEX_API_PORT=8080
COMPLEX_API_VERSION=$PACKAGE_VERSION
COMPLEX_API_BUILD_TIME=$PACKAGE_BUILT_AT
COMPLEX_EVIDENCE_ATTACHMENT_DIR=/var/lib/complex/evidence-files
COMPLEX_BAR_POS_IMAGE_DIR=/var/lib/complex/bar-images
EOF
id complex >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin complex
mkdir -p "$ROOT/var/lib/complex/evidence-files" "$ROOT/var/lib/complex/bar-images" "$ROOT/var/log/complex" "$ROOT/etc/systemd/system" "$ROOT/etc/nginx/sites-enabled"
chown -R complex:complex "$ROOT/var/lib/complex"
sed "s|@JAVA@|$(command -v java)|" "$PKG/config/complex-api.service.tmpl" > "$ROOT/etc/systemd/system/complex-api.service"
SITE="$ROOT/etc/nginx/sites-enabled/complex"
OLD_SITE="$(cat "$SITE" 2>/dev/null || true)"
rm -f "$ROOT/etc/nginx/sites-enabled/default"
sed -e "s|@PUBLIC_HOST@|$PUBLIC_HOST|" -e "s|@TLS_CERT@|$TLS_CERT|" -e "s|@TLS_KEY@|$TLS_KEY|" "$PKG/config/nginx-complex.conf.tmpl" > "$SITE"
if ! nginx -t >/dev/null 2>&1; then
    if [ -n "$OLD_SITE" ]; then printf '%s\n' "$OLD_SITE" > "$SITE"; else rm -f "$SITE"; fi
    stop "网站配置检查未通过，请联系 IT"
fi

step 7 放置新版本
REL="$PACKAGE_VERSION"
declare -A PREV=()
for part in api web clients; do
    PREV[$part]="$(readlink "$OPT/$part/current" 2>/dev/null || true)"
    mkdir -p "$OPT/$part/releases/$REL"
done
cp "$PKG/server/complex-api.jar" "$OPT/api/releases/$REL/complex-api.jar"
mkdir -p "$OPT/web/releases/$REL/dist" && cp -a "$PKG/server/web/." "$OPT/web/releases/$REL/dist/"
cp "$PKG/clients/staff-android/complex-terminal-$ENV.apk" "$OPT/clients/releases/$REL/complex-terminal.apk"
for part in api web clients; do ln -sfn "releases/$REL" "$OPT/$part/current"; done

step 8 启动并检查
healthy() {
    local i
    for i in $(seq 60); do
        curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null 2>&1 && return 0
        sleep 2
    done
    return 1
}
systemctl daemon-reload
systemctl enable complex-api >/dev/null 2>&1
systemctl restart complex-api
systemctl restart nginx
if ! healthy; then
    if [ -n "${PREV[api]}" ] && [ "${PREV[api]}" != "releases/$REL" ]; then
        for part in api web clients; do [ -n "${PREV[$part]}" ] && ln -sfn "${PREV[$part]}" "$OPT/$part/current"; done
        systemctl restart complex-api
        echo "上一版本：${PREV[api]#releases/}"
        stop "新版本启动失败，已自动恢复到上一版本"
    fi
    journalctl -u complex-api -n 80 --no-pager || true
    stop "系统启动失败，请把屏幕上的内容拍照发给 IT"
fi

step 9 收尾
ADMIN_PW=""
if [ -f "$ETC/initial-admin.env" ]; then
    ADMIN_PW="$(sed -n 's/^COMPLEX_AUTH_PRODUCTION_SEED_ADMIN_PASSWORD=//p' "$ETC/initial-admin.env")"
    rm -f "$ETC/initial-admin.env"
fi
cat > "$ETC/install-state.env" <<EOF
INSTALLED_ENV=$ENV
INSTALLED_VERSION=$REL
INSTALLED_AT=$(date -Iseconds)
INSTALLED_PREVIOUS_RELEASE=${PREV[api]#releases/}
INSTALLED_SECRETS_SOURCE=$SECRETS_SOURCE
EOF
# 留痕（BSM:1967）：谁、什么时间、装成什么环境、密钥来源与破窗公钥指纹（与 MasterAuthorityService.fingerprint 同算法：SPKI DER 的 SHA-256 前 16 位）。
FINGERPRINT="$(sed -n 's/^COMPLEX_MASTER_AUTHORITY_PUBKEY=//p' "$ETC/secrets.env" | base64 -d | sha256sum | cut -c1-16)"
printf '%s 执行人=%s 环境=%s 版本=%s 动作=%s 密钥来源=%s 破窗公钥指纹=%s\n' "$(date -Iseconds)" "${SUDO_USER:-root}" "$(label "$ENV")" \
    "$REL" "$( [ -z "$INSTALLED_VERSION" ] && echo 首装 || echo 升级 )" "$SECRETS_SOURCE" "$FINGERPRINT" >> "$ROOT/var/log/complex/install.log"
mkdir -p "$OPT/docs" && cp "$PKG/教程/使用教程.html" "$OPT/docs/使用教程.html"

echo "安装完成：$(label "$ENV")，版本 $REL"
echo "后台网址：https://$PUBLIC_HOST/"
if [ -n "$ADMIN_PW" ]; then
    echo "初始管理员账号：admin"
    echo "初始管理员口令：$ADMIN_PW（只显示这一次，请立即登录后台修改并为各岗位建账号）"
fi
if [ "$ENV" = prod ]; then
    echo "请拔出 U 盘交保管人；本次插盘托管系统已自动留痕，另按教程第四章记入安全基线台账。"
fi
