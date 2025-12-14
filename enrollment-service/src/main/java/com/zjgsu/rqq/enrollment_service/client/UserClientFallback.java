package com.zjgsu.rqq.enrollment_service.client;

import com.zjgsu.rqq.enrollment_service.common.ApiResponse;
import com.zjgsu.rqq.enrollment_service.model.StudentDto;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    
    @Override
    public ApiResponse<StudentDto> getStudentByStudentId(String studentId) {
        // 返回一个表示服务不可用的响应
        return ApiResponse.error(506, "用户服务暂时不可用，无法获取学生信息（学号: " + studentId + "）");
    }
    
    @Override
    public ApiResponse<StudentDto> getStudentById(String id) {
        // 返回一个表示服务不可用的响应
        return ApiResponse.error(506, "用户服务暂时不可用，无法获取学生信息（ID: " + id + "）");
    }
}
