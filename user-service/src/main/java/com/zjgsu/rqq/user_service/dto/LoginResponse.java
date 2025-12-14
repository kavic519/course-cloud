package com.zjgsu.rqq.user_service.dto;

public class LoginResponse {
    
    private String token;
    private String tokenType = "Bearer";
    private String userId;
    private String username;
    private String role;
    private Long expiresIn;
    
    // 构造方法
    public LoginResponse() {}
    
    public LoginResponse(String token, String userId, String username, String role, Long expiresIn) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.expiresIn = expiresIn;
    }
    
    // Getter和Setter方法
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
