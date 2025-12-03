package com.zjgsu.rqq.enrollment_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
public class Enrollment {
    public enum EnrollmentStatus {
        ACTIVE, DROPPED, COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "course_id", nullable = false)
    private String courseId;    // 修改：courseId 作为普通字段，不是外键

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @PrePersist
    protected void onCreate() {
        if (enrolledAt == null) enrolledAt = LocalDateTime.now();
        if (status == null) status = EnrollmentStatus.ACTIVE;
    }

    // Getter和Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
}