@echo off
setlocal EnableDelayedExpansion
REM ========================================================================
REM Phase 3.5: Execute Generator Scripts
REM Executes compiled .script files to generate actual JEO/CRUD classes
REM ========================================================================
REM
REM CRITICAL: This phase executes the ScriptHelper wrappers created in Phase 3
REM          When executed, they output Java source code via iOutputManager.println()
REM          JAC automatically compiles the generated .java to final .class files
REM          with actual JEO getter/setter and CRUD methods
REM
REM ========================================================================

echo ========================================================================
echo Phase 3.5: Execute Generator Scripts
echo ========================================================================

echo.
echo Executing JEO/CRUD generator scripts...
echo This will generate actual Java source and compile to .class files
echo.

set TOTAL_EXECUTED=0
set TOTAL_ERRORS=0

REM ========================================
REM Execute Data Layer Generator Scripts
REM ========================================

REM Hardcode JAC_BUILD to ensure it's always available (even in for loops)
set JAC_BUILD=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\jacBuild24

REM Debug: Show current JAC_BUILD value
echo DEBUG: JAC_BUILD = %JAC_BUILD%

REM Override JAC_WORK and JAC_SCRIPTS for MyBible application
set JAC_WORK=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\classes
set JAC_SCRIPTS=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\app

REM Also set JAC_HOME explicitly for Job.bat
set JAC_HOME=%JAC_BUILD%

REM Ensure application classes are in CLASSPATH
set CLASSPATH=%JAC_WORK%;%CLASSPATH%

REM Debug: Show JAC_WORK
echo DEBUG: JAC_WORK = %JAC_WORK%

REM Change to data directory
pushd "%MYBIBLE_HOME%\data"

echo [3.5.1] Executing JEO generator scripts...

REM Execute each JEO script (only AUTH_USERS for MyBible)
REM Use jac.bat with execute method - jac.bat has hardcoded paths that work reliably
for %%f in (AUTH_USERS.script) do (
    if exist "%%f" (
        echo   Executing %%~nf...
        call "!JAC_BUILD!\bin\jac.bat" com.mybible.data.%%~nf execute
        if errorlevel 1 (
            echo   ERROR executing %%~nf
            set /a TOTAL_ERRORS+=1
        ) else (
            set /a TOTAL_EXECUTED+=1
        )
    ) else (
        echo   WARNING: %%f not found, checking for .new file...
        if exist "%%~nf.new" (
            echo   Renaming %%~nf.new to %%f...
            copy /Y "%%~nf.new" "%%f" >nul
            echo   Executing %%~nf...
            call "!JAC_BUILD!\bin\jac.bat" com.mybible.data.%%~nf execute
            if errorlevel 1 (
                echo   ERROR executing %%~nf
                set /a TOTAL_ERRORS+=1
            ) else (
                set /a TOTAL_EXECUTED+=1
            )
        )
    )
)

echo.
echo [3.5.2] Executing CRUD generator scripts...

REM Execute each CRUD script
for %%f in (AUTH_USERSCrud.script) do (
    if exist "%%f" (
        echo   Executing %%~nf...
        call "!JAC_BUILD!\bin\jac.bat" com.mybible.data.%%~nf execute
        if errorlevel 1 (
            echo   ERROR executing %%~nf
            set /a TOTAL_ERRORS+=1
        ) else (
            set /a TOTAL_EXECUTED+=1
        )
    ) else (
        echo   WARNING: %%f not found, checking for .new file...
        if exist "%%~nf.new" (
            echo   Renaming %%~nf.new to %%f...
            copy /Y "%%~nf.new" "%%f" >nul
            echo   Executing %%~nf...
            call "!JAC_BUILD!\bin\jac.bat" com.mybible.data.%%~nf execute
            if errorlevel 1 (
                echo   ERROR executing %%~nf
                set /a TOTAL_ERRORS+=1
            ) else (
                set /a TOTAL_EXECUTED+=1
            )
        )
    )
)

popd

REM ========================================
REM Copy Generated JEO/CRUD from jacBuild24 to jac2024
REM ========================================
REM JAC's jac.bat outputs generated classes to jacBuild24/classes
REM but the build expects them in jac2024/classes
REM Copy the actual JEO/CRUD .java and .class files to the correct location

echo.
echo [3.5.3] Copying generated JEO/CRUD files to application classes directory...

set SRC_DIR=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\jacBuild24\classes\com\mybible\data
set DST_DIR=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\classes\com\mybible\data

REM Create destination directory if it doesn't exist
if not exist "%DST_DIR%" mkdir "%DST_DIR%"

REM Copy JEO files (AUTH_USERS only for MyBible)
for %%f in (AUTH_USERS) do (
    if exist "%SRC_DIR%\%%f.java" (
        echo   Copying %%f.java...
        copy /Y "%SRC_DIR%\%%f.java" "%DST_DIR%\" >nul
    )
    if exist "%SRC_DIR%\%%f.class" (
        echo   Copying %%f.class...
        copy /Y "%SRC_DIR%\%%f.class" "%DST_DIR%\" >nul
    )
)

REM Copy CRUD files
for %%f in (AUTH_USERSCrud) do (
    if exist "%SRC_DIR%\%%f.java" (
        echo   Copying %%f.java...
        copy /Y "%SRC_DIR%\%%f.java" "%DST_DIR%\" >nul
    )
    if exist "%SRC_DIR%\%%f.class" (
        echo   Copying %%f.class...
        copy /Y "%SRC_DIR%\%%f.class" "%DST_DIR%\" >nul
    )
)

echo   JEO/CRUD files copied successfully

REM ========================================
REM Compile any CRUD .java files that don't have .class files
REM ========================================
echo.
echo [3.5.4] Compiling CRUD Service classes that need compilation...

set JAVA_HOME=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\jacBuild24\jdk-24
set COMPILE_CP=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\jacBuild24\phase1Classes;C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\jacBuild24\classes;C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\classes;C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\jacBuild24\lib\jetty\javax.servlet.jar

pushd "%SRC_DIR%"
for %%f in (AUTH_USERSCrud) do (
    if exist "%%f.java" (
        if not exist "%%f.class" (
            echo   Compiling %%f.java...
            "%JAVA_HOME%\bin\javac.exe" -classpath "%COMPILE_CP%" -d "%SRC_DIR%\..\..\..\..\.." "%%f.java" >nul 2>&1
            if exist "%SRC_DIR%\..\..\..\..\..\com\mybible\data\%%f.class" (
                echo   Copying compiled %%f.class to classes...
                copy /Y "%SRC_DIR%\..\..\..\..\..\com\mybible\data\%%f.class" "%DST_DIR%\" >nul
            )
        )
    )
)
popd

echo   CRUD Service compilation complete

REM ========================================
REM Execution Summary
REM ========================================
echo.
echo Phase 3.5 Results:
echo   Scripts Executed: !TOTAL_EXECUTED!
echo   Execution Errors: !TOTAL_ERRORS!

if !TOTAL_ERRORS! GTR 0 (
    echo.
    echo ERROR: Phase 3.5 completed with !TOTAL_ERRORS! errors
    echo ========================================================================
    endlocal
    exit /b 1
)

if !TOTAL_EXECUTED! EQU 0 (
    echo.
    echo WARNING: No scripts were executed
    echo ========================================================================
    endlocal
    exit /b 1
)

echo.
echo Phase 3.5 Complete: Generator scripts executed successfully
echo ========================================================================
endlocal
