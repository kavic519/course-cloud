package com.zjgsu.rqq.enrollment_service.repository;

import com.zjgsu.rqq.enrollment_service.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {

    List<Enrollment> findByCourseId(String courseId);
    List<Enrollment> findByStudentId(String studentId);
    List<Enrollment> findByStatus(Enrollment.EnrollmentStatus status);

    // 修改：使用 course_id 字段查询
    @Query("SELECT e FROM Enrollment e WHERE e.courseId = :courseId AND e.status = 'ACTIVE'")
    List<Enrollment> findActiveByCourseId(@Param("courseId") String courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.studentId = :studentId AND e.status = 'ACTIVE'")
    List<Enrollment> findActiveByStudentId(@Param("studentId") String studentId);

    // 修改：使用 course_id 和 student_id 字段检查重复选课
    @Query("SELECT COUNT(e) > 0 FROM Enrollment e WHERE e.courseId = :courseId AND e.studentId = :studentId AND e.status = :status")
    boolean existsByCourseIdAndStudentIdAndStatus(
            @Param("courseId") String courseId,
            @Param("studentId") String studentId,
            @Param("status") Enrollment.EnrollmentStatus status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId AND e.status = 'ACTIVE'")
    int countActiveByCourseId(@Param("courseId") String courseId);

    @Query("SELECT COUNT(e) > 0 FROM Enrollment e WHERE e.studentId = :studentId AND e.status = 'ACTIVE'")
    boolean hasActiveEnrollmentsByStudentId(@Param("studentId") String studentId);
}