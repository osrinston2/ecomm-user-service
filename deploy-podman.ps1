
# deploy-podman.ps1
# Deployment for ecomm-user-service

$ErrorActionPreference = "Stop"

$Network = "ecomm-network"
$UserContainer = "ecomm-user-service"
$ImageTag = $env:IMAGE_TAG

if ([string]::IsNullOrWhiteSpace($ImageTag)) {
    throw "IMAGE_TAG environment variable was not provided by Jenkins."
}

$UserImage = "localhost/ecomm-user-service:$ImageTag"

Write-Host "Deploying image: $UserImage" -ForegroundColor Cyan

Write-Host "=== Deploying E-Commerce User Service ===" -ForegroundColor Cyan

# 1. Verify shared infrastructure
$RequiredContainers = @(
    "ecomm-mysql",
    "ecomm-redis",
    "ecomm-auth-service"
)

foreach ($Container in $RequiredContainers) {

    podman container exists $Container

    if ($LASTEXITCODE -ne 0) {
        throw "Required container '$Container' does not exist."
    }

    $Running = podman inspect --format '{{.State.Running}}' $Container

    if ($Running -ne "true") {
        Write-Host "Starting $Container..." -ForegroundColor Yellow
        podman start $Container

        if ($LASTEXITCODE -ne 0) {
            throw "Failed to start $Container."
        }
    }
}

# 2. Verify shared network
podman network exists $Network

if ($LASTEXITCODE -ne 0) {
    throw "Network '$Network' does not exist."
}

# 3. Recreate user-service container
Write-Host "Removing previous User Service container..." -ForegroundColor Yellow

podman rm -f $UserContainer 2>$null | Out-Null

# 4. Deploy user-service
Write-Host "Starting User Service..." -ForegroundColor Yellow

podman run -d `
    --name $UserContainer `
    --network $Network `
    --network-alias $UserContainer `
    --restart unless-stopped `
    -p 8082:8082 `
    -e SERVER_PORT=8082 `
    -e DB_HOST=ecomm-mysql `
    -e DB_PORT=3306 `
    -e DB_NAME=ecomm_user_db `
    -e DB_USER=root `
    -e DB_PASSWORD=rootpassword `
    -e REDIS_HOST=ecomm-redis `
    -e REDIS_PORT=6379 `
    -e AUTH_SERVICE_URL=http://ecomm-auth-service:8081 `
    -e JWT_ISSUER=ecomm-auth-service `
    $UserImage

if ($LASTEXITCODE -ne 0) {
    throw "Failed to start User Service."
}

Write-Host "`n=== User Service Deployment Completed ===" -ForegroundColor Green
Write-Host "User Service: http://localhost:8082"
Write-Host "Health:       http://localhost:8082/actuator/health"
Write-Host "Auth Service: http://ecomm-auth-service:8081"
Write-Host "Network:      $Network"
