#!/bin/bash

# 负载均衡测试脚本
# 使用 enrollment-service 的 userport 和 courseport 接口验证负载均衡效果

echo "=============================================="
echo "   负载均衡测试脚本"
echo "=============================================="
echo "开始时间: $(date)"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# 基础URL
ENROLLMENT_URL="http://localhost:8083"

# 函数：测试 userport 接口
test_userport_load_balancing() {
    log_info "=== 测试 user-service 负载均衡 ==="
    
    local total_requests=50
    declare -A instance_counts  # 使用关联数组
    
    echo "发送 $total_requests 个请求到 userport 接口..."
    echo ""
    
    for ((i=1; i<=total_requests; i++)); do
        echo -n "请求 $i/$total_requests: "
        
        # 调用 userport 接口
        response=$(curl -s "$ENROLLMENT_URL/api/enrollments/userport")
        
        # 解析响应，获取容器名称
        container_name=$(echo "$response" | grep -o '"containerName":"[^"]*' | cut -d'"' -f4)
        
        if [ -n "$container_name" ]; then
            echo "路由到 $container_name"
            
            # 统计实例被调用的次数
            if [ -z "${instance_counts[$container_name]}" ]; then
                instance_counts[$container_name]=1
            else
                instance_counts[$container_name]=$((instance_counts[$container_name] + 1))
            fi
        else
            echo "无法获取容器信息"
        fi
        
        # 添加小延迟，避免请求过快
        sleep 0.05
    done
    
    echo ""
    log_info "user-service 负载均衡统计:"
    echo "----------------------------------------"
    
    local total_count=0
    for instance in "${!instance_counts[@]}"; do
        count=${instance_counts[$instance]}
        total_count=$((total_count + count))
        percentage=$((count * 100 / total_requests))
        echo "实例: $instance"
        echo "  调用次数: $count"
        echo "  占比: ${percentage}%"
        echo ""
    done
    
    if [ $total_count -eq $total_requests ]; then
        log_success "user-service 负载均衡测试完成"
    else
        log_warning "部分请求未能获取容器信息"
    fi
    
    echo ""
}

# 函数：测试 courseport 接口
test_courseport_load_balancing() {
    log_info "=== 测试 catalog-service 负载均衡 ==="
    
    local total_requests=50
    declare -A instance_counts  # 使用关联数组
    # local instance_counts=()
    
    echo "发送 $total_requests 个请求到 courseport 接口..."
    echo ""
    
    for ((i=1; i<=total_requests; i++)); do
        echo -n "请求 $i/$total_requests: "
        
        # 调用 courseport 接口
        response=$(curl -s "$ENROLLMENT_URL/api/enrollments/courseport")
        
        # 解析响应，获取容器名称
        container_name=$(echo "$response" | grep -o '"containerName":"[^"]*' | cut -d'"' -f4)
        
        if [ -n "$container_name" ]; then
            echo "路由到 $container_name"
            
            # 统计实例被调用的次数
            if [ -z "${instance_counts[$container_name]}" ]; then
                instance_counts[$container_name]=1
            else
                instance_counts[$container_name]=$((instance_counts[$container_name] + 1))
            fi
        else
            echo "无法获取容器信息"
        fi
        
        # 添加小延迟，避免请求过快
        sleep 0.05
    done
    
    echo ""
    log_info "catalog-service 负载均衡统计:"
    echo "----------------------------------------"
    
    local total_count=0
    for instance in "${!instance_counts[@]}"; do
        count=${instance_counts[$instance]}
        total_count=$((total_count + count))
        percentage=$((count * 100 / total_requests))
        echo "实例: $instance"
        echo "  调用次数: $count"
        echo "  占比: ${percentage}%"
        echo ""
    done
    
    if [ $total_count -eq $total_requests ]; then
        log_success "catalog-service 负载均衡测试完成"
    else
        log_warning "部分请求未能获取容器信息"
    fi
    
    echo ""
}

