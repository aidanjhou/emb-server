package com.emb.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

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
        loadDotEnvIfPresent();

        ConfigurableApplicationContext context = SpringApplication.run(EmbServerApplication.class, args);
        printStartupBanner(context.getEnvironment());
    }

    /**
     * 自动从项目根目录检测并加载 .env 文件（若存在）
     * 解决本地开发无需频繁手动 export 环境变量的问题
     */
    private static void loadDotEnvIfPresent() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            envPath = Path.of(System.getProperty("user.dir", "."), ".env");
        }
        if (Files.exists(envPath)) {
            try {
                List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
                int loadedCount = 0;
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String val = line.substring(eqIdx + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        // 仅在当前系统属性与环境变量皆未定义时加载，不覆盖显式传入的系统环境变量
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                            loadedCount++;
                        }
                    }
                }
                if (loadedCount > 0) {
                    System.out.println("🌱 [dotenv] 已自动从 .env 文件注入 " + loadedCount + " 项配置");
                }
            } catch (Exception e) {
                System.err.println("⚠️ [dotenv] 读取 .env 失败: " + e.getMessage());
            }
        }
    }

    private static void printStartupBanner(Environment env) {
        String baseUrl = env.getProperty("spring.ai.openai.base-url", "https://api.deepseek.com");
        String model = env.getProperty("spring.ai.openai.chat.options.model", "deepseek-chat");
        String apiKey = env.getProperty("spring.ai.openai.api-key", "demo-key");
        String port = env.getProperty("server.port", "8080");

        String maskedKey = maskKey(apiKey);

        System.out.println();
        System.out.println("==================================================================");
        System.out.println("🚀 emb-server 已成功启动 (端口: " + port + ")");
        System.out.println("📡 大模型接口: " + baseUrl);
        System.out.println("🤖 默认模型: " + model);
        System.out.println("🔑 API Key: " + maskedKey);
        System.out.println("==================================================================");
        System.out.println();
    }

    private static String maskKey(String key) {
        if (key == null || key.isBlank()) return "(未设置)";
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    /**
     * 运行时双重校验：
     * 1. 兼容性：主版本号 >= 17
     * 2. 安全性：严格基于纯正的 UTC 时间线 (Instant) 进行绝对比对
     */
    private static void enforceJdkSecurityCheck() {
        int currentFeature = Runtime.version().feature();
        if (currentFeature < MIN_FEATURE_VERSION) {
            blockStartup(String.format("Java 主版本 %d 低于最低要求 %d，不满足框架运行兼容性要求！",
                    currentFeature, MIN_FEATURE_VERSION));
        }

        String versionDateStr = System.getProperty("java.version.date");
        if (versionDateStr != null && !versionDateStr.isBlank()) {
            try {
                Instant releaseInstantUtc = Instant.parse(versionDateStr.trim() + "T00:00:00Z");
                if (releaseInstantUtc.isBefore(MIN_SAFE_RELEASE_INSTANT)) {
                    blockStartup(String.format("当前 JDK 发布日期为 %s，早于 2026年10月24日 UTC 最终修复基线（17.0.21.0.3），存在严重安全漏洞！",
                            versionDateStr));
                }
            } catch (Exception ignored) {
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
