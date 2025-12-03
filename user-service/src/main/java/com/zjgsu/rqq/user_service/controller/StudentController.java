package com.zjgsu.rqq.user_service.controller;

import com.zjgsu.rqq.user_service.common.ApiResponse;
import com.zjgsu.rqq.user_service.model.Student;
import com.zjgsu.rqq.user_service.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${enrollment-service.url:http://localhost:8083}")
    private String enrollmentServiceUrl;

//    @Autowired
//    private EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getStudents(
            @RequestParam(required = false) String studentid,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String grade) {

        // 如果提供了学号参数，按学号查询
        if (studentid != null && !studentid.trim().isEmpty()) {
            Optional<Student> student = studentService.getStudentByStudentId(studentid);
            if (student.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(student.get()));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "学生不存在，学号: " + studentid));
            }
        }

        // 如果提供了邮箱参数，按邮箱查询
        if (email != null && !email.trim().isEmpty()) {
            Optional<Student> student = studentService.getStudentByEmail(email);
            if (student.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(student.get()));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "学生不存在，邮箱: " + email));
            }
        }

        // 如果提供了专业参数，按专业查询
        if (major != null && !major.trim().isEmpty()) {
            List<Student> students = studentService.getStudentsByMajor(major);
            if (students.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "没有找到专业《" + major + "》下的学生"));
            } else {
                return ResponseEntity.ok(ApiResponse.success(students));
            }
        }

        // 如果提供了年级参数，按年级查询
        if (grade != null && !grade.trim().isEmpty()) {
            List<Student> students = studentService.getStudentsByGrade(Integer.parseInt(grade));
            if (students.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "没有找到年级" + grade + "下的学生"));
            } else {
                return ResponseEntity.ok(ApiResponse.success(students));
            }
        }

        // 如果没有提供查询参数，返回所有学生
        List<Student> students = studentService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Student>> getStudentById(@PathVariable String id) {
        return studentService.getStudentById(id)
                .map(student -> ResponseEntity.ok(ApiResponse.success(student)))
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "学生不存在")));
    }

    // 专门为enrollment-service提供的按学号查询接口
    @GetMapping("/studentId/{studentId}")
    public ResponseEntity<ApiResponse<Student>> getStudentByStudentId(@PathVariable String studentId) {
        return studentService.getStudentByStudentId(studentId)
                .map(student -> ResponseEntity.ok(ApiResponse.success(student)))
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "学生不存在，学号: " + studentId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Student>> createStudent(@Valid @RequestBody Student student) {
        try {
            Student createdStudent = studentService.createStudent(student);
            return ResponseEntity.status(201)
                    .body(ApiResponse.created("学生创建成功", createdStudent));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Student>> updateStudent(
            @PathVariable String id,
            @Valid @RequestBody Student student) {
        try {
            Student updatedStudent = studentService.updateStudent(id, student);
            return ResponseEntity.ok(ApiResponse.success("学生信息更新成功", updatedStudent));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, e.getMessage()));
            }
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable String id) {
        try {
            // 1. 首先获取学生信息
            Student student = studentService.getStudentByStudentId(id)
                    .orElseThrow(() -> new IllegalArgumentException("学生不存在: " + id));

            // 2. 调用选课服务检查学生是否有选课记录
            String enrollmentCheckUrl = enrollmentServiceUrl + "/api/enrollments/student/" + student.getStudentId();
            try {
                ResponseEntity<Object> response = restTemplate.getForEntity(enrollmentCheckUrl, Object.class);

                // 如果学生有选课记录，不允许删除
                if (response.getStatusCode().is2xxSuccessful()) {
                    Object responseBody = response.getBody();
                    if (responseBody != null) {
                        // 这里需要根据实际的响应结构来判断是否有选课记录
                        // 假设响应包含数据且不为空表示有选课记录
                        return ResponseEntity.status(400)
                                .body(ApiResponse.error(400, "无法删除：该学生存在选课记录"));
                    }
                }
            } catch (HttpClientErrorException.NotFound e) {
                // 如果没有找到选课记录，继续执行删除
                // 这是正常情况，不做处理
            } catch (Exception e) {
                // 选课服务不可用，记录日志但允许删除（或者根据业务需求决定）
                System.err.println("选课服务调用失败，但仍然删除学生: " + e.getMessage());
                // 这里可以选择继续删除或者返回错误，根据业务需求决定
            }

            // 3. 删除学生
            studentService.deleteStudent(id);
            return ResponseEntity.ok(ApiResponse.noContent("学生删除成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }
}
