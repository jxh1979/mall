// 文件版本：V0.01
package com.complex.platform.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 第 23 条 23-R2 暗纹二维码（PGM:687「暗纹可反向推算：把页面的截屏图片、或对着屏幕拍摄的照片上传到系统，即可算出……」）。
 * 页面暗纹（前端 PageWatermark.tsx）与导出文件暗纹（23-R3）都把 {@link WatermarkMarkService} 发的码画成纠错等级 H 的二维码平铺，
 * 低不透明度叠在内容上；反查时从截屏或拍屏照片里把码解回来。
 *
 * <p>解码：灰度 → 3×3 平滑 → 按块估背景（浅底取块内最亮、深底取最暗）→ 与背景差超过阈值的记为模块；差值远大于暗纹本身的是页面文字，
 * 先抹掉再按邻近像素补回，免得文字笔画把模块盖坏 → 浅底、深底两种极性各试几档阈值，zxing 多码读取任一可解的码。
 */
public final class WatermarkQr {

    /** 与前端 PageWatermark.tsx 同值：平铺间距、模块边长、二维码在每格内的偏移（CSS 像素）与不透明度。 */
    public static final int TILE_PX = 200;
    public static final int MODULE_PX = 6;
    public static final int OFFSET_PX = 24;
    public static final double OPACITY = 0.08;

    private static final int BLOCK = 32;
    private static final int[] THRESHOLDS = {4, 6, 9, 13, 18};
    // ponytail: 超大照片先缩到长边 2000 像素再解，控住内存与耗时；屏幕照片里模块仍有 5 像素以上。
    private static final int MAX_SIDE = 2000;

    private WatermarkQr() {
    }

    /** 码的二维码模块矩阵（纠错 H、无静区，每格一个模块）。 */
    public static BitMatrix encode(String code) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 0);
        try {
            return new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 0, 0, hints);
        } catch (WriterException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** 从截屏或拍屏照片里解出的码（可能为空）。 */
    public static Set<String> decode(BufferedImage image) {
        BufferedImage source = shrink(image);
        int w = source.getWidth();
        int h = source.getHeight();
        int[] lum = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = source.getRGB(x, y);
                lum[y * w + x] = (((p >> 16) & 255) * 299 + ((p >> 8) & 255) * 587 + (p & 255) * 114) / 1000;
            }
        }
        int[] smooth = box(lum, w, h);
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        Set<String> found = new LinkedHashSet<>();
        for (boolean lightBackground : new boolean[] {true, false}) {
            int[] bg = background(smooth, w, h, lightBackground);
            for (int threshold : THRESHOLDS) {
                byte[] bits = new byte[w * h];
                boolean[] text = new boolean[w * h];
                for (int i = 0; i < bits.length; i++) {
                    int d = lightBackground ? bg[i] - smooth[i] : smooth[i] - bg[i];
                    int raw = lightBackground ? bg[i] - lum[i] : lum[i] - bg[i];
                    bits[i] = (byte) (d >= threshold ? 0 : 255);
                    text[i] = raw >= threshold * 3;
                }
                inpaint(bits, text, w, h);
                try {
                    for (Result result : new QRCodeMultiReader().decodeMultiple(new BinaryBitmap(new HybridBinarizer(
                            new PlanarYUVLuminanceSource(bits, w, h, 0, 0, w, h, false))), hints)) {
                        found.add(result.getText());
                    }
                } catch (NotFoundException ignored) {
                    // 这一档解不出，换下一档
                }
                if (!found.isEmpty()) {
                    return found;
                }
            }
        }
        return found;
    }

    private static BufferedImage shrink(BufferedImage image) {
        int side = Math.max(image.getWidth(), image.getHeight());
        if (side <= MAX_SIDE) {
            return image;
        }
        double f = (double) MAX_SIDE / side;
        BufferedImage out = new BufferedImage((int) (image.getWidth() * f), (int) (image.getHeight() * f), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    private static int[] box(int[] v, int w, int h) {
        int[] out = new int[v.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sum = 0;
                int n = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int yy = y + dy;
                        int xx = x + dx;
                        if (yy >= 0 && yy < h && xx >= 0 && xx < w) {
                            sum += v[yy * w + xx];
                            n++;
                        }
                    }
                }
                out[y * w + x] = sum / n;
            }
        }
        return out;
    }

    /** 每块取最亮（浅底）或最暗（深底），再在相邻 3×3 块里取一次，免得整块落在模块里时把模块当成背景。 */
    private static int[] background(int[] s, int w, int h, boolean light) {
        int bw = (w + BLOCK - 1) / BLOCK;
        int bh = (h + BLOCK - 1) / BLOCK;
        int[] block = new int[bw * bh];
        java.util.Arrays.fill(block, light ? 0 : 255);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int b = (y / BLOCK) * bw + x / BLOCK;
                block[b] = light ? Math.max(block[b], s[y * w + x]) : Math.min(block[b], s[y * w + x]);
            }
        }
        int[] near = new int[block.length];
        for (int by = 0; by < bh; by++) {
            for (int bx = 0; bx < bw; bx++) {
                int v = light ? 0 : 255;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int yy = by + dy;
                        int xx = bx + dx;
                        if (yy >= 0 && yy < bh && xx >= 0 && xx < bw) {
                            v = light ? Math.max(v, block[yy * bw + xx]) : Math.min(v, block[yy * bw + xx]);
                        }
                    }
                }
                near[by * bw + bx] = v;
            }
        }
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out[y * w + x] = near[(y / BLOCK) * bw + x / BLOCK];
            }
        }
        return out;
    }

    /** 文字像素（连同外扩一圈）抹掉，按周围已知像素多数补回，最多八轮；补不到的当背景。 */
    private static void inpaint(byte[] bits, boolean[] text, int w, int h) {
        boolean[] unknown = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!text[y * w + x]) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int yy = y + dy;
                        int xx = x + dx;
                        if (yy >= 0 && yy < h && xx >= 0 && xx < w) {
                            unknown[yy * w + xx] = true;
                        }
                    }
                }
            }
        }
        for (int pass = 0; pass < 8; pass++) {
            boolean[] next = unknown.clone();
            byte[] filled = bits.clone();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    if (!unknown[i]) {
                        continue;
                    }
                    int dark = 0;
                    int known = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int yy = y + dy;
                            int xx = x + dx;
                            if (yy < 0 || yy >= h || xx < 0 || xx >= w || unknown[yy * w + xx]) {
                                continue;
                            }
                            known++;
                            if (bits[yy * w + xx] == 0) {
                                dark++;
                            }
                        }
                    }
                    if (known > 0) {
                        filled[i] = (byte) (dark * 2 > known ? 0 : 255);
                        next[i] = false;
                    }
                }
            }
            System.arraycopy(filled, 0, bits, 0, bits.length);
            unknown = next;
        }
        for (int i = 0; i < bits.length; i++) {
            if (unknown[i]) {
                bits[i] = (byte) 255;
            }
        }
    }
}
