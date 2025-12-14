package com.zjgsu.rqq.enrollment_service.exception;

import com.zjgsu.rqq.enrollment_service.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        // 根据异常消息判断是业务错误还是服务不可用错误
        String message = ex.getMessage();
        System.out.println("=========================================");
        System.out.println("DEBUG: GlobalExceptionHandler被调用！");
        System.out.println("DEBUG: 异常类型: IllegalArgumentException");
        System.out.println("DEBUG: 异常消息: " + message);
        System.out.println("DEBUG: message.startsWith('用户服务暂时不可用') = " + (message != null && message.startsWith("用户服务暂时不可用")));
        System.out.println("DEBUG: message.contains('服务暂时不可用') = " + (message != null && message.contains("服务暂时不可用")));
        System.out.println("DEBUG: 堆栈跟踪:");
        ex.printStackTrace();
        System.out.println("=========================================");
        
        // 临时：总是返回503，用于调试
        System.out.println("DEBUG: 返回503状态码");
        if (message != null) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error(503, message));
        } else {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error(503, "服务暂时不可用"));
        }
    }
    
    /**
     * 从Feign异常消息中提取干净的错误信息
     * @param errorMessage 原始错误消息
     * @return 清理后的错误消息
     */
    private String extractCleanErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "未知错误";
        }
        
        // 如果包含Feign堆栈信息，尝试提取JSON响应
        if (errorMessage.contains("during [") && errorMessage.contains("]:")) {
            // 查找JSON响应部分
            int jsonStart = errorMessage.indexOf('{');
            int jsonEnd = errorMessage.lastIndexOf('}');
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                String jsonStr = errorMessage.substring(jsonStart, jsonEnd + 1);
                try {
                    // 简单提取"message"字段
                    int messageStart = jsonStr.indexOf("\"message\":\"");
                    if (messageStart != -1) {
                        messageStart += "\"message\":\"".length();
                        int messageEnd = jsonStr.indexOf("\"", messageStart);
                        if (messageEnd != -1) {
                            return jsonStr.substring(messageStart, messageEnd);
                        }
                    }
                } catch (Exception e) {
                    // 如果解析失败，返回原始消息
                }
            }
            
            // 如果无法解析JSON，尝试提取简单的错误信息
            if (errorMessage.contains("404")) {
                if (errorMessage.contains("学生")) {
                    return "学生不存在";
                } else if (errorMessage.contains("课程")) {
                    return "课程不存在";
                }
                return "资源不存在";
            }
        }
        
        // 返回原始消息
        return errorMessage;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(402) //888
                .body(ApiResponse.error(403, "参数验证失败", errors)); //888
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "服务器内部错误: " + ex.getMessage()));
    }
}
