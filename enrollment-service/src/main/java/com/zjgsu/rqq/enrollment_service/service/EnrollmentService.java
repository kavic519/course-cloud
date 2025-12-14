package com.zjgsu.rqq.enrollment_service.service;

import com.zjgsu.rqq.enrollment_service.client.UserClient;
import com.zjgsu.rqq.enrollment_service.client.CatalogClient;
import com.zjgsu.rqq.enrollment_service.common.ApiResponse;
import com.zjgsu.rqq.enrollment_service.model.Enrollment;
import com.zjgsu.rqq.enrollment_service.model.StudentDto;
import com.zjgsu.rqq.enrollment_service.model.CourseDto;
import com.zjgsu.rqq.enrollment_service.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private UserClient userClient;

    @Autowired
    private CatalogClient catalogClient;

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

        // 1. 使用Feign Client调用用户服务验证学生存在
        log.info("调用用户服务获取学生信息: studentId={}", studentId);
        ApiResponse<StudentDto> studentResponse;
        try {
            studentResponse = userClient.getStudentByStudentId(studentId);
            log.info("用户服务调用成功，响应: code={}, message={}", 
                studentResponse != null ? studentResponse.getCode() : "null",
                studentResponse != null ? studentResponse.getMessage() : "null");
        } catch (Exception e) {
            log.error("调用用户服务异常: studentId={}, 异常类型: {}, 消息: {}", 
                studentId, e.getClass().getName(), e.getMessage());
            // 首先检查是否是连接错误（服务不可用）
            if (isConnectionError(e)) {
                log.error("用户服务连接失败（服务不可用）: studentId={}, error={}", studentId, e.getMessage());
                throw new IllegalArgumentException("用户服务暂时不可用，请稍后重试");
            }
            
            // 检查是否是404错误（学生不存在）
            String errorMessage = extractErrorMessageFromException(e, "学生");
            if (errorMessage != null && errorMessage.contains("不存在")) {
                log.error("学生不存在: {}, 错误: {}", studentId, errorMessage);
                throw new IllegalArgumentException("学生不存在: " + studentId);
            }
            
            // 其他所有异常都视为服务不可用
            log.error("调用用户服务失败: studentId={}, error={}", studentId, e.getMessage());
            throw new IllegalArgumentException("用户服务暂时不可用，请稍后重试");
        }
        
        // 检查Fallback返回的503响应
        if (studentResponse != null && studentResponse.getCode() == 503) {
            log.error("用户服务不可用（熔断器触发）: studentId={}, 响应: code={}, message={}", 
                studentId, studentResponse.getCode(), studentResponse.getMessage());
            throw new IllegalArgumentException(studentResponse.getMessage());
        }
        
        if (studentResponse == null || studentResponse.getCode() != 200) {
            log.error("学生不存在: {}, 响应: code={}, message={}", 
                studentId, 
                studentResponse != null ? studentResponse.getCode() : "null",
                studentResponse != null ? studentResponse.getMessage() : "null");
            
            // 检查是否是服务不可用错误
            if (studentResponse != null && studentResponse.getMessage() != null &&
                studentResponse.getMessage().contains("用户服务暂时不可用")) {
                throw new IllegalArgumentException(studentResponse.getMessage());
            }
            
            throw new IllegalArgumentException("学生不存在: " + studentId);
        }
        log.info("学生验证成功: {}", studentId);

        // 2. 使用Feign Client调用课程目录服务验证课程
        log.info("调用课程服务获取课程信息: courseCode={}", courseCode);
        ApiResponse<CourseDto> courseResponse;
        try {
            courseResponse = catalogClient.getCourseByCode(courseCode);
        } catch (Exception e) {
            // 检查是否是连接错误（服务不可用）
            if (isConnectionError(e)) {
                log.error("课程服务连接失败（服务不可用）: courseCode={}, error={}", courseCode, e.getMessage());
                throw new IllegalArgumentException("课程目录服务暂时不可用，请稍后重试");
            }
            
            // 从异常信息中提取更具体的错误信息
            String errorMessage = extractErrorMessageFromException(e, "课程");
            if (errorMessage != null && errorMessage.contains("不存在")) {
                log.error("课程不存在: {}, 错误: {}", courseCode, errorMessage);
                throw new IllegalArgumentException("课程不存在: " + courseCode);
            }
            log.error("调用课程服务失败: courseCode={}, error={}", courseCode, e.getMessage());
            throw new IllegalArgumentException("课程目录服务暂时不可用，请稍后重试");
        }
        
        if (courseResponse == null) {
            log.error("课程服务返回空响应: courseCode={}", courseCode);
            throw new IllegalArgumentException("课程目录服务暂时不可用，请稍后重试");
        }
        
        if (courseResponse.getCode() == 404) {
            log.error("课程不存在: {}, 响应: {}", courseCode, courseResponse);
            throw new IllegalArgumentException("课程不存在: " + courseCode);
        } else if (courseResponse.getCode() != 200) {
            log.error("课程服务错误: courseCode={}, 响应: {}", courseCode, courseResponse);
//            throw new IllegalArgumentException("课程目录服务错误: " + courseResponse.getMessage());
            throw new IllegalArgumentException(courseResponse.getMessage());
        }
        
        CourseDto courseDto = courseResponse.getData();
        if (courseDto == null) {
            log.error("课程数据为空: courseCode={}", courseCode);
            throw new IllegalArgumentException("课程数据获取失败");
        }
        
        String courseId = courseDto.getId();
        Integer capacity = courseDto.getCapacity();
        Integer enrolled = courseDto.getEnrolled();

        log.info("课程信息: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);

        // 3. 检查课程容量
        if (enrolled >= capacity) {
            log.warn("课程容量已满: courseId={}", courseId);
            throw new IllegalArgumentException("课程容量已满");
        }

        // 4. 检查重复选课
        if (enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(
                courseId, studentId, Enrollment.EnrollmentStatus.ACTIVE)) {
            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
            throw new IllegalArgumentException("学生已选该课程");
        }

        // 5. 创建选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("选课记录创建成功: enrollmentId={}", savedEnrollment.getId());

        // 6. 更新课程的已选人数
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
        Map<String, Object> updateData = Map.of("enrolled", newCount);
        try {
            // 使用Feign Client更新课程选课人数
            ApiResponse<CourseDto> response = catalogClient.partialUpdateCourse(courseId, updateData);
            if (response.getCode() == 200) {
                log.info("课程选课人数更新成功: courseId={}, newCount={}", courseId, newCount);
            } else {
                log.error("课程选课人数更新失败: courseId={}, response={}", courseId, response);
            }
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

    /**
     * 检查异常是否是连接错误（服务不可用）
     * @param e 异常
     * @return 如果是连接错误返回true，否则返回false
     */
    private boolean isConnectionError(Exception e) {
        if (e == null) {
            return false;
        }
        
        String errorMessage = e.getMessage();
        if (errorMessage == null) {
            return false;
        }
        
        // 检查常见的连接错误关键词
        String lowerErrorMessage = errorMessage.toLowerCase();
        return lowerErrorMessage.contains("connection") ||
               lowerErrorMessage.contains("connect") ||
               lowerErrorMessage.contains("timeout") ||
               lowerErrorMessage.contains("refused") ||
               lowerErrorMessage.contains("unavailable") ||
               lowerErrorMessage.contains("available") ||  // 添加available
               lowerErrorMessage.contains("no servers") || // 添加no servers
               lowerErrorMessage.contains("failed") ||
               lowerErrorMessage.contains("error") ||
               lowerErrorMessage.contains("ioexception") ||
               lowerErrorMessage.contains("socket") ||
               lowerErrorMessage.contains("network") ||
               lowerErrorMessage.contains("loadbalancer") || // 添加loadbalancer
               lowerErrorMessage.contains("load balancer"); // 添加load balancer
    }

    /**
     * 从异常中提取错误信息
     * @param e 异常
     * @param resourceType 资源类型（"学生"或"课程"）
     * @return 提取的错误信息，如果无法提取则返回null
     */
    private String extractErrorMessageFromException(Exception e, String resourceType) {
        if (e == null) {
            return null;
        }
        
        String errorMessage = e.getMessage();
        if (errorMessage == null) {
            return null;
        }
        
        // 检查是否包含404错误
        if (errorMessage.contains("404")) {
            // 尝试从错误信息中提取JSON响应
            // 错误信息格式示例: [404] during [GET] to [http://catalog-service/api/courses/code/CSS301] [CatalogClient#getCourseByCode(String)]: [{"code":404,"message":"课程不存在: CSS301","data":null}]
            int jsonStart = errorMessage.indexOf('{');
            int jsonEnd = errorMessage.lastIndexOf('}');
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                String jsonStr = errorMessage.substring(jsonStart, jsonEnd + 1);
                try {
                    // 简单提取"message"字段
                    int messageStart = jsonStr.indexOf("\"message\":\"");
                    if (messageStart != -1) {
                        messageStart += "\"message\":\"".length();
                        int messageEnd = jsonStr.indexOf("\"", messageStart);
                        if (messageEnd != -1) {
                            return jsonStr.substring(messageStart, messageEnd);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("无法解析错误信息中的JSON: {}", jsonStr);
                }
            }
            
            // 如果无法解析JSON，返回默认错误信息
            return resourceType + "不存在";
        }
        
        return null;
    }
}
