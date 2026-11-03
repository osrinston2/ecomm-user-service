# deploy-podman.ps1 - Native Podman deployment for ecomm-user-service (Port 8082)
Write-Host "=== Deploying E-Commerce User Service to Podman ===" -ForegroundColor Cyan

# 1. Stop and remove existing user-service pod / container if present
podman pod stop ecomm-user-pod 2>$null | Out-Null
podman pod rm -f ecomm-user-pod 2>$null | Out-Null
podman rm -f ecomm-user-service 2>$null | Out-Null

# 2. Build User Service Image
Write-Host "[1/3] Building ecomm-user-service container image..." -ForegroundColor Yellow
if (Test-Path "target\user-service-1.0.0-SNAPSHOT.jar") {
    podman build -f Dockerfile.fast -t ecomm-user-service:latest .
} else {
    podman build -f Dockerfile -t ecomm-user-service:latest .
}

# 3. Check if ecomm-mysql container already exists (from ecomm-auth-service)
podman container exists ecomm-mysql
if ($LASTEXITCODE -eq 0) {
    podman start ecomm-mysql ecomm-redis ecomm-auth-service 2>$null | Out-Null

    Write-Host "[2/3] Detected existing 'ecomm-mysql' & 'ecomm-redis'. Provisioning database 'ecomm_user_db'..." -ForegroundColor Yellow
    podman exec ecomm-mysql mysql -u root -prootpassword -e "CREATE DATABASE IF NOT EXISTS ecomm_user_db; GRANT ALL PRIVILEGES ON ecomm_user_db.* TO 'auth_user'@'%'; FLUSH PRIVILEGES;" 2>$null

    Write-Host "[3/3] Starting 'ecomm-user-service' (port 8082) on shared network 'ecomm-auth-service_ecomm-network'..." -ForegroundColor Yellow
    podman run -d --name ecomm-user-service `
        --network ecomm-auth-service_ecomm-network `
        -p 8082:8082 `
        -e JAVA_OPTS="-XX:-UseContainerSupport -XX:+UseG1GC -XX:MaxRAMPercentage=75.0" `
        -e SPRING_PROFILES_ACTIVE=docker `
        -e SERVER_PORT=8082 `
        -e DB_HOST=ecomm-mysql `
        -e DB_PORT=3306 `
        -e DB_NAME=ecomm_user_db `
        -e DB_USER=root `
        -e DB_PASSWORD=rootpassword `
        -e REDIS_HOST=ecomm-redis `
        -e REDIS_PORT=6379 `
        -e AUTH_SERVICE_URL=http://ecomm-auth-service:8081 `
        ecomm-user-service:latest
} else {
    Write-Host "[2/3] 'ecomm-mysql' not found. Creating standalone 'ecomm-user-pod' with MySQL & Redis..." -ForegroundColor Yellow
    podman pod create --name ecomm-user-pod -p 8082:8082 -p 3306:3306 -p 6379:6379

    podman run -d --pod ecomm-user-pod --name ecomm-user-mysql `
        -e MYSQL_ROOT_PASSWORD=rootpassword `
        -e MYSQL_DATABASE=ecomm_user_db `
        docker.io/library/mysql:8.0

    podman run -d --pod ecomm-user-pod --name ecomm-user-redis `
        docker.io/library/redis:7.2-alpine

    Write-Host "[3/3] Starting ecomm-user-service container on port 8082..." -ForegroundColor Yellow
    podman run -d --pod ecomm-user-pod --name ecomm-user-service `
        -e JAVA_OPTS="-XX:-UseContainerSupport -XX:+UseG1GC -XX:MaxRAMPercentage=75.0" `
        -e SERVER_PORT=8082 `
        -e DB_HOST=localhost `
        -e DB_PORT=3306 `
        -e DB_NAME=ecomm_user_db `
        -e DB_USER=root `
        -e DB_PASSWORD=rootpassword `
        -e REDIS_HOST=localhost `
        -e REDIS_PORT=6379 `
        -e AUTH_SERVICE_URL=http://host.containers.internal:8081 `
        ecomm-user-service:latest
}

Write-Host "`n=== Deployment Completed Successfully! ===" -ForegroundColor Green
Write-Host "User Service is running on port 8082."
Write-Host "Health Endpoint: http://localhost:8082/actuator/health"
