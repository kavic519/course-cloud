package com.zjgsu.rqq.catalog_service.controller;

import com.zjgsu.rqq.catalog_service.common.ApiResponse;
import com.zjgsu.rqq.catalog_service.model.Course;
import com.zjgsu.rqq.catalog_service.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    // 获取所有课程
    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    // 搜索课程
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Course>>> searchCourses(
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String keyword) {

        if (instructorId != null && !instructorId.trim().isEmpty()) {
            List<Course> courses = courseService.getCoursesByInstructor(instructorId);
            return ResponseEntity.ok(ApiResponse.success(courses));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Course> courses = courseService.searchCoursesByTitle(keyword);
            return ResponseEntity.ok(ApiResponse.success(courses));
        }

        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, "必须提供instructorId或keyword参数"));
    }

    // 根据课程代码查询
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<Course>> getCourseByCode(@PathVariable String code) {
        return courseService.getCourseByCode(code)
                .map(course -> ResponseEntity.ok(ApiResponse.success(course)))
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "课程不存在: " + code)));
    }

    // 获取可用课程
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Course>>> getAvailableCourses() {
        List<Course> courses = courseService.getAvailableCourses();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    // 根据ID获取课程
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable String id) {
        return courseService.getCourseById(id)
                .map(course -> ResponseEntity.ok(ApiResponse.success(course)))
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "课程不存在")));
    }

    // 创建课程
    @PostMapping
    public ResponseEntity<ApiResponse<Course>> createCourse(@Valid @RequestBody Course course) {
        try {
            Course createdCourse = courseService.createCourse(course);
            return ResponseEntity.status(201)
                    .body(ApiResponse.created("课程创建成功", createdCourse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    // 更新课程
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable String id,
            @Valid @RequestBody Course course) {
        try {
            Course updatedCourse = courseService.updateCourse(id, course);
            return ResponseEntity.ok(ApiResponse.success("课程更新成功", updatedCourse));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, e.getMessage()));
            }
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    // 部分更新课程（用于更新选课人数等字段）
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> partialUpdateCourse(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        try {
            Course course = courseService.getCourseById(id)
                    .orElseThrow(() -> new IllegalArgumentException("课程不存在: " + id));

            // 只更新提供的字段
            if (updates.containsKey("enrolled")) {
                course.setEnrolled((Integer) updates.get("enrolled"));
            }
            if (updates.containsKey("title")) {
                course.setTitle((String) updates.get("title"));
            }
            if (updates.containsKey("capacity")) {
                course.setCapacity((Integer) updates.get("capacity"));
            }

            Course updatedCourse = courseService.updateCourse(id, course);
            return ResponseEntity.ok(ApiResponse.success("课程更新成功", updatedCourse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    // 删除课程
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable String id) {
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.ok(ApiResponse.noContent("课程删除成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }
}