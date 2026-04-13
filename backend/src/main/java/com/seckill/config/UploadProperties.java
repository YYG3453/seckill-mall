package com.seckill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.upload.root}：本地磁盘存储根目录，相对路径相对进程工作目录。
 */
@Data
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    /**
     * 上传根目录（绝对路径或相对运行目录），其下会创建 products、avatars 子目录
     */
    private String root = "upload";
}
