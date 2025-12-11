@echo off
REM ========================================================================
REM Phase 4: Verify Build Artifacts
REM Checks that all expected artifacts were created
REM ========================================================================

echo ========================================================================
echo Phase 4: Verify Build Artifacts
echo ========================================================================

set VERIFY_ERRORS=0

REM Check data layer classes exist
echo.
echo [4.1] Verifying data layer classes...

if exist "%MYBIBLE_CLASSES%\data\AUTH_USERS.class" (
    echo   OK: AUTH_USERS.class
) else (
    echo   MISSING: AUTH_USERS.class
    set /a VERIFY_ERRORS+=1
)

if exist "%MYBIBLE_CLASSES%\data\AUTH_USERSCrud.class" (
    echo   OK: AUTH_USERSCrud.class
) else (
    echo   MISSING: AUTH_USERSCrud.class
    set /a VERIFY_ERRORS+=1
)

REM Check server classes
echo.
echo [4.2] Verifying server classes...

if exist "%MYBIBLE_CLASSES%\server\MyBibleRouter.class" (
    echo   OK: MyBibleRouter.class
) else (
    echo   MISSING: MyBibleRouter.class
    set /a VERIFY_ERRORS+=1
)

REM Check util classes
echo.
echo [4.3] Verifying utility classes...

set UTIL_CLASSES=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024\classes\com\mybible\util

if exist "%UTIL_CLASSES%\HashUtil.class" (
    echo   OK: HashUtil.class
) else (
    echo   MISSING: HashUtil.class
    set /a VERIFY_ERRORS+=1
)

if exist "%UTIL_CLASSES%\JWTUtil.class" (
    echo   OK: JWTUtil.class
) else (
    echo   MISSING: JWTUtil.class
    set /a VERIFY_ERRORS+=1
)

if exist "%UTIL_CLASSES%\RequestContext.class" (
    echo   OK: RequestContext.class
) else (
    echo   MISSING: RequestContext.class
    set /a VERIFY_ERRORS+=1
)

REM Summary
echo.
echo ========================================================================
if %VERIFY_ERRORS% GTR 0 (
    echo Phase 4: FAILED with %VERIFY_ERRORS% missing artifacts
    echo ========================================================================
    exit /b 1
) else (
    echo Phase 4: SUCCESS - All artifacts verified
    echo ========================================================================
    exit /b 0
)
