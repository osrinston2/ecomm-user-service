@Library('ecomm-ci') _

pipeline {
    agent any

    tools {
        jdk 'jdk-18'
    }

    stages {

        stage('Verify Java') {
            steps {
                echo 'Checking Java version...'
                bat 'java -version'
                bat 'echo JAVA_HOME=%JAVA_HOME%'
            }
        }

        stage('Determine Image Metadata') {
            steps {
                script {
                    prepareImageMetadata()
                }
            }
        }

        stage('Run Tests') {
            steps {
                echo 'Running automated tests...'
                bat '.\\mvnw.cmd -B test'
            }
        }

        stage('Build Application') {
            steps {
                echo 'Building Spring Boot application...'
                bat '.\\mvnw.cmd -B clean package'
            }
        }

        stage('Build Result') {
            steps {
                echo 'Build completed successfully!'
                archiveArtifacts artifacts: 'target/*.jar',
                                 fingerprint: true
            }
        }
            
        stage('Build Container Image') {
            steps {
                buildPodmanImage()
            }
        }
    
        stage('Deploy to Podman') {
            steps {
                echo 'Deploying User Service to Podman...'

                bat '''
                    set "APPDATA=C:\\Users\\rinst\\AppData\\Roaming"
                    set "PATH=C:\\Users\\rinst\\AppData\\Local\\Programs\\Podman;%PATH%"
                    
                    powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File ".\\deploy-podman.ps1"
                    
                    if errorlevel 1 exit /b %ERRORLEVEL%
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                echo 'Checking User Service health...'

                bat '''
                    powershell.exe -NoProfile -NonInteractive -Command "$url='http://localhost:8082/actuator/health'; for ($i=0; $i -lt 30; $i++) { try { $response=Invoke-RestMethod -Uri $url -TimeoutSec 5; if ($response.status -eq 'UP') { Write-Host 'User Service is HEALTHY'; exit 0 } } catch { Write-Host 'Waiting for User Service...' }; Start-Sleep -Seconds 2 }; Write-Error 'User Service health check failed'; exit 1"
                '''
            }
        }
    }
}
