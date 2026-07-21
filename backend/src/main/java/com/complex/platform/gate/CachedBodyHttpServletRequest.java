package com.complex.platform.gate;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * AUD-010 ④ 缓存请求体，使设备凭证验签能把 body 的 SHA-256 摘要纳入签名量（防截获签名后改体），
 * 同时让下游 Controller 仍能读到同一 body。由 {@link DeviceCredentialBodyCachingFilter} 仅对
 * 「设备绑定型 + 写 + 带签名头」的请求包裹（其余请求零改动）。
 *
 * <p><b>预认证 DoS 防护</b>：body 缓冲上限 {@link #MAX_BODY_BYTES}（1MB，最多读到此上限）。filter 先按
 * Content-Length 早拒超限；无 Content-Length（chunked）时读满上限即止——超限被截断的 body 其 SHA-256
 * 与签名不符，验签 fail-closed 拒绝，Controller 拿不到截断体。资金 JSON 体极小，1MB 远够。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    /** 签名请求 body 缓冲上限（字节）。 */
    public static final int MAX_BODY_BYTES = 1024 * 1024;

    private final byte[] body;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 有上限读取：最多 MAX_BODY_BYTES，防未认证者用超大 body 触发鉴权前全量缓冲。
        this.body = request.getInputStream().readNBytes(MAX_BODY_BYTES);
    }

    public byte[] cachedBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream buffer = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return buffer.read();
            }

            @Override
            public boolean isFinished() {
                return buffer.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("async read not supported for cached body");
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
    }
}
