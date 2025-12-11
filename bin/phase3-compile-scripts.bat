@echo off
setlocal EnableDelayedExpansion
REM ========================================================================
REM Phase 3: Compile All Scripts
REM Compiles all .script files to .class files
REM ========================================================================

echo ========================================================================
echo Phase 3: Compile All Scripts
echo ========================================================================

set TOTAL_COMPILED=0
set TOTAL_ERRORS=0

REM ========================================
REM Compile Data Layer (JEO + CRUD)
REM ========================================
echo.
echo [3.1] Compiling data layer scripts...

REM Change to SOURCE directory where generated scripts are
pushd "%MYBIBLE_HOME%\data"

REM First, rename .new files to .script files (only for AUTH_USERS, not ACTIVITY_LEDGER)
echo [3.1.1] Renaming .new files to .script files...
for %%f in (AUTH_USERS.new AUTH_USERSCrud.new) do (
    if exist "%%f" (
        set BASENAME=%%~nf
        echo   Renaming %%f to !BASENAME!.script...
        copy /Y "%%f" "!BASENAME!.script" >nul
    )
)

REM Delete any leftover ACTIVITY_LEDGER files (not used in MyBible)
if exist "ACTIVITY_LEDGER.new" del "ACTIVITY_LEDGER.new"
if exist "ACTIVITY_LEDGERCrud.new" del "ACTIVITY_LEDGERCrud.new"
if exist "ACTIVITY_LEDGERCrud_Crud.new" del "ACTIVITY_LEDGERCrud_Crud.new"
if exist "ACTIVITY_LEDGER.script" del "ACTIVITY_LEDGER.script"
if exist "ACTIVITY_LEDGERCrud.script" del "ACTIVITY_LEDGERCrud.script"

REM Count scripts to compile (only AUTH_USERS related)
for /f %%i in ('dir /b AUTH_USERS*.script 2^>nul ^| %SystemRoot%\System32\find.exe /c /v ""') do set DATA_SCRIPTS=%%i
if not defined DATA_SCRIPTS set DATA_SCRIPTS=0

echo Found !DATA_SCRIPTS! data layer scripts to compile
echo Compiling from: %MYBIBLE_HOME%\data
echo Compiling to:   %MYBIBLE_CLASSES%\data

if !DATA_SCRIPTS! GTR 0 (
    REM Compile each AUTH_USERS script using jac.bat
    for %%f in (AUTH_USERS.script AUTH_USERSCrud.script) do (
        if exist "%%f" (
            echo   Compiling %%~nf...
            call "%JAC_BUILD%\bin\jac.bat" com.mybible.data.%%~nf
            if errorlevel 1 (
                echo   ERROR compiling %%~nf
                set /a TOTAL_ERRORS+=1
            ) else (
                set /a TOTAL_COMPILED+=1
            )
        )
    )
)

popd

REM ========================================
REM Server Layer (deferred to Phase 3.6)
REM ========================================
echo.
echo [3.2] Server layer scripts - deferred to Phase 3.6 (after JEO/CRUD generation)

REM ========================================
REM Compile Util Layer - SKIPPED
REM ========================================
echo.
echo [3.3] Utility layer scripts - SKIPPED (using .java files compiled in Phase 3.55)

REM ========================================
REM Compilation Summary
REM ========================================
echo.
echo Phase 3 Results:
echo   Scripts Compiled: !TOTAL_COMPILED!
echo   Compilation Errors: !TOTAL_ERRORS!

if !TOTAL_ERRORS! GTR 0 (
    echo.
    echo WARNING: Phase 3 completed with !TOTAL_ERRORS! errors
    echo ========================================================================
    endlocal
    exit /b 1
)

echo.
echo Phase 3 Complete: All scripts compiled successfully
echo ========================================================================
endlocal
