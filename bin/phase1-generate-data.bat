@echo off
REM ========================================================================
REM Phase 1: Generate Data Layer
REM Generates DDL, JEO, and CRUD services for all database tables
REM ========================================================================

echo ========================================================================
echo Phase 1: Generate Data Layer (DDL, JEO, CRUD)
echo ========================================================================

echo Running data generators from mybible/data...
echo   Config: %MYBIBLE_HOME%\data
echo   Output: %MYBIBLE_HOME%\data\ (source artifacts)

REM Navigate to data directory and run generators
pushd "%MYBIBLE_HOME%\data"

REM Use RunMakeAll to invoke MakeAll orchestrator (proper JAC pattern)
echo [1.1] Generating DDL, JEO, and CRUD using MakeAll...
call "%JAC_BUILD%\bin\Jrun2.bat" RunMakeAll.jrun
if errorlevel 1 (
    echo ERROR: Data generation failed!
    popd
    exit /b 1
)

popd

REM Count generated artifacts
echo.
echo Checking generated artifacts...

REM Count JEO scripts (in source directory)
for /f %%i in ('dir /b "%MYBIBLE_HOME%\data\*.script" 2^>nul ^| %SystemRoot%\System32\find.exe /c /v ""') do set JEO_COUNT=%%i
if not defined JEO_COUNT set JEO_COUNT=0

REM Count CRUD scripts (in source directory)
for /f %%i in ('dir /b "%MYBIBLE_HOME%\data\*Crud.script" 2^>nul ^| %SystemRoot%\System32\find.exe /c /v ""') do set CRUD_COUNT=%%i
if not defined CRUD_COUNT set CRUD_COUNT=0

REM Check for SQL file (in source directory)
set SQL_FILE=%MYBIBLE_HOME%\data\MyBible_PostgreSQL.sql
if exist "%SQL_FILE%" (
    set SQL_FOUND=YES
) else (
    set SQL_FOUND=NO
)

echo.
echo Phase 1 Results:
echo   JEO Scripts:  %JEO_COUNT% generated
echo   CRUD Scripts: %CRUD_COUNT% generated
echo   SQL DDL:      %SQL_FOUND%

REM Verify minimum expected artifacts (2 tables = 2 JEO + 2 CRUD = 4 scripts)
if %JEO_COUNT% LSS 2 (
    echo WARNING: Expected at least 2 JEO scripts, found %JEO_COUNT%
)

if %CRUD_COUNT% LSS 2 (
    echo WARNING: Expected at least 2 CRUD scripts, found %CRUD_COUNT%
)

echo.
echo Phase 1 Complete: Data layer generated
echo ========================================================================
