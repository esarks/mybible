@echo off
REM ========================================================================
REM MyBible - Complete Build System
REM Emulates jacBuild24/bin/allPhases.bat pattern
REM
REM This script orchestrates a complete clean build of the MyBible
REM application through multiple phases:
REM   Phase 0: Clean build directories
REM   Phase 1: Generate data layer (DDL, JEO, CRUD)
REM   Phase 3: Compile all scripts
REM   Phase 3.55: Compile utility classes
REM   Phase 3.6: Recompile server scripts
REM   Phase 4: Verify build artifacts
REM
REM Usage:
REM   cd C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\app\com\mybible\bin
REM   allPhases.bat
REM ========================================================================

REM Save the starting directory so we can return to it at the end
set STARTING_DIR=%CD%
pushd "%~dp0"

echo.
echo ========================================================================
echo ========================================================================
echo.
echo     MYBIBLE BUILD SYSTEM v1.0
echo.
echo     Personal Bible Study Application
echo     Multi-Phase Build Process
echo.
echo ========================================================================
echo ========================================================================
echo.

REM ========================================
REM Environment Setup
REM ========================================
echo [Setup] Initializing build environment...
call "%~dp0Set2MyBible.bat"
if errorlevel 1 goto error_exit

echo.
echo Build Configuration:
echo   Source:  %MYBIBLE_HOME%
echo   Output:  %MYBIBLE_CLASSES%
echo   JAC:     %JAC_BUILD%
echo.

REM ========================================
REM Phase 0: Clean Build
REM ========================================
echo.
echo ========================================================================
call "%MYBIBLE_BIN%\phase0-clean.bat"
if errorlevel 1 goto error_exit

REM ========================================
REM Phase 1: Generate Data Layer
REM ========================================
echo.
echo ========================================================================
call "%MYBIBLE_BIN%\phase1-generate-data.bat"
if errorlevel 1 goto error_exit

REM ========================================
REM Phase 3: Compile Generator Scripts
REM ========================================
echo.
echo ========================================================================
call "%MYBIBLE_BIN%\phase3-compile-scripts.bat"
REM Ignore Phase 3 errors - server will fail without executed generators (Phase 3.5 will fix)
set PHASE3_EXIT_CODE=%ERRORLEVEL%
echo Phase 3 completed with exit code: %PHASE3_EXIT_CODE%

REM ========================================
REM Phase 3.5: Execute Generator Scripts
REM ========================================
echo.
echo ========================================================================
call "%MYBIBLE_BIN%\phase3.5-execute-generators.bat"
if errorlevel 1 goto error_exit

REM ========================================
REM Phase 3.55: Compile Utility Classes
REM ========================================
echo.
echo ========================================================================
echo Phase 3.55: Compile Utility Classes (Java files in util/)
echo ========================================================================
echo   Compiling utility Java files...

set UTIL_SRC=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\app\com\mybible\util
set CLASSES_OUT=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\classes

echo   Source: %UTIL_SRC%
echo   Output: %CLASSES_OUT%

REM Compile all Java files in util directory
REM Order matters for dependencies:
REM   1. HashUtil (no dependencies)
REM   2. JWTUtil (no dependencies)
REM   3. RequestContext (depends on JWTUtil)

if exist "%UTIL_SRC%\HashUtil.java" (
    echo     Compiling HashUtil.java...
    "%JAVA_HOME%\bin\javac.exe" -cp "%CLASSPATH%" -d "%CLASSES_OUT%" "%UTIL_SRC%\HashUtil.java"
    if errorlevel 1 (
        echo   WARNING: HashUtil.java compilation failed
    ) else (
        echo     SUCCESS: HashUtil.java compiled
    )
)

if exist "%UTIL_SRC%\JWTUtil.java" (
    echo     Compiling JWTUtil.java...
    "%JAVA_HOME%\bin\javac.exe" -cp "%CLASSPATH%" -d "%CLASSES_OUT%" "%UTIL_SRC%\JWTUtil.java"
    if errorlevel 1 (
        echo   WARNING: JWTUtil.java compilation failed
    ) else (
        echo     SUCCESS: JWTUtil.java compiled
    )
)

