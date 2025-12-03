package com.zjgsu.rqq.catalog_service.repository;

import com.zjgsu.rqq.catalog_service.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 课程仓库
@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    Optional<Course> findByCode(String code);
    List<Course> findByInstructorId(String instructorId);

    @Query("SELECT c FROM Course c WHERE c.title LIKE %:keyword%")
    List<Course> findByTitleContaining(@Param("keyword") String keyword);

    @Query("SELECT c FROM Course c WHERE c.enrolled < c.capacity")
    List<Course> findAvailableCourses();

    boolean existsByCode(String code);

    @Query("SELECT COUNT(c) > 0 FROM Course c WHERE c.code = :code AND c.id != :id")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("id") String id);
}
