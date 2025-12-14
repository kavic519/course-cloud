package com.zjgsu.rqq.enrollment_service.client;

import com.zjgsu.rqq.enrollment_service.common.ApiResponse;
import com.zjgsu.rqq.enrollment_service.model.CourseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "catalog-service",
    fallback = CatalogClientFallback.class
)
public interface CatalogClient {
    
    @GetMapping("/api/courses/code/{code}")
    ApiResponse<CourseDto> getCourseByCode(@PathVariable("code") String code);
    
    @GetMapping("/api/courses/{id}")
    ApiResponse<CourseDto> getCourseById(@PathVariable("id") String id);
    
    @PatchMapping("/api/courses/{id}")
    ApiResponse<CourseDto> partialUpdateCourse(
        @PathVariable("id") String id,
        @RequestBody Map<String, Object> updates);
}
