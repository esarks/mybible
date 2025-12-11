@echo off
REM ============================================================================
REM MyBible - Setup Cloud SQL Database
REM ============================================================================
REM This script connects to Cloud SQL and runs the schema
REM
REM Prerequisites:
REM   1. Google Cloud SDK (gcloud) installed
REM   2. Cloud SQL Auth Proxy downloaded (optional for local)
REM ============================================================================

echo.
echo ============================================================================
echo MyBible - Cloud SQL Database Setup
echo ============================================================================
echo.

set PROJECT_ID=mybible-480818
set INSTANCE_NAME=mybible-db
set DATABASE=mybible_db
set DB_USER=mybible_user

echo Cloud SQL Instance: %PROJECT_ID%:%INSTANCE_NAME%
echo Database: %DATABASE%
echo User: %DB_USER%
echo.

echo ============================================================================
echo Option 1: Use Cloud Shell (Recommended)
echo ============================================================================
echo.
echo 1. Open Google Cloud Console: https://console.cloud.google.com
echo 2. Click the Cloud Shell icon (top right)
echo 3. Run this command:
echo.
echo    gcloud sql connect %INSTANCE_NAME% --user=%DB_USER% --database=%DATABASE%
echo.
echo 4. When prompted, enter the password for %DB_USER%
echo 5. Copy and paste the SQL below into the psql prompt:
echo.
echo ============================================================================
echo SQL SCHEMA TO RUN:
echo ============================================================================
echo.
type "%~dp0..\data\MyBible_PostgreSQL.sql"
echo.
echo ============================================================================
echo.
echo After running the SQL, type \q to exit psql
echo.
echo ============================================================================
echo Option 2: Use Cloud SQL Auth Proxy (Local)
echo ============================================================================
echo.
echo 1. Download Cloud SQL Auth Proxy:
echo    https://cloud.google.com/sql/docs/postgres/connect-auth-proxy
echo.
echo 2. Start the proxy:
echo    cloud-sql-proxy %PROJECT_ID%:us-central1:%INSTANCE_NAME%
echo.
echo 3. Connect with psql:
echo    psql -h localhost -U %DB_USER% -d %DATABASE%
echo.
echo 4. Run the SQL schema from: %~dp0..\data\MyBible_PostgreSQL.sql
echo.
pause
