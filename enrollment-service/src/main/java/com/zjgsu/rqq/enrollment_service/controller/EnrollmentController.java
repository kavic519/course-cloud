package com.zjgsu.rqq.enrollment_service.controller;

import com.zjgsu.rqq.enrollment_service.common.ApiResponse;
import com.zjgsu.rqq.enrollment_service.model.Enrollment;
import com.zjgsu.rqq.enrollment_service.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Enrollment>>> getAllEnrollments() {
        List<Enrollment> enrollments = enrollmentService.getAllEnrollments();
        return ResponseEntity.ok(ApiResponse.success(enrollments));
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
                return ResponseEntity.status(400)
                        .body(ApiResponse.error(400, "courseCode和studentId不能为空"));
            }

            Enrollment enrollment = enrollmentService.enrollStudent(courseCode, studentId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("选课成功", enrollment));
        } catch (IllegalArgumentException e) {
            int statusCode = e.getMessage().contains("不存在") ? 404 : 400;
            return ResponseEntity.status(statusCode)
                    .body(ApiResponse.error(statusCode, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error(503, "服务暂时不可用: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> unenrollStudent(@PathVariable String id) {
        try {
            enrollmentService.unenrollStudent(id);
            return ResponseEntity.ok(ApiResponse.noContent("退选成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, e.getMessage()));
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