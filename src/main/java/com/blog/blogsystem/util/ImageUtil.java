package com.blog.blogsystem.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 图片处理工具：格式识别 + 服务端重编码
 * 重编码可剥离图片尾部的 polyglot 载荷与 EXIF 隐藏数据，防止恶意文件绕过 magic bytes 检测
 */
public final class ImageUtil {

    /** 允许的最大像素数（约 40MP），防止解压炸弹 */
    private static final long MAX_PIXELS = 40_000_000L;

    private ImageUtil() {}

    /**
     * 通过 magic bytes 识别图片类型
     * @return jpg/png/gif/webp，无法识别返回 null
     */
    public static String detectImageType(byte[] bytes) {
        if (bytes.length >= 12) {
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return "jpg";
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "png";
            if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) return "gif";
            if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                    && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return "webp";
        }
        return null;
    }

    /**
     * 服务端重新编码图片（jpg/png/gif），剥离附加数据；webp 暂不支持重编码，原样返回
     * @throws IllegalArgumentException 图片无法解析或尺寸超限
     */
    public static byte[] sanitizeImage(byte[] bytes, String ext) {
        if ("webp".equalsIgnoreCase(ext)) {
            return bytes;
        }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new IllegalArgumentException("图片数据无法解析");
            }
            long pixels = (long) img.getWidth() * img.getHeight();
            if (img.getWidth() <= 0 || img.getHeight() <= 0 || pixels > MAX_PIXELS) {
                throw new IllegalArgumentException("图片尺寸超限");
            }
            String format = "jpg".equalsIgnoreCase(ext) ? "jpg"
                    : "png".equalsIgnoreCase(ext) ? "png" : "gif";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(img, format, out)) {
                throw new IllegalArgumentException("图片格式不支持");
            }
            return out.toByteArray();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("图片重编码失败", e);
        }
    }

}
