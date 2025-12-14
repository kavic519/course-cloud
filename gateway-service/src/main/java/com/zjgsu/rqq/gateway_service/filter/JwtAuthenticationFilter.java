package com.zjgsu.rqq.gateway_service.filter;

import com.zjgsu.rqq.gateway_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // 白名单路径，不需要认证
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/health",
            "/actuator/health"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // 1. 检查是否为白名单路径
        if (isWhiteListPath(path)) {
            System.out.println("DEBUG: Path " + path + " is in whitelist, skipping authentication");
            return chain.filter(exchange);
        }
        
        System.out.println("DEBUG: Path " + path + " requires authentication");
        
        // 2. 获取Authorization头
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        
        // 3. 验证Authorization头格式
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "缺少有效的Authorization头，格式应为: Bearer <token>");
        }
        
        // 4. 提取Token
        String token = authHeader.substring(7);
        
        // 5. 验证Token有效性
        if (!jwtUtil.validateToken(token)) {
            return unauthorizedResponse(exchange, "Token无效或已过期");
        }
        
        // 6. 检查Token是否过期
        if (jwtUtil.isTokenExpired(token)) {
            return unauthorizedResponse(exchange, "Token已过期");
        }
        
        // 7. 解析Token获取用户信息
        Map<String, String> userInfo = jwtUtil.getUserInfoFromToken(token);
        
        // 8. 添加用户信息到请求头
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userInfo.get("userId"))
                .header("X-Username", userInfo.get("username"))
                .header("X-User-Role", userInfo.get("role"))
                .build();
        
        // 9. 转发请求
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    
    @Override
    public int getOrder() {
        return -100; // 高优先级，在其他过滤器之前执行
    }
    
    /**
     * 检查路径是否为白名单路径
     */
    private boolean isWhiteListPath(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }
    
    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        
        String responseBody = String.format(
                "{\"code\": 401, \"message\": \"%s\", \"data\": null, \"timestamp\": \"%s\"}",
                message,
                java.time.LocalDateTime.now()
        );
        
        byte[] bytes = responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(bytes)));
    }
}
