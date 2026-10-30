package com.emb.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 官方标准 OpenAI 错误协议响应模型
 *
 * 格式遵循：
 * {
 *   "error": {
 *     "message": "错误原因说明",
 *     "type": "invalid_request_error | authentication_error | api_error",
 *     "param": null,
 *     "code": "invalid_api_key | null"
 *   }
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(ErrorDetail error) {

    public record ErrorDetail(
            String message,
            String type,
            String param,
            String code
    ) {}

    public static ApiErrorResponse of(String message, String type, String code) {
        return new ApiErrorResponse(new ErrorDetail(message, type, null, code));
    }

    public static ApiErrorResponse invalidApiKey(String message) {
        return of(message, "authentication_error", "invalid_api_key");
    }

    public static ApiErrorResponse badRequest(String message) {
        return of(message, "invalid_request_error", "bad_request");
    }

    public static ApiErrorResponse rateLimit(String message) {
        return of(message, "rate_limit_error", "rate_limit_exceeded");
    }

    public static ApiErrorResponse serverError(String message) {
        return of(message, "api_error", "internal_server_error");
    }
}
