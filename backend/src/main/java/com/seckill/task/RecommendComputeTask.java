package com.seckill.task;

import com.seckill.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 推荐离线计算：启动时与每小时重算「商品相似度」，结果写入 Redis ZSet，供在线接口快速读取。
 * 计算较重，放在定时任务里避免用户请求时全表两两比对。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendComputeTask {

    private final RecommendService recommendService;

    /**
     * 应用启动完成后立即尝试建一次相似度矩阵；失败只打日志，不阻止应用启动。
     */
    @PostConstruct
    public void init() {
        try {
            recommendService.rebuildSimilarityMatrix();
        } catch (Exception e) {
            log.warn("recommend init failed", e);
        }
    }

    /**
     * 每小时 15 分 0 秒全量重算：{@code 0 15 * * * ?} 表示每小时一次，在 minute=15。
     */
    @Scheduled(cron = "0 15 * * * ?")
    public void hourly() {
        recommendService.rebuildSimilarityMatrix();
    }
}
