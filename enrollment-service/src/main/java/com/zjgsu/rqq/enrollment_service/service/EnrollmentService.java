package com.zjgsu.rqq.enrollment_service.service;

import com.zjgsu.rqq.enrollment_service.model.Enrollment;
import com.zjgsu.rqq.enrollment_service.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    @Autowired
    private EnrollmentRepository enrollmentRepository;



    @Autowired
    private RestTemplate restTemplate;

    // 使用服务名而不是硬编码URL
    private static final String USER_SERVICE_NAME = "user-service";
    private static final String CATALOG_SERVICE_NAME = "catalog-service";

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    public List<Enrollment> getEnrollmentsByCourseId(String courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    public List<Enrollment> getEnrollmentsByStudentId(String studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Transactional
    public Enrollment enrollStudent(String courseCode, String studentId) {
        log.info("开始选课: courseCode={}, studentId={}", courseCode, studentId);

        // 1. 调用用户服务验证学生存在
        String studentUrl = "http://" + USER_SERVICE_NAME + "/api/students?studentid=" + studentId;
        log.info("调用用户服务: {}", studentUrl);

        try {
            ResponseEntity<Map> studentResponse = restTemplate.getForEntity(studentUrl, Map.class);
            log.info("用户服务响应: {}", studentResponse.getBody());
            
            // 检查响应是否成功
            Map<String, Object> responseBody = studentResponse.getBody();
            if (responseBody != null) {
                Integer code = (Integer) responseBody.get("code");
                if (code != null && code == 200) {
                    // 学生存在，继续处理
                    log.info("学生验证成功: {}", studentId);
                } else {
                    // 学生不存在
                    log.error("学生不存在: {}", studentId);
                    throw new IllegalArgumentException("学生不存在: " + studentId);
                }
            } else {
                log.error("用户服务响应为空");
                throw new RuntimeException("用户服务响应为空");
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.error("学生不存在: {}", studentId);
            throw new IllegalArgumentException("学生不存在: " + studentId);
        } catch (Exception e) {
            log.error("用户服务调用失败: {}", e.getMessage());
            throw new RuntimeException("用户服务调用失败: " + e.getMessage());
        }

        // 2. 调用课程目录服务验证课程
        String courseUrl = "http://" + CATALOG_SERVICE_NAME + "/api/courses/code/" + courseCode;
        log.info("调用课程服务: {}", courseUrl);

        Map<String, Object> courseResponse;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(courseUrl, Map.class);
            courseResponse = response.getBody();
            log.info("课程服务响应: {}", courseResponse);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("课程不存在: {}", courseCode);
            throw new IllegalArgumentException("课程不存在: " + courseCode);
        } catch (Exception e) {
            log.error("课程服务调用失败: {}", e.getMessage());
            throw new RuntimeException("课程服务调用失败: " + e.getMessage());
        }

        // 3. 提取课程信息
        Map<String, Object> courseData = (Map<String, Object>) courseResponse.get("data");
        String courseId = (String) courseData.get("id");
        Integer capacity = (Integer) courseData.get("capacity");
        Integer enrolled = (Integer) courseData.get("enrolled");

        log.info("课程信息: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);

        // 4. 检查课程容量
        if (enrolled >= capacity) {
            log.warn("课程容量已满: courseId={}", courseId);
            throw new IllegalArgumentException("课程容量已满");
        }

        // 5. 检查重复选课
        if (enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(
                courseId, studentId, Enrollment.EnrollmentStatus.ACTIVE)) {
            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
            throw new IllegalArgumentException("学生已选该课程");
        }

        // 6. 创建选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("选课记录创建成功: enrollmentId={}", savedEnrollment.getId());

        // 7. 更新课程的已选人数
        updateCourseEnrolledCount(courseId, enrolled + 1);

        return savedEnrollment;
    }

    @Transactional
    public void unenrollStudent(String enrollmentId) {
        log.info("开始退课: enrollmentId={}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("选课记录不存在: " + enrollmentId));

        if (enrollment.getStatus() != Enrollment.EnrollmentStatus.ACTIVE) {
            log.warn("选课记录不是活跃状态: enrollmentId={}, status={}", enrollmentId, enrollment.getStatus());
            throw new IllegalArgumentException("选课记录不是活跃状态");
        }

        String courseId = enrollment.getCourseId();
        enrollment.setStatus(Enrollment.EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);
        log.info("选课记录状态更新为DROPPED: enrollmentId={}", enrollmentId);

        // 获取当前选课人数并更新
        int currentCount = enrollmentRepository.countActiveByCourseId(courseId);
        updateCourseEnrolledCount(courseId, currentCount);
        log.info("退课完成: enrollmentId={}, 当前课程选课人数={}", enrollmentId, currentCount);
    }

    private void updateCourseEnrolledCount(String courseId, int newCount) {
        String url = "http://" + CATALOG_SERVICE_NAME + "/api/courses/" + courseId;
        Map<String, Object> updateData = Map.of("enrolled", newCount);
        try {
            restTemplate.patchForObject(url, updateData, Void.class);
            log.info("课程选课人数更新成功: courseId={}, newCount={}", courseId, newCount);
        } catch (Exception e) {
            log.error("更新课程选课人数失败: courseId={}, error={}", courseId, e.getMessage());
        }
    }

    public boolean hasStudentEnrollments(String studentId) {
        return enrollmentRepository.hasActiveEnrollmentsByStudentId(studentId);
    }

    public int getCourseEnrollmentCount(String courseId) {
        return enrollmentRepository.countActiveByCourseId(courseId);
    }

    public boolean isStudentEnrolled(String courseId, String studentId) {
        return enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(
                courseId, studentId, Enrollment.EnrollmentStatus.ACTIVE);
    }
}