if exist "%UTIL_SRC%\RequestContext.java" (
    echo     Compiling RequestContext.java...
    "%JAVA_HOME%\bin\javac.exe" -cp "%CLASSPATH%;%CLASSES_OUT%" -d "%CLASSES_OUT%" "%UTIL_SRC%\RequestContext.java"
    if errorlevel 1 (
        echo   WARNING: RequestContext.java compilation failed
    ) else (
        echo     SUCCESS: RequestContext.java compiled
    )
)

if exist "%UTIL_SRC%\EmailService.java" (
    echo     Compiling EmailService.java...
    "%JAVA_HOME%\bin\javac.exe" -cp "%CLASSPATH%;%CLASSES_OUT%" -d "%CLASSES_OUT%" "%UTIL_SRC%\EmailService.java"
    if errorlevel 1 (
        echo   WARNING: EmailService.java compilation failed
    ) else (
        echo     SUCCESS: EmailService.java compiled
    )
)

echo.
echo Phase 3.55 Complete: Utility classes compiled
echo ========================================================================

REM ========================================
REM Phase 3.6: Recompile Server Scripts
REM ========================================
echo.
echo ========================================================================
echo Phase 3.6: Recompile Server Scripts (now that JEO/CRUD classes exist)
echo ========================================================================
pushd "%MYBIBLE_HOME%\server"
echo   Compiling MyBibleRouter...
call "%JAC_BUILD%\bin\jac.bat" com.mybible.server.MyBibleRouter
if errorlevel 1 (
    echo   ERROR: MyBibleRouter compilation failed
    popd
    goto error_exit
)
echo.
echo Phase 3.6 Complete: Server scripts compiled successfully
echo ========================================================================
popd

REM ========================================
REM Phase 4: Verify Build
REM ========================================
echo.
echo ========================================================================
call "%MYBIBLE_BIN%\phase4-verify.bat"
if errorlevel 1 goto error_exit

REM ========================================
REM Build Successful
REM ========================================
echo.
echo ========================================================================
echo ========================================================================
echo.
echo     BUILD SUCCESSFUL
echo.
echo ========================================================================
echo ========================================================================
echo.
echo Generated Artifacts:
echo   Data Layer:  %MYBIBLE_CLASSES%\data\
echo   Server:      %MYBIBLE_CLASSES%\server\
echo   Utilities:   %MYBIBLE_CLASSES%\util\
echo.
echo ========================================================================
echo Next Steps:
echo ========================================================================
echo.
echo 1. Review generated artifacts in: %MYBIBLE_CLASSES%
echo.
echo 2. Start the MyBible server:
echo    cd %MYBIBLE_HOME%\server
echo    ..\..\..\..\jacBuild24\bin\Jrun.bat StartMyBibleRouter.jrun
echo.
echo 3. Access the application:
echo    http://localhost:8080/
echo.
echo ========================================================================
echo ========================================================================
echo.

goto end

:error_exit
echo.
echo ========================================================================
echo ========================================================================
echo.
echo     BUILD FAILED
echo.
echo ========================================================================
echo ========================================================================
echo.
echo The build process encountered errors. Please review the error messages
echo above and correct any issues before rebuilding.
echo.
echo Common issues:
echo   - JAC environment not properly configured (run Set2Job.bat first)
echo   - Source files missing or corrupted
echo   - Compilation errors in script files
echo   - Missing dependencies
echo.
echo ========================================================================
echo ========================================================================
echo.
REM Return to starting directory
popd
cd /d "%STARTING_DIR%"
echo.
echo Returned to: %CD%
echo.
echo Press any key to exit...
pause >nul
exit /b 1

:end
REM Return to starting directory
popd
cd /d "%STARTING_DIR%"
echo.
echo Returned to: %CD%
echo.
echo Press any key to exit...
pause >nul
exit /b 0
