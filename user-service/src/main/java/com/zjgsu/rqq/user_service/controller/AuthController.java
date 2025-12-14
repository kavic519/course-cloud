package com.zjgsu.rqq.user_service.controller;

import com.zjgsu.rqq.user_service.common.ApiResponse;
import com.zjgsu.rqq.user_service.dto.LoginRequest;
import com.zjgsu.rqq.user_service.dto.LoginResponse;
import com.zjgsu.rqq.user_service.model.Student;
import com.zjgsu.rqq.user_service.repository.StudentRepository;
import com.zjgsu.rqq.user_service.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 1. 根据学号查找用户
        Optional<Student> studentOpt = studentRepository.findByStudentId(loginRequest.getStudentId());
        
        if (studentOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "学号或密码错误"));
        }
        
        Student student = studentOpt.get();
        
        // 2. 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), student.getPassword())) {
            return ResponseEntity.ok(ApiResponse.error(401, "学号或密码错误"));
        }
        
        // 3. 生成JWT Token
        String token = jwtUtil.generateToken(
                student.getId(),
                student.getName(),
                "STUDENT" // 默认角色为STUDENT
        );
        
        // 4. 构建响应
        LoginResponse loginResponse = new LoginResponse(
                token,
                student.getId(),
                student.getName(),
                "STUDENT",
                jwtExpiration
        );
        
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Student>> register(@Valid @RequestBody Student student) {
        // 1. 检查学号是否已存在
        if (studentRepository.findByStudentId(student.getStudentId()).isPresent()) {
            return ResponseEntity.ok(ApiResponse.error(400, "学号已存在"));
        }
        
        // 2. 检查邮箱是否已存在
        if (studentRepository.findByEmail(student.getEmail()).isPresent()) {
            return ResponseEntity.ok(ApiResponse.error(400, "邮箱已存在"));
        }
        
        // 3. 加密密码
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        
        // 4. 保存用户
        Student savedStudent = studentRepository.save(student);
        
        // 5. 移除密码返回
        savedStudent.setPassword(null);
        
        return ResponseEntity.ok(ApiResponse.success(savedStudent));
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("User Service is healthy"));
    }
}
