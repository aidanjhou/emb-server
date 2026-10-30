package com.emb.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * SSE Web 消息转换器定制配置
 *
 * <p>Spring 官方 ServerSentEvent / SseEmitter 遵循 W3C 规范最小实现，
 * 默认在写每一帧时输出 "data:"（无空格）。
 * 而所有的主流大模型厂商（OpenAI、DeepSeek、Moonshot 等）在协议中约定俗成以 "data: "（带空格）输出。
 *
 * 本配置通过定制 Spring Web 的 StringHttpMessageConverter，
 * 在流式事件（text/event-stream）输出 "data:" 前缀时自动对齐工业标准带上空格，
 * 使业务层无需任何 hack，可以直接 100% 使用 Spring 官方原生的 ServerSentEvent 与自动 JSON 序列化能力。</p>
 */
@Configuration
public class SseWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.withStringConverter(new OpenAiSseStringHttpMessageConverter());
    }

    public static class OpenAiSseStringHttpMessageConverter extends StringHttpMessageConverter {
        public OpenAiSseStringHttpMessageConverter() {
            super(StandardCharsets.UTF_8);
        }

        @Override
        protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
            if ("data:".equals(str)) {
                MediaType contentType = outputMessage.getHeaders().getContentType();
                if (contentType != null && contentType.includes(MediaType.TEXT_EVENT_STREAM)) {
                    str = "data: ";
                }
            }
            super.writeInternal(str, outputMessage);
        }
    }
}
