// 文件版本：V0.01
package com.complex.platform.security;

import com.complex.platform.gate.DeviceCredentialVerifier;
import com.complex.platform.gate.TerminalIdentityHolder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * 第 23 条 23-R1 浏览器设备编号。第十八批裁决 6：「浏览器第一次登录时系统发一个设备编号存进这个浏览器并在后台登记，
 * 导出密码和页面暗纹用这个编号（清浏览器数据或换浏览器算新设备），不用装软件」。
 *
 * <p>编号存在 HttpOnly Cookie {@value #COOKIE}（Path=/、SameSite=Strict，Secure 按请求是否 https，同 {@code AuthCookies}），
 * 登记在 browser_devices。登出不清此 Cookie（{@code AuthCookies.clearSession} 只清三个会话 Cookie），否则每次重新登录都成了新设备。
 */
@Component
public class BrowserDeviceRegistry {

    public static final String COOKIE = "complex_device_no";
    // 去掉易混的 0、O、1、I、L，共 31 个字符。
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final Duration MAX_AGE = Duration.ofDays(400);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;

    public BrowserDeviceRegistry(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 是不是浏览器登录（才发编号）：请求体带 device_id 的、或请求头 X-Terminal-Type 报员工工作手机、员工私人手机、平板、一体机（设备绑定型终端）的
     * 设备端登录不发、不登记（核对稿 23-R1 ③「Android 端……不发浏览器编号」；这些设备的暗纹与导出取验签后的登记设备号，见 {@link #current}）。
     */
    public static boolean browserLogin(HttpServletRequest request, String deviceId) {
        String terminal = request.getHeader("X-Terminal-Type");
        return (deviceId == null || deviceId.isBlank())
                && (terminal == null || !DeviceCredentialVerifier.DEVICE_BOUND_TYPES.contains(terminal.trim()));
    }

    /**
     * 网页登录成功后调用：Cookie 里的编号已登记就沿用，没有或查不到（伪造、清过）就发 WB＋12 位新号并登记；
     * 每次登录都重写同值 Cookie 续期 400 天。返回本浏览器的设备编号。
     */
    public String ensure(HttpServletRequest request, HttpServletResponse response, String userId) {
        String deviceNo = registered(request).orElseGet(() -> {
            String issued = newDeviceNo();
            jdbcTemplate.update("INSERT INTO browser_devices (device_no, first_user_id) VALUES (?, ?)", issued, userId);
            return issued;
        });
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE, deviceNo)
                .httpOnly(true).secure(request.isSecure()).sameSite("Strict").path("/").maxAge(MAX_AGE).build().toString());
        return deviceNo;
    }

    /**
     * 当前请求的设备编号：员工手机经设备凭证验签的取其登记设备号；否则取浏览器 Cookie 里、且在 browser_devices 登记过的编号；
     * 都没有为空（调用方按「请重新登录后再打开」处理）。
     */
    public Optional<String> current(HttpServletRequest request) {
        return TerminalIdentityHolder.current()
                .filter(TerminalIdentityHolder.VerifiedTerminal::verified)
                .map(TerminalIdentityHolder.VerifiedTerminal::deviceId)
                .filter(id -> !id.isBlank())
                .or(() -> registered(request));
    }

    /** 当前请求的接入网络：来源地址（TLS 终止反代由 server.forward-headers-strategy=framework 还原真实来源）。 */
    public static String network(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private Optional<String> registered(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE);
        String value = cookie == null ? null : cookie.getValue();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Boolean known = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM browser_devices WHERE device_no = ?)", Boolean.class, value);
        return Boolean.TRUE.equals(known) ? Optional.of(value) : Optional.empty();
    }

    private static String newDeviceNo() {
        StringBuilder deviceNo = new StringBuilder("WB");
        for (int i = 0; i < 12; i++) {
            deviceNo.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return deviceNo.toString();
    }
}
