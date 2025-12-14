#!/bin/bash

# 启动多实例微服务脚本
# 用于启动多个相同服务的实例，测试负载均衡效果

echo "=============================================="
echo "   启动多实例微服务脚本"
echo "=============================================="
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

# 函数：检查端口是否被占用
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        return 0  # 端口被占用
    else
        return 1  # 端口可用
    fi
}

# 函数：停止现有服务
stop_existing_services() {
    log_info "停止现有服务..."
    
    # 停止使用标准docker-compose.yml启动的服务
    if [ -f "docker-compose.yml" ]; then
        docker-compose down
    fi
    
    # 停止使用多实例docker-compose启动的服务
    if [ -f "docker-compose-multi-instance.yml" ]; then
        docker-compose -f docker-compose-multi-instance.yml down
    fi
    
    log_success "现有服务已停止"
    echo ""
}

# 函数：检查必需端口
check_required_ports() {
    log_info "检查必需端口..."
    
    local required_ports=(8848 3306 3307 3308 3309 8081 8082 8083 8084 8085 8086 8087 8088 8090)
    local occupied_ports=()
    
    for port in "${required_ports[@]}"; do
        if check_port $port; then
            occupied_ports+=($port)
        fi
    done
    
    if [ ${#occupied_ports[@]} -gt 0 ]; then
        log_warning "以下端口已被占用: ${occupied_ports[*]}"
        log_warning "请确保这些端口没有被其他服务使用"
        echo ""
    else
        log_success "所有必需端口都可用"
        echo ""
    fi
}

# 函数：构建微服务
build_microservices() {
    log_info "构建微服务..."
    
    # 构建 user-service
    log_info "构建 user-service..."
    cd user-service
    mvn clean package -DskipTests
    if [ $? -eq 0 ]; then
        log_success "user-service 构建成功"
    else
        log_error "user-service 构建失败"
        exit 1
    fi
    cd ..
    
    # 构建 catalog-service
    log_info "构建 catalog-service..."
    cd catalog-service
    mvn clean package -DskipTests
    if [ $? -eq 0 ]; then
        log_success "catalog-service 构建成功"
    else
        log_error "catalog-service 构建失败"
        exit 1
    fi
    cd ..
    
    # 构建 enrollment-service
    log_info "构建 enrollment-service..."
    cd enrollment-service
    mvn clean package -DskipTests
    if [ $? -eq 0 ]; then
        log_success "enrollment-service 构建成功"
    else
        log_error "enrollment-service 构建失败"
        exit 1
    fi
    cd ..
    
    # 构建 gateway-service
    log_info "构建 gateway-service..."
    cd gateway-service
    mvn clean package -DskipTests
    if [ $? -eq 0 ]; then
        log_success "gateway-service 构建成功"
    else
        log_error "gateway-service 构建失败"
        exit 1
    fi
    cd ..
    
    echo ""
}

# 函数：启动多实例服务
start_multi_instance_services() {
    log_info "构建镜像"
    docker build -t user-service:latest ./user-service
    docker build -t catalog-service:latest ./catalog-service
    docker build -t enrollment-service:latest ./enrollment-service
    docker build -t gateway-service:latest ./gateway-service
    
    log_info "启动多实例服务..."
    
    if [ ! -f "docker-compose-multi-instance.yml" ]; then
        log_error "找不到 docker-compose-multi-instance.yml 文件"
        exit 1
    fi
    
    # 启动服务
    docker-compose -f docker-compose-multi-instance.yml up -d
    
    if [ $? -eq 0 ]; then
        log_success "多实例服务启动成功"
    else
        log_error "多实例服务启动失败"
        exit 1
    fi
    
    echo ""
}

# 函数：等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."
    
    local max_attempts=30
    local attempt=1
    
    # 等待 Nacos 就绪
    log_info "等待 Nacos 服务就绪..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:8848/nacos/ > /dev/null 2>&1; then
            log_success "Nacos 服务已就绪"
            break
        fi
        
        echo "等待 Nacos 服务... ($attempt/$max_attempts)"
        sleep 5
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_error "Nacos 服务启动超时"
        exit 1
    fi
    
    # 等待数据库就绪
    log_info "等待数据库服务就绪..."
    sleep 10
    
    # 等待微服务就绪
    log_info "等待微服务就绪..."
    sleep 20
    
    echo ""
}

# 函数：验证服务状态
verify_services() {
    log_info "验证服务状态..."
    
    # 检查容器状态
    log_info "检查容器状态:"
    docker-compose -f docker-compose-multi-instance.yml ps
    
    echo ""
    
    # 检查 Nacos 中的服务注册
    log_info "检查 Nacos 服务注册..."
    
    # 等待一下让服务有时间注册
    sleep 10
    
    # 尝试获取 Nacos 服务列表
    if curl -s http://localhost:8848/nacos/v1/ns/service/list | grep -q "user-service"; then
        log_success "user-service 已注册到 Nacos"
    else
        log_warning "user-service 可能未注册到 Nacos"
    fi
    
    if curl -s http://localhost:8848/nacos/v1/ns/service/list | grep -q "catalog-service"; then
        log_success "catalog-service 已注册到 Nacos"
    else
        log_warning "catalog-service 可能未注册到 Nacos"
    fi
    
    if curl -s http://localhost:8848/nacos/v1/ns/service/list | grep -q "enrollment-service"; then
        log_success "enrollment-service 已注册到 Nacos"
    else
        log_warning "enrollment-service 可能未注册到 Nacos"
    fi
    
    if curl -s http://localhost:8848/nacos/v1/ns/service/list | grep -q "gateway-service"; then
        log_success "gateway-service 已注册到 Nacos"
    else
        log_warning "gateway-service 可能未注册到 Nacos"
    fi
    
    echo ""
}

# 函数：显示服务信息
show_service_info() {
    log_info "服务信息:"
    echo ""
    
    echo "📊 多实例服务配置:"
    echo "-------------------"
    echo "✅ user-service: 3个实例"
    echo "   - 实例1: http://localhost:8081 (容器端口: 8081)"
    echo "   - 实例2: http://localhost:8084 (容器端口: 8081)"
    echo "   - 实例3: http://localhost:8085 (容器端口: 8081)"
    echo ""
    echo "✅ catalog-service: 3个实例"
    echo "   - 实例1: http://localhost:8082 (容器端口: 8082)"
    echo "   - 实例2: http://localhost:8086 (容器端口: 8082)"
    echo "   - 实例3: http://localhost:8087 (容器端口: 8082)"
    echo ""
    echo "✅ enrollment-service: 2个实例"
    echo "   - 实例1: http://localhost:8083 (容器端口: 8083)"
    echo "   - 实例2: http://localhost:8088 (容器端口: 8083)"
    echo ""
    echo "✅ gateway-service: 1个实例"
    echo "   - 实例1: http://localhost:8090 (容器端口: 8090)"
    echo ""
    echo "✅ Nacos 控制台: http://localhost:8848/nacos"
    echo "   - 用户名: nacos"
    echo "   - 密码: nacos"
    echo ""
    echo "📋 测试脚本:"
    echo "-------------"
    echo "1. 测试负载均衡效果:"
    echo "   ./multi-instance-load-balancing-enhanced.sh"
    echo ""
    echo "2. 测试端口响应:"
    echo "   ./test-port-response.sh"
    echo ""
    echo "3. 完整功能测试:"
    echo "   ./test-services.sh"
    echo ""
    echo "4. JWT认证测试:"
    echo "   ./test-jwt-auth.sh"
    echo ""
}

# 主执行函数
main() {
    echo "开始启动多实例微服务..."
    echo ""
    
    # 1. 停止现有服务
    stop_existing_services
    
    # 2. 检查端口
    check_required_ports
    
    # 3. 构建微服务
    build_microservices
    
    # 4. 启动多实例服务
    start_multi_instance_services
    
    # 5. 等待服务就绪
    wait_for_services
    
    # 6. 验证服务状态
    verify_services
    
    # 7. 显示服务信息
    show_service_info
    
    log_success "多实例微服务启动完成！"
    echo ""
    echo "🚀 现在可以运行负载均衡测试脚本:"
    echo "   ./multi-instance-load-balancing-enhanced.sh"
    echo ""
}

# 执行主函数
main
