package com.zjgsu.rqq.enrollment_service.client;

import com.zjgsu.rqq.enrollment_service.common.ApiResponse;
import com.zjgsu.rqq.enrollment_service.model.CourseDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CatalogClientFallback implements CatalogClient {
    
    @Override
    public ApiResponse<CourseDto> getCourseByCode(String code) {
        // 返回一个表示服务不可用的响应
        return ApiResponse.error(503, "课程目录服务暂时不可用，无法获取课程信息（课程代码: " + code + "）");
    }
    
    @Override
    public ApiResponse<CourseDto> getCourseById(String id) {
        // 返回一个表示服务不可用的响应
        return ApiResponse.error(503, "课程目录服务暂时不可用，无法获取课程信息（ID: " + id + "）");
    }
    
    @Override
    public ApiResponse<CourseDto> partialUpdateCourse(String id, Map<String, Object> updates) {
        // 返回一个表示服务不可用的响应
        return ApiResponse.error(503, "课程目录服务暂时不可用，无法更新课程信息（ID: " + id + "）");
    }
}
