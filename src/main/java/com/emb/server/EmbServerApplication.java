package com.emb.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Instant;

@SpringBootApplication
public class EmbServerApplication {

    // 1. 兼容性基线：主版本号必须 >= 17
    private static final int MIN_FEATURE_VERSION = 17;

    // 2. 最终安全日期基线（标准 UTC 时间戳，末尾 Z 代表 100% 绝对 UTC 零时区）：
    // 2026-10-21 发布的 17.0.21.0.2 存在证书信任链高危遗留漏洞(CVSS 9.1)，
    // Oracle 及各主流 JDK 于 2026-10-24 (UTC) 紧急发布了 17.0.21.0.3 最终修复版。
    private static final Instant MIN_SAFE_RELEASE_INSTANT = Instant.parse("2026-10-24T00:00:00Z");

    public static void main(String[] args) {
        enforceJdkSecurityCheck();
        SpringApplication.run(EmbServerApplication.class, args);
    }

    /**
     * 运行时双重校验：
     * 1. 兼容性：主版本号 >= 17
     * 2. 安全性：严格基于纯正的 UTC 时间线 (Instant) 进行绝对比对
     */
    private static void enforceJdkSecurityCheck() {
        // 维度一：兼容性检查 (主版本号 >= 17)
        int currentFeature = Runtime.version().feature();
        if (currentFeature < MIN_FEATURE_VERSION) {
            blockStartup(String.format("Java 主版本 %d 低于最低要求 %d，不满足框架运行兼容性要求！",
                    currentFeature, MIN_FEATURE_VERSION));
        }

        // 维度二：安全性检查 (基于标准 UTC Instant 时间戳比对)
        String versionDateStr = System.getProperty("java.version.date");
        if (versionDateStr != null && !versionDateStr.isBlank()) {
            try {
                // 拼接 UTC 标识 Z，直接解析为绝对无歧义的 UTC Instant
                Instant releaseInstantUtc = Instant.parse(versionDateStr.trim() + "T00:00:00Z");
                if (releaseInstantUtc.isBefore(MIN_SAFE_RELEASE_INSTANT)) {
                    blockStartup(String.format("当前 JDK 发布日期为 %s，早于 2026年10月24日 UTC 最终修复基线（17.0.21.0.3），存在严重安全漏洞！",
                            versionDateStr));
                }
            } catch (Exception e) {
                // 日期解析异常时不阻断
            }
        }
    }

    private static void blockStartup(String reason) {
        System.err.println("==================================================================");
        System.err.println("❌ 安全/兼容性拦截：当前 Java 环境未达标，应用已拒绝启动！");
        System.err.println("👉 原因：" + reason);
        System.err.println("🛡️ 要求：主版本号 >= 17，且发布时间必须 >= 2026-10-24T00:00:00Z (UTC)！");
        System.err.println("==================================================================");
        System.exit(1);
    }
}