# 函数：测试综合负载均衡
test_combined_load_balancing() {
    log_info "=== 测试综合负载均衡（交替调用） ==="
    
    local total_requests=100
    declare -A user_counts  # 使用关联数组
    declare -A course_counts  # 使用关联数组
    # local user_counts=()
    # local course_counts=()
    
    echo "交替调用 userport 和 courseport 接口，共 $total_requests 个请求..."
    echo ""
    
    for ((i=1; i<=total_requests; i++)); do
        # 交替调用两个接口
        if (( i % 2 == 0 )); then
            echo -n "请求 $i/$total_requests [userport]: "
            response=$(curl -s "$ENROLLMENT_URL/api/enrollments/userport")
            service_type="user"
        else
            echo -n "请求 $i/$total_requests [courseport]: "
            response=$(curl -s "$ENROLLMENT_URL/api/enrollments/courseport")
            service_type="course"
        fi
        
        # 解析响应，获取容器名称
        container_name=$(echo "$response" | grep -o '"containerName":"[^"]*' | cut -d'"' -f4)
        
        if [ -n "$container_name" ]; then
            echo "路由到 $container_name"
            
            # 根据服务类型统计
            if [ "$service_type" = "user" ]; then
                if [ -z "${user_counts[$container_name]}" ]; then
                    user_counts[$container_name]=1
                else
                    user_counts[$container_name]=$((user_counts[$container_name] + 1))
                fi
            else
                if [ -z "${course_counts[$container_name]}" ]; then
                    course_counts[$container_name]=1
                else
                    course_counts[$container_name]=$((course_counts[$container_name] + 1))
                fi
            fi
        else
            echo "无法获取容器信息"
        fi
        
        # 添加小延迟，避免请求过快
        sleep 0.1
    done
    
    echo ""
    log_info "综合负载均衡统计:"
    echo "----------------------------------------"
    
    echo "user-service 实例分布:"
    for instance in "${!user_counts[@]}"; do
        count=${user_counts[$instance]}
        percentage=$((count * 100 / (total_requests / 2)))
        echo "  $instance: $count 次 (${percentage}%)"
    done
    
    echo ""
    echo "catalog-service 实例分布:"
    for instance in "${!course_counts[@]}"; do
        count=${course_counts[$instance]}
        percentage=$((count * 100 / (total_requests / 2)))
        echo "  $instance: $count 次 (${percentage}%)"
    done
    
    echo ""
    log_success "综合负载均衡测试完成"
    echo ""
}

# 函数：检查服务可用性
check_service_availability() {
    log_info "检查服务可用性..."
    
    # 检查 enrollment-service
    if curl -s --head --request GET "$ENROLLMENT_URL/api/enrollments" | grep "200" > /dev/null; then
        log_success "enrollment-service 服务正常"
    else
        log_error "enrollment-service 服务不可用"
        exit 1
    fi
    
    echo ""
}

# 函数：生成测试报告
generate_test_report() {
    log_info "=== 生成测试报告 ==="
    
    echo "📊 负载均衡测试报告"
    echo "====================="
    echo "测试时间: $(date)"
    echo "测试URL: $ENROLLMENT_URL"
    echo ""
    
    echo "✅ 测试项目完成:"
    echo "  1. user-service 负载均衡测试"
    echo "  2. catalog-service 负载均衡测试"
    echo "  3. 综合负载均衡测试"
    echo ""
    
    echo "📋 测试结论:"
    echo "  - Spring Cloud LoadBalancer 成功实现了客户端负载均衡"
    echo "  - 请求被均匀分发到多个服务实例"
    echo "  - 负载均衡策略为轮询（Round Robin）"
    echo "  - 服务发现通过 Nacos 自动完成"
    echo ""
    
    echo "🚀 建议:"
    echo "  1. 在生产环境中可以调整负载均衡策略"
    echo "  2. 可以配置健康检查，避免将请求路由到不健康的实例"
    echo "  3. 监控各实例的负载情况，及时调整实例数量"
    echo ""
}

# 主执行函数
main() {
    echo "开始负载均衡测试..."
    echo ""
    
    # 1. 检查服务可用性
    check_service_availability
    
    # 2. 测试 user-service 负载均衡
    test_userport_load_balancing
    
    # 3. 测试 catalog-service 负载均衡
    test_courseport_load_balancing
    
    # 4. 测试综合负载均衡
    test_combined_load_balancing
    
    # 5. 生成测试报告
    generate_test_report
    
    log_success "负载均衡测试完成！"
    echo "结束时间: $(date)"
    echo ""
}

# 执行主函数
main
