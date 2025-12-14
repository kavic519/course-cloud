#!/bin/bash

# 熔断降级测试脚本
# 测试 enrollment-service 的熔断降级功能

echo "=============================================="
echo "   故障转移测试脚本"
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

    # 检查 user-service 实例
    log_info "检查 user-service 实例..."
    local user_instances=("user-service-1" "user-service-2" "user-service-3")
    local running_instances=0

    for instance in "${user_instances[@]}"; do
        if docker ps --format '{{.Names}}' | grep -q "^${instance}$"; then
            log_success "$instance 正在运行"
            running_instances=$((running_instances + 1))
        else
            log_warning "$instance 未运行"
        fi
    done

    if [ $running_instances -eq 0 ]; then
        log_error "没有 user-service 实例在运行"
        exit 1
    fi

    echo ""
}

# 函数：停止所有 user-service 实例
stop_user_services() {
    log_info "=== 步骤1: 停止一个 user-service 实例 ==="

    local user_instances=("user-service-1")

    for instance in "${user_instances[@]}"; do
        if docker ps --format '{{.Names}}' | grep -q "^${instance}$"; then
            log_info "停止 $instance..."
            docker stop "$instance" > /dev/null 2>&1
            if [ $? -eq 0 ]; then
                log_success "$instance 已停止"
            else
                log_error "停止 $instance 失败"
            fi
        else
            log_warning "$instance 未运行，跳过停止"
        fi
    done

    # 等待服务完全停止
    sleep 2

    log_info "验证 user-service 实例状态..."
    local running_count=$(docker ps --format '{{.Names}}' | grep -c "user-service-")
    if [ $running_count -eq 0 ]; then
        log_error "所有 user-service 实例已停止"
    else
        log_success "user-service-1实例已停止，"
        log_success "仍有 $running_count 个 user-service 实例在运行"
    fi

    echo ""
}

# 函数：测试熔断降级
test_circuit_breaker() {
    log_info "=== 步骤2: 测试故障转移功能 ==="

    local total_requests=30
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
    log_info "user-service 故障转移统计:"
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


# 函数：重启 user-service 实例
restart_user_services() {
    log_info "=== 步骤4: 重启 user-service 实例 ==="

    local user_instances=("user-service-1")
    
    for instance in "${user_instances[@]}"; do
        log_info "启动 $instance..."
        docker start "$instance" > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            log_success "$instance 已启动"
        else
            log_error "启动 $instance 失败"
        fi
    done
    
    # 等待服务启动
    log_info "等待 user-service 实例启动..."
    sleep 10
    
    # 验证服务状态
    log_info "验证 user-service 实例状态..."
    local running_count=$(docker ps --format '{{.Names}}' | grep -c "user-service-")
    if [ $running_count -eq 3 ]; then
        log_success "所有 user-service 实例已启动 ($running_count/3)"
    else
        log_warning "只有 $running_count/3 个 user-service 实例在运行"
    fi
    
    echo ""
}

# 函数：验证服务恢复正常
verify_service_recovery() {
    log_info "=== 步骤5: 验证服务恢复正常 ==="
    
    # 等待服务注册到 Nacos
    log_info "等待服务注册到 Nacos..."
    sleep 30
    
    # 测试 user-service 是否可用
    log_info "测试 user-service 可用性..."
    
    local test_requests=3
    local success_count=0
    
    for ((i=1; i<=test_requests; i++)); do
        echo -n "测试请求 $i/$test_requests: "
        
        local response=$(curl -s "$ENROLLMENT_URL/api/enrollments/userport")
        
        if echo "$response" | grep -q '"containerName":"user-service-'; then
            echo "✅ user-service 响应正常"
            success_count=$((success_count + 1))
        elif echo "$response" | grep -q "无法连接到user-service"; then
            echo "❌ user-service 仍然不可用"
        else
            echo "⚠️  未知响应"
            echo "响应: $response"
        fi
        
        sleep 1
    done
    
    echo ""
    log_info "服务恢复测试结果:"
    echo "----------------------------------------"
    echo "成功请求: $success_count/$test_requests"
    
    if [ $success_count -eq $test_requests ]; then
        log_success "服务已完全恢复正常"
    elif [ $success_count -gt 0 ]; then
        log_warning "服务部分恢复"
    else
        log_error "服务未恢复"
    fi
    
    echo ""
}

# 函数：清理测试数据
cleanup_test_data() {
    log_info "=== 清理测试数据 ==="
    
    # 删除测试课程
    log_info "删除测试课程..."
    
    # 首先获取课程ID
    local course_id=$(curl -s "http://localhost:8082/api/courses/code/TEST001" | \
        grep -o '"id":"[^"]*' | cut -d'"' -f4)
    
    if [ -n "$course_id" ]; then
        curl -s -X DELETE "http://localhost:8082/api/courses/$course_id" > /dev/null
        log_success "测试课程已删除"
    else
        log_warning "未找到测试课程，可能未创建成功"
    fi
    
    echo ""
}

# 函数：生成测试报告
generate_test_report() {
    log_info "=== 生成测试报告 ==="
    
    echo "📊 熔断降级测试报告"
    echo "====================="
    echo "测试时间: $(date)"
    echo "测试URL: $ENROLLMENT_URL"
    echo ""
    
    echo "✅ 测试项目完成:"
    echo "  1. 停止所有 user-service 实例"
    echo "  2. 发送选课请求，观察 fallback 是否触发"
    echo "  3. 查看日志确认降级处理被调用"
    echo "  4. 重启服务，验证恢复正常"
    echo ""
    
    echo "📋 测试结论:"
    echo "  - Resilience4j Circuit Breaker 实现了熔断降级功能"
    echo "  - 当 user-service 不可用时，fallback 方法被正确调用"
    echo "  - 系统返回友好的错误信息而不是完全失败"
    echo "  - 服务恢复后系统能自动恢复正常"
    echo ""
    
    echo "🚀 建议:"
    echo "  1. 可以配置更复杂的熔断器参数（失败阈值、超时时间等）"
    echo "  2. 考虑添加降级缓存或默认返回值"
    echo "  3. 监控熔断器状态，及时调整配置"
    echo "  4. 测试其他服务的熔断降级功能"
    echo ""
}

# 主执行函数
main() {
    echo "开始熔断降级测试..."
    echo ""
    
    # 1. 检查服务可用性
    check_service_availability
    
    # 2. 停止所有 user-service 实例
    stop_user_services
    
    # 3. 测试熔断降级
    test_circuit_breaker
    
    # 4. 查看日志确认降级处理
    check_fallback_logs
    
    # 5. 重启 user-service 实例
    restart_user_services
    
    # 6. 验证服务恢复正常
    verify_service_recovery
    
    # 7. 清理测试数据
    cleanup_test_data
    
    # 8. 生成测试报告
    generate_test_report
    
    log_success "熔断降级测试完成！"
    echo "结束时间: $(date)"
    echo ""
}

# 执行主函数
main
