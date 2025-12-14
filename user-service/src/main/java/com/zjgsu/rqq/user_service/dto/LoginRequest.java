package com.zjgsu.rqq.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    
    @NotBlank(message = "学号不能为空")
    private String studentId;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    // 构造方法
    public LoginRequest() {}
    
    public LoginRequest(String studentId, String password) {
        this.studentId = studentId;
        this.password = password;
    }
    
    // Getter和Setter方法
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}
