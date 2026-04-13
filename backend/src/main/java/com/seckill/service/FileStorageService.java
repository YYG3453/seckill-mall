package com.seckill.service;

import com.seckill.config.UploadProperties;
import com.seckill.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘图片存储：根目录来自配置 {@link com.seckill.config.UploadProperties}，启动时创建 {@code products/}、{@code avatars/} 子目录。
 * 校验 Content-Type 与 5MB 大小，生成 UUID 文件名，返回可供前端直接使用的 {@code /uploads/...} 路径（由 {@link com.seckill.config.WebMvcConfig} 映射）。
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_CT = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX = 5 * 1024 * 1024;

    private final UploadProperties uploadProperties;
    private Path rootAbsolute;

    /** 解析绝对路径并创建子目录，避免首次上传失败。 */
    @PostConstruct
    public void init() throws IOException {
        rootAbsolute = Paths.get(uploadProperties.getRoot()).toAbsolutePath().normalize();
        Files.createDirectories(rootAbsolute.resolve("products"));
        Files.createDirectories(rootAbsolute.resolve("avatars"));
    }

    /**
     * 保存图片到 {root}/{subDir}/ 下，返回浏览器可访问路径 /uploads/{subDir}/{filename}
     */
    public String storeImage(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }
        if (file.getSize() > MAX) {
            throw new BusinessException("图片不能超过 5MB");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CT.contains(ct.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("仅支持 JPG、PNG、GIF、WEBP");
        }
        String ext = extensionFromContentType(ct);
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = rootAbsolute.resolve(subDir).normalize();
        if (!dir.startsWith(rootAbsolute)) {
            throw new BusinessException("非法路径");
        }
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(dir)) {
                throw new BusinessException("非法路径");
            }
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException("保存文件失败");
        }
        return "/uploads/" + subDir + "/" + name;
    }

    private static String extensionFromContentType(String ct) {
        String c = ct.toLowerCase(Locale.ROOT);
        if (c.contains("png")) {
            return ".png";
        }
        if (c.contains("gif")) {
            return ".gif";
        }
        if (c.contains("webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    public Path getRootAbsolute() {
        return rootAbsolute;
    }
}
