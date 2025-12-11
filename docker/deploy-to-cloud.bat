@echo off
REM ============================================================================
REM MyBible - Deploy to Google Cloud
REM ============================================================================
REM Prerequisites:
REM   1. Google Cloud SDK (gcloud) installed and configured
REM   2. Docker installed
REM   3. Authenticated to gcloud: gcloud auth login
REM   4. Project set: gcloud config set project mybible-480818
REM ============================================================================

setlocal EnableDelayedExpansion

echo.
echo ============================================================================
echo MyBible - Google Cloud Deployment
echo ============================================================================
echo.

REM Configuration
set PROJECT_ID=mybible-480818
set REGION=us-central1
set IMAGE_NAME=mybible-app
set IMAGE_TAG=v1
set CLOUD_SQL_INSTANCE=mybible-480818:us-central1:mybible-db
set SERVICE_NAME=mybible-app

REM Navigate to jac2024 root (Docker context)
cd /d "%~dp0..\..\..\.."
set JAC_ROOT=%CD%
echo JAC Root: %JAC_ROOT%

REM ============================================================================
REM Step 1: Verify gcloud authentication
REM ============================================================================
echo.
echo [Step 1] Verifying Google Cloud authentication...
gcloud auth print-identity-token >nul 2>&1
if errorlevel 1 (
    echo ERROR: Not authenticated to Google Cloud
    echo Please run: gcloud auth login
    goto error_exit
)
echo   OK: Authenticated to Google Cloud

REM Set project
gcloud config set project %PROJECT_ID% >nul 2>&1
echo   OK: Project set to %PROJECT_ID%

REM ============================================================================
REM Step 2: Configure Docker for GCR
REM ============================================================================
echo.
echo [Step 2] Configuring Docker for Google Container Registry...
gcloud auth configure-docker --quiet
if errorlevel 1 (
    echo ERROR: Failed to configure Docker for GCR
    goto error_exit
)
echo   OK: Docker configured for GCR

REM ============================================================================
REM Step 3: Build Docker image
REM ============================================================================
echo.
echo [Step 3] Building Docker image...
echo   Context: %JAC_ROOT%
echo   Dockerfile: app/com/mybible/docker/Dockerfile
echo   Image: gcr.io/%PROJECT_ID%/%IMAGE_NAME%:%IMAGE_TAG%
echo.

docker build -t gcr.io/%PROJECT_ID%/%IMAGE_NAME%:%IMAGE_TAG% -f app/com/mybible/docker/Dockerfile .
if errorlevel 1 (
    echo ERROR: Docker build failed
    goto error_exit
)
echo   OK: Docker image built successfully

REM ============================================================================
REM Step 4: Push to Google Container Registry
REM ============================================================================
echo.
echo [Step 4] Pushing image to Google Container Registry...
docker push gcr.io/%PROJECT_ID%/%IMAGE_NAME%:%IMAGE_TAG%
if errorlevel 1 (
    echo ERROR: Failed to push image to GCR
    goto error_exit
)
echo   OK: Image pushed to GCR

REM ============================================================================
REM Step 5: Prompt for secrets
REM ============================================================================
echo.
echo [Step 5] Configuration for deployment...
echo.
set /p DB_PASSWORD="Enter database password for mybible_user: "
set /p JWT_SECRET="Enter JWT secret (min 32 characters): "

REM ============================================================================
REM Step 6: Deploy to Cloud Run
REM ============================================================================
echo.
echo [Step 6] Deploying to Cloud Run...
echo   Service: %SERVICE_NAME%
echo   Region: %REGION%
echo   Image: gcr.io/%PROJECT_ID%/%IMAGE_NAME%:%IMAGE_TAG%
echo.

gcloud run deploy %SERVICE_NAME% ^
    --image gcr.io/%PROJECT_ID%/%IMAGE_NAME%:%IMAGE_TAG% ^
    --platform managed ^
    --region %REGION% ^
    --allow-unauthenticated ^
    --add-cloudsql-instances %CLOUD_SQL_INSTANCE% ^
    --set-env-vars "CLOUD_SQL_CONNECTION=%CLOUD_SQL_INSTANCE%" ^
    --set-env-vars "DB_HOST=/cloudsql/%CLOUD_SQL_INSTANCE%" ^
    --set-env-vars "DB_PORT=5432" ^
    --set-env-vars "DB_NAME=mybible_db" ^
    --set-env-vars "DB_USER=mybible_user" ^
    --set-env-vars "DB_PASSWORD=%DB_PASSWORD%" ^
    --set-env-vars "JWT_SECRET=%JWT_SECRET%" ^
    --memory 512Mi ^
    --cpu 1 ^
    --port 8080 ^
    --min-instances 0 ^
    --max-instances 2

if errorlevel 1 (
    echo ERROR: Cloud Run deployment failed
    goto error_exit
)

REM ============================================================================
REM Step 7: Get service URL
REM ============================================================================
echo.
echo [Step 7] Getting service URL...
for /f "tokens=*" %%i in ('gcloud run services describe %SERVICE_NAME% --platform managed --region %REGION% --format "value(status.url)"') do set SERVICE_URL=%%i

echo.
echo ============================================================================
echo DEPLOYMENT SUCCESSFUL
echo ============================================================================
echo.
echo Service URL: %SERVICE_URL%
echo.
echo Test endpoints:
echo   Health:    %SERVICE_URL%/health
echo   Home:      %SERVICE_URL%/
echo   Login:     %SERVICE_URL%/login
echo   Register:  %SERVICE_URL%/register
echo.
echo ============================================================================
goto end

:error_exit
echo.
echo ============================================================================
echo DEPLOYMENT FAILED
echo ============================================================================
echo.
echo Please review the error messages above and try again.
echo.
endlocal
exit /b 1

:end
endlocal
exit /b 0
