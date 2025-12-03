package com.zjgsu.rqq.catalog_service.service;

import com.zjgsu.rqq.catalog_service.model.Course;
import com.zjgsu.rqq.catalog_service.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Optional<Course> getCourseById(String id) {
        return courseRepository.findById(id);
    }

    public Optional<Course> getCourseByCode(String code) {
        return courseRepository.findByCode(code);
    }

    public Course createCourse(Course course) {
        if (courseRepository.findByCode(course.getCode()).isPresent()) {
            throw new IllegalArgumentException("课程代码已存在: " + course.getCode());
        }
        return courseRepository.save(course);
    }

    public Course updateCourse(String id, Course course) {
        if (!courseRepository.existsById(id)) {
            throw new IllegalArgumentException("课程不存在: " + id);
        }

        Optional<Course> existingCourse = courseRepository.findByCode(course.getCode());
        if (existingCourse.isPresent() && !existingCourse.get().getId().equals(id)) {
            throw new IllegalArgumentException("课程代码已存在: " + course.getCode());
        }

        course.setId(id);
        return courseRepository.save(course);
    }

    public void deleteCourse(String id) {
        if (!courseRepository.existsById(id)) {
            throw new IllegalArgumentException("课程不存在: " + id);
        }
        courseRepository.deleteById(id);
    }

    @Transactional
    public void updateEnrolledCount(String courseId, Integer enrolled) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在: " + courseId));
        course.setEnrolled(enrolled);
        courseRepository.save(course);
    }

    public List<Course> getAvailableCourses() {
        return courseRepository.findAvailableCourses();
    }

    public List<Course> getCoursesByInstructor(String instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }

    public List<Course> searchCoursesByTitle(String keyword) {
        return courseRepository.findByTitleContaining(keyword);
    }
}