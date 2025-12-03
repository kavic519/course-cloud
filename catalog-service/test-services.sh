#!/bin/bash

# 校园选课系统微服务测试脚本（新版接口）
# 测试 catalog-service (8081), enrollment-service (8082), user-service (8083)

echo "=============================================="
echo "   校园选课系统微服务测试脚本（新版接口）"
echo "=============================================="
echo "开始时间: $(date)"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 基础URL
CATALOG_URL="http://localhost:8082"
ENROLLMENT_URL="http://localhost:8083"
USER_URL="http://localhost:8081"

# 函数：打印带颜色的消息
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 函数：检查服务是否可用
check_service_availability() {
    log_info "检查服务可用性..."

    # 检查 catalog-service
    if curl -s --head --request GET "$CATALOG_URL/api/courses" | grep "200" > /dev/null; then
        log_success "catalog-service (8082) 服务正常"
    else
        log_error "catalog-service (8082) 服务不可用"
        exit 1
    fi

    # 检查 enrollment-service
    if curl -s --head --request GET "$ENROLLMENT_URL/api/enrollments" | grep "200" > /dev/null; then
        log_success "enrollment-service (8083) 服务正常"
    else
        log_error "enrollment-service (8083) 服务不可用"
        exit 1
    fi

    # 检查 user-service
    if curl -s --head --request GET "$USER_URL/api/students" | grep "200" > /dev/null; then
        log_success "user-service (8081) 服务正常"
    else
        log_error "user-service (8081) 服务不可用"
        exit 1
    fi
    echo ""
}

# 函数：测试课程目录服务
test_catalog_service() {
    log_info "=== 测试课程目录服务 ==="

    # 1. 创建课程 - 计算机科学导论
    log_info "1. 创建课程：计算机科学导论"
    COURSE_CS101=$(curl -s -X POST "$CATALOG_URL/api/courses" \
        -H "Content-Type: application/json" \
        -d '{
            "code": "CS101",
            "title": "Introduction to Computer Science",
            "instructor": {
                "id": "T001",
                "name": "Professor Zhang",
                "email": "zhang@example.edu.cn"
            },
            "schedule": {
                "dayOfWeek": "MONDAY",
                "startTime": "08:00",
                "endTime": "10:00",
                "location": "Building A101"
            },
            "capacity": 30,
            "enrolled": 0
        }')
    echo "响应: $COURSE_CS101"

    # 2. 创建课程 - 数据结构与算法
    log_info "2. 创建课程：数据结构与算法"
    COURSE_CS201=$(curl -s -X POST "$CATALOG_URL/api/courses" \
        -H "Content-Type: application/json" \
        -d '{
            "code": "CS201",
            "title": "Data Structures and Algorithms",
            "instructor": {
                "id": "T002",
                "name": "Professor Li",
                "email": "li@example.edu.cn"
            },
            "schedule": {
                "dayOfWeek": "WEDNESDAY",
                "startTime": "10:00",
                "endTime": "12:00",
                "location": "Building B201"
            },
            "capacity": 25,
            "enrolled": 0
        }')
    echo "响应: $COURSE_CS201"

    # 3. 创建课程 - 数据库系统原理
    log_info "3. 创建课程：数据库系统原理"
    COURSE_CS301=$(curl -s -X POST "$CATALOG_URL/api/courses" \
        -H "Content-Type: application/json" \
        -d '{
            "code": "CS301",
            "title": "Database System Principles",
            "instructor": {
                "id": "T003",
                "name": "Professor Wang",
                "email": "wang@example.edu.cn"
            },
            "schedule": {
                "dayOfWeek": "FRIDAY",
                "startTime": "14:00",
                "endTime": "16:00",
                "location": "Lab Building C301"
            },
            "capacity": 20,
            "enrolled": 0
        }')
    echo "响应: $COURSE_CS301"

    # 4. 获取所有课程
    log_info "4. 获取所有课程"
    ALL_COURSES=$(curl -s -X GET "$CATALOG_URL/api/courses")
    echo "所有课程: $ALL_COURSES"

    # 提取课程ID用于后续测试
    CS101_ID=$(echo $COURSE_CS101 | grep -o '"id":"[^"]*' | cut -d'"' -f4)
    CS201_ID=$(echo $COURSE_CS201 | grep -o '"id":"[^"]*' | cut -d'"' -f4)

    log_success "课程目录服务测试完成"
    echo ""
}

# 函数：测试学生管理服务
test_student_service() {
    log_info "=== 测试学生管理服务 ==="

    # 1. 创建学生 - 张三
    log_info "1. 创建学生：张三"
    STUDENT_1=$(curl -s -X POST "$USER_URL/api/students" \
        -H "Content-Type: application/json; charset=UTF-8" \
        -d '{
            "studentId": "S2024001",
            "name": "张三",
            "major": "计算机科学与技术",
            "grade": 2024,
            "email": "zhangsan@example.edu.cn"
        }')
    echo "响应: $STUDENT_1"

    # 2. 创建学生 - 李四
    log_info "2. 创建学生：李四"
    STUDENT_2=$(curl -s -X POST "$USER_URL/api/students" \
        -H "Content-Type: application/json; charset=UTF-8" \
        -d '{
            "studentId": "S2024002",
            "name": "李四",
            "major": "软件工程",
            "grade": 2024,
            "email": "lisi@example.edu.cn"
        }')
    echo "响应: $STUDENT_2"

    # 3. 创建学生 - 王五
    log_info "3. 创建学生：王五"
    STUDENT_3=$(curl -s -X POST "$USER_URL/api/students" \
        -H "Content-Type: application/json; charset=UTF-8" \
        -d '{
            "studentId": "S2024003",
            "name": "王五",
            "major": "人工智能",
            "grade": 2024,
            "email": "wangwu@example.edu.cn"
        }')
    echo "响应: $STUDENT_3"

    # 4. 获取所有学生
    log_info "4. 获取所有学生"
    ALL_STUDENTS=$(curl -s -X GET "$USER_URL/api/students")
    echo "所有学生: $ALL_STUDENTS"

    # 5. 测试按学号查询
    log_info "5. 测试按学号查询"
    STUDENT_BY_ID=$(curl -s -X GET "$USER_URL/api/students?studentid=S2024001")
    echo "按学号查询结果: $STUDENT_BY_ID"

    log_success "学生管理服务测试完成"
    echo ""
}

# 函数：测试选课功能
test_enrollment_function() {
    log_info "=== 测试选课功能 ==="

    # 1. 张三选课 - 计算机科学导论（使用课程代码）
    log_info "1. 张三选课：计算机科学导论"
    ENROLLMENT_1=$(curl -s -X POST "$ENROLLMENT_URL/api/enrollments" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "CS101",
            "studentId": "S2024001"
        }')
    echo "响应: $ENROLLMENT_1"

    # 2. 李四选课 - 计算机科学导论
    log_info "2. 李四选课：计算机科学导论"
    ENROLLMENT_2=$(curl -s -X POST "$ENROLLMENT_URL/api/enrollments" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "CS101",
            "studentId": "S2024002"
        }')
    echo "响应: $ENROLLMENT_2"

    # 3. 王五选课 - 数据结构与算法
    log_info "3. 王五选课：数据结构与算法"
    ENROLLMENT_3=$(curl -s -X POST "$ENROLLMENT_URL/api/enrollments" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "CS201",
            "studentId": "S2024003"
        }')
    echo "响应: $ENROLLMENT_3"

    # 4. 获取所有选课记录
    log_info "4. 获取所有选课记录"
    ALL_ENROLLMENTS=$(curl -s -X GET "$ENROLLMENT_URL/api/enrollments")
    echo "所有选课记录: $ALL_ENROLLMENTS"

    log_success "选课功能测试完成"
    echo ""
}

# 函数：测试服务间通信和错误处理
test_error_handling() {
    log_info "=== 测试错误处理和服务间通信 ==="

    # 1. 测试课程不存在的情况
    log_info "1. 测试课程不存在的情况"
    ERROR_RESPONSE=$(curl -s -X POST "$ENROLLMENT_URL/api/enrollments" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "NONEXISTENT",
            "studentId": "S2024001"
        }')
    echo "错误响应: $ERROR_RESPONSE"

    # 2. 测试学生不存在的情况
    log_info "2. 测试学生不存在的情况"
    ERROR_RESPONSE2=$(curl -s -X POST "$ENROLLMENT_URL/api/enrollments" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "CS101",
            "studentId": "NONEXISTENT"
        }')
    echo "错误响应: $ERROR_RESPONSE2"

    # 3. 测试重复选课
    log_info "3. 测试重复选课（张三再次选同一门课）"
    DUPLICATE_ENROLLMENT=$(curl -s -X POST "$ENROLLMENT_URL/api/enrollments" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "CS101",
            "studentId": "S2024001"
        }')
    echo "重复选课响应: $DUPLICATE_ENROLLMENT"

    # 4. 按课程查询选课记录
    log_info "4. 按课程查询选课记录（计算机科学导论）"
    COURSE_ENROLLMENTS=$(curl -s -X GET "$ENROLLMENT_URL/api/enrollments/course/$CS101_ID")
    echo "课程选课记录: $COURSE_ENROLLMENTS"

    # 5. 按学生查询选课记录
    log_info "5. 按学生查询选课记录（张三）"
    STUDENT_ENROLLMENTS=$(curl -s -X GET "$ENROLLMENT_URL/api/enrollments/student/S2024001")
    echo "学生选课记录: $STUDENT_ENROLLMENTS"

    log_success "错误处理测试完成"
    echo ""
}

# 函数：测试课程容量限制
test_capacity_limits() {
    log_info "=== 测试课程容量限制 ==="

    # 创建一个容量很小的课程进行测试
    log_info "1. 创建小容量测试课程"
    SMALL_COURSE=$(curl -s -X POST "$CATALOG_URL/api/courses" \
        -H "Content-Type: application/json" \
        -d '{
            "code": "TEST101",
            "title": "Capacity Test Course",
            "instructor": {
                "id": "T999",
                "name": "Test Professor",
                "email": "test@example.edu.cn"
            },
            "schedule": {
                "dayOfWeek": "THURSDAY",
                "startTime": "16:00",
                "endTime": "18:00",
                "location": "Test Classroom"
            },
            "capacity": 1,
            "enrolled": 0
        }')
    echo "响应: $SMALL_COURSE"

    # 第一个学生选课成功
    log_info "2. 第一个学生选课（应该成功）"
    ENROLL_SUCCESS=$(curl -s -X POST "$ENROLLMENT_URL/api/enrollments" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "TEST101",
            "studentId": "S2024001"
        }')
    echo "响应: $ENROLL_SUCCESS"

    # 第二个学生选课应该失败（容量已满）
    log_info "3. 第二个学生选课（应该失败 - 容量已满）"
    ENROLL_FAIL=$(curl -s -X POST "$ENROLLMENT_URL/api/enrollments" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "TEST101",
            "studentId": "S2024002"
        }')
    echo "响应: $ENROLL_FAIL"

    log_success "容量限制测试完成"
    echo ""
}

# 函数：测试学生删除保护
test_student_deletion_protection() {
    log_info "=== 测试学生删除保护 ==="

    # 尝试删除有选课记录的学生
    log_info "1. 尝试删除有选课记录的学生（张三）"
    STUDENT_ID=$(echo $STUDENT_1 | grep -o '"id":"[^"]*' | cut -d'"' -f4)
    DELETE_RESPONSE=$(curl -s -X DELETE "$USER_URL/api/students/$STUDENT_ID")
    echo "删除响应: $DELETE_RESPONSE"

    log_success "学生删除保护测试完成"
    echo ""
}

# 函数：生成测试报告
generate_test_report() {
    log_info "=== 生成测试报告 ==="

    echo "📊 测试报告摘要"
    echo "-------------------"
    echo "✅ 课程目录服务测试完成"
    echo "✅ 学生管理服务测试完成"
    echo "✅ 选课功能测试完成"
    echo "✅ 错误处理测试完成"
    echo "✅ 容量限制测试完成"
    echo "✅ 学生删除保护测试完成"
    echo ""

    # 最终状态检查
    log_info "最终状态检查："
    echo "课程数量: $(curl -s -X GET "$CATALOG_URL/api/courses" | grep -o '"id"' | wc -l)"
    echo "学生数量: $(curl -s -X GET "$USER_URL/api/students" | grep -o '"studentId"' | wc -l)"
    echo "选课记录数量: $(curl -s -X GET "$ENROLLMENT_URL/api/enrollments" | grep -o '"id"' | wc -l)"

    echo ""
    log_success "所有测试执行完成！"
    echo "结束时间: $(date)"
}

# 主执行函数
main() {
    echo "开始执行微服务测试..."
    echo ""

    # 检查服务可用性
    check_service_availability

    # 执行各项测试
    test_catalog_service
    test_student_service
    test_enrollment_function
    test_error_handling
    test_capacity_limits
    test_student_deletion_protection

    # 生成测试报告
    generate_test_report
}

# 执行主函数
main
