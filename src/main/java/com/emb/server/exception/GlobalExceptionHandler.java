package com.emb.server.exception;

import com.emb.server.dto.ApiErrorResponse;
import com.openai.errors.BadRequestException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIServiceException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理器：对齐 OpenAI 标准错误结构，避免暴露 Spring 原始 500 页面
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 大模型 API 鉴权失败（401 Unauthorized）
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.error("大模型 API 鉴权失败: {}", ex.getMessage());
        String msg = "大模型 API Key 无效或鉴权失败: " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.invalidApiKey(msg));
    }

    /**
     * 大模型速率限制 / 额度耗尽（429 Too Many Requests）
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitException ex) {
        log.error("大模型请求频率超限或额度不足: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiErrorResponse.rateLimit(ex.getMessage()));
    }

    /**
     * 请求参数不合法（400 Bad Request）
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        log.error("大模型请求参数错误: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.badRequest(ex.getMessage()));
    }

    /**
     * Spring 参数校验异常（400 Bad Request）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("入参校验失败: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.badRequest("请求参数校验未通过: " + ex.getMessage()));
    }

    /**
     * 业务非法参数（400 Bad Request）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("非法业务参数: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.badRequest(ex.getMessage()));
    }

    /**
     * OpenAI 服务端返回错误
     */
    @ExceptionHandler(OpenAIServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleOpenAIService(OpenAIServiceException ex) {
        log.error("大模型服务端发生异常: code={}, message={}", ex.statusCode(), ex.getMessage());
        HttpStatus status = HttpStatus.resolve(ex.statusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.serverError("上游大模型服务异常: " + ex.getMessage()));
    }

    /**
     * OpenAI 通用异常
     */
    @ExceptionHandler(OpenAIException.class)
    public ResponseEntity<ApiErrorResponse> handleOpenAIException(OpenAIException ex) {
        log.error("大模型客户端异常: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.serverError("大模型调用失败: " + ex.getMessage()));
    }

    /**
     * 系统兜底全局异常（500 Internal Server Error）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("系统未捕获异常: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.serverError("服务器内部异常: " + ex.getMessage()));
    }
}
