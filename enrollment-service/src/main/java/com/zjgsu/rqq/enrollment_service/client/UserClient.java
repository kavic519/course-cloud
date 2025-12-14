package com.zjgsu.rqq.enrollment_service.client;

import com.zjgsu.rqq.enrollment_service.common.ApiResponse;
import com.zjgsu.rqq.enrollment_service.model.StudentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "user-service",
    fallback = UserClientFallback.class
)
public interface UserClient {
    
    @GetMapping("/api/students/studentId/{studentId}")
    ApiResponse<StudentDto> getStudentByStudentId(@PathVariable("studentId") String studentId);
    
    @GetMapping("/api/students/{id}")
    ApiResponse<StudentDto> getStudentById(@PathVariable("id") String id);
}
