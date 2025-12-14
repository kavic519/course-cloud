package com.zjgsu.rqq.enrollment_service.controller;

import com.zjgsu.rqq.enrollment_service.client.UserClient;
import com.zjgsu.rqq.enrollment_service.common.ApiResponse;
import com.zjgsu.rqq.enrollment_service.model.Enrollment;
import com.zjgsu.rqq.enrollment_service.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    //private final UserClient userClient;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EnrollmentController.class);

    // @Value("${user-service.url:http://user-service:8081}")
    // private String userServiceUrl;
    
    // @Value("${catalog-service.url:http://catalog-service:8082}")
    // private String catalogServiceUrl;
    private static final String USER_SERVICE_NAME = "user-service";
    private static final String CATALOG_SERVICE_NAME = "catalog-service";

//    @Value("${server.port:8083}")
//    private String serverPort;

    String containerName = System.getenv("CONTAINER_NAME");
    String externalPort = System.getenv("EXTERNAL_PORT");

    @GetMapping("/port")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPort() {
        Map<String, Object> ContainerData = new HashMap<>();
        ContainerData.put("containerName", containerName);
        ContainerData.put("Port", externalPort);
        return ResponseEntity.ok(ApiResponse.success(ContainerData));
    }
    
    @GetMapping("/userport")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPort() {
        try {
            String url = "http://" + USER_SERVICE_NAME + "/api/students/port";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            Map<String, Object> responseData = new HashMap<>();
//            responseData.put("enrollmentService", Map.of(
//                "containerName", containerName,
//                "port", externalPort
//            ));
            responseData.put("userService", response.getBody());
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("enrollmentService", Map.of(
                "containerName", containerName,
                "port", externalPort
            ));
            errorData.put("userService", Map.of(
                "error", "无法连接到user-service: " + e.getMessage()
            ));
            return ResponseEntity.ok(ApiResponse.success(errorData));
        }
    }
    
    @GetMapping("/courseport")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCoursePort() {
        try {
            String url = "http://" + CATALOG_SERVICE_NAME + "/api/courses/port";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            Map<String, Object> responseData = new HashMap<>();
//            responseData.put("enrollmentService", Map.of(
//                "containerName", containerName,
//                "port", externalPort
//            ));
            responseData.put("catalogService", response.getBody());
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("enrollmentService", Map.of(
                "containerName", containerName,
                "port", externalPort
            ));
            errorData.put("catalogService", Map.of(
                "error", "无法连接到catalog-service: " + e.getMessage()
            ));
            return ResponseEntity.ok(ApiResponse.success(errorData));
        }
    }


    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllEnrollments() {
        List<Enrollment> enrollments = enrollmentService.getAllEnrollments();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("enrollments", enrollments);
        //responseData.put("port", hostPort);
        //responseData.put("containerPort", serverPort);
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<Enrollment>>> getEnrollmentsByCourseId(
            @PathVariable String courseId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByCourseId(courseId);
        return ResponseEntity.ok(ApiResponse.success(enrollments));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<Enrollment>>> getEnrollmentsByStudentId(
            @PathVariable String studentId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.success(enrollments));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Enrollment>> enrollStudent(
            @RequestBody Map<String, String> request) {
        try {
            String courseCode = request.get("courseCode");
            String studentId = request.get("studentId");

            if (courseCode == null || studentId == null) {
                return ResponseEntity.status(401)//888
                        .body(ApiResponse.error(406, "courseCode和studentId不能为空")); //888
            }

            Enrollment enrollment = enrollmentService.enrollStudent(courseCode, studentId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("选课成功", enrollment));
        } catch (IllegalArgumentException e) {
            // 检查是否是服务不可用错误
            String message = e.getMessage();
            if (message != null && 
                (message.startsWith("用户服务暂时不可用") ||
                 message.startsWith("课程目录服务暂时不可用") ||
                 message.contains("服务暂时不可用") ||
                 message.contains("用户服务暂时不可用") ||
                 message.contains("课程目录服务暂时不可用"))) {
                // 服务不可用错误，重新抛出，让GlobalExceptionHandler处理
                throw e;
            }
            
            // 其他业务错误
            int statusCode = e.getMessage().contains("不存在") ? 404 : 405;//888
            return ResponseEntity.status(statusCode)
                    .body(ApiResponse.error(statusCode, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error(503, "服务暂时不可用: " + e.getMessage()));
        }
    }

    @GetMapping("/count/{courseId}")
    public ResponseEntity<ApiResponse<Integer>> getCourseEnrollmentCount(
            @PathVariable String courseId) {
        int count = enrollmentService.getCourseEnrollmentCount(courseId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/isEnrolled")
    public ResponseEntity<ApiResponse<Boolean>> isStudentEnrolled(
            @RequestParam String courseId,
            @RequestParam String studentId) {
        boolean enrolled = enrollmentService.isStudentEnrolled(courseId, studentId);
        return ResponseEntity.ok(ApiResponse.success(enrolled));
    }
}
