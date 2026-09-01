package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.util.ImageUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cover")
public class CoverController {

    private static final long MAX_SIZE = 2 * 1024 * 1024;
    /** 封面长边最大 1600px，超过按比例降采样（降低存储与带宽） */
    private static final int COVER_MAX_DIMENSION = 1600;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> upload(@RequestAttribute("userId") Integer userId,
                                                       @RequestBody Map<String, String> body) {
        try {
            String image = body.get("image");
            if (image == null || image.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("请提供图片"));
            }
            if (image.length() > (int)(MAX_SIZE * 1.4) + 100) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("图片不能超过 2MB"));
            }
            if (image.contains(",")) image = image.substring(image.indexOf(",") + 1);
            byte[] bytes = Base64.getDecoder().decode(image);
            if (bytes.length > MAX_SIZE) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("图片不能超过 2MB"));
            }
            String ext = ImageUtil.detectImageType(bytes);
            if (ext == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("不支持的图片格式，仅支持 JPG/PNG/GIF/WEBP"));
            }
            // 服务端重编码，剥离 polyglot 载荷；长边超限降采样
            bytes = ImageUtil.sanitizeImage(bytes, ext, COVER_MAX_DIMENSION);
            File dir = new File("uploads");
            if (!dir.exists()) dir.mkdirs();
            String filename = "cover_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            java.nio.file.Files.write(new File(dir, filename).toPath(), bytes);
            return ResponseEntity.ok(ApiResponse.success("上传成功", "/uploads/" + filename));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("图片数据格式错误"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail("上传失败"));
        }
    }

}
