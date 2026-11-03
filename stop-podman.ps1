# stop-podman.ps1 - Cleanly stops and removes ecomm-user-service and ecomm-user-pod
Write-Host "Stopping and removing ecomm-user-service..." -ForegroundColor Yellow
podman rm -f ecomm-user-service 2>$null | Out-Null
podman pod stop ecomm-user-pod 2>$null | Out-Null
podman pod rm -f ecomm-user-pod 2>$null | Out-Null
Write-Host "ecomm-user-service has been stopped and removed." -ForegroundColor Green
