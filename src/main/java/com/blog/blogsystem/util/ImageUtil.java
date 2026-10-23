package com.blog.blogsystem.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

/**
 * 图片处理工具：格式识别 + 解码前尺寸校验 + 服务端重编码 + 按需降采样。
 * 重编码可剥离图片尾部的 polyglot 载荷与 EXIF 隐藏数据；
 * 解码前先通过 ImageReader 读取宽高，防止小文件解压出超大位图（解压炸弹）。
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public final class ImageUtil {

    /** 允许的最大像素数（约 40MP），防止解压炸弹 */
    private static final long MAX_PIXELS = 40_000_000L;

    static {
        // 只读容器内不落磁盘缓存，全部走内存
        ImageIO.setUseCache(false);
    }

    private ImageUtil() {}

    /**
     * 通过 magic bytes 识别图片类型。WebP 不在允许列表（JDK 无内置解码器，无法安全重编码）。
     *
     * @return jpg/png/gif，无法识别返回 null
     */
    public static String detectImageType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return "jpg";
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "png";
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) return "gif";
        return null;
    }

    /**
     * 服务端重新编码图片（jpg/png/gif），剥离附加数据。
     *
     * @throws IllegalArgumentException 图片无法解析或尺寸超限
     */
    public static byte[] sanitizeImage(byte[] bytes, String ext) {
        return sanitizeImage(bytes, ext, 0);
    }

    /**
     * 服务端重新编码图片，且长边超过 maxDimension 时按比例降采样（maxDimension<=0 表示不压缩）。
     *
     * @throws IllegalArgumentException 图片无法解析或尺寸超限
     */
    public static byte[] sanitizeImage(byte[] bytes, String ext, int maxDimension) {
        BufferedImage img = readImage(bytes);
        if (maxDimension > 0 && Math.max(img.getWidth(), img.getHeight()) > maxDimension) {
            img = resize(img, maxDimension);
        }
        String format = "jpg".equalsIgnoreCase(ext) ? "jpg"
                : "png".equalsIgnoreCase(ext) ? "png" : "gif";
        try {
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

    private static BufferedImage readImage(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("图片数据为空");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IllegalArgumentException("图片数据无法解析");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("图片数据无法解析");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                // 先读元数据，超限时在 decode 分配内存前直接拒绝
                validateDimensions(reader.getWidth(0), reader.getHeight(0));
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("图片数据无法解析");
                }
                validateDimensions(image.getWidth(), image.getHeight());
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("图片重编码失败", e);
        }
    }

    private static void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("图片尺寸无效");
        }
        long pixels = (long) width * height;
        if (pixels > MAX_PIXELS) {
            throw new IllegalArgumentException("图片尺寸超限");
        }
    }

    private static BufferedImage resize(BufferedImage src, int maxDimension) {
        double scale = (double) maxDimension / Math.max(src.getWidth(), src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

}
