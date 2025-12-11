@echo off
REM ========================================================================
REM MyBible Environment Setup
REM Sets environment variables for MyBible build system
REM ========================================================================

echo ========================================================================
echo MyBible Environment Setup
echo ========================================================================

REM First, call JAC's Set2Job to establish JAC environment
call "%~dp0..\..\..\..\jacBuild24\bin\Set2Job.bat"

REM MyBible specific paths
set MYBIBLE_HOME=%~dp0..
set MYBIBLE_BIN=%MYBIBLE_HOME%\bin

REM JAC_HOME is jac2024 (parent of jacBuild24)
set JAC_HOME=C:\Users\ptm\OneDrive\Documents\GitHub\ArchitectsCompanion\jac2024

REM Classes go to jac2024\classes (NOT jacBuild24\classes)
set MYBIBLE_CLASSES=%JAC_HOME%\classes\com\mybible

REM Override JAC_SCRIPTS to point to MyBible app directory
set JAC_SCRIPTS=%JAC_HOME%\app

REM Add jac2024\classes to CLASSPATH so MyBible classes can be found
set CLASSPATH=%JAC_HOME%\classes;%CLASSPATH%

REM Add mail.jar and activation.jar for JavaMail support (if needed)
set CLASSPATH=%JAC_BUILD%\lib\soap\mail.jar;%JAC_BUILD%\lib\soap\activation.jar;%CLASSPATH%

REM Create base directories if they don't exist
if not exist "%JAC_HOME%\classes" mkdir "%JAC_HOME%\classes"
if not exist "%JAC_HOME%\classes\com" mkdir "%JAC_HOME%\classes\com"
if not exist "%MYBIBLE_CLASSES%" mkdir "%MYBIBLE_CLASSES%"
if not exist "%MYBIBLE_CLASSES%\data" mkdir "%MYBIBLE_CLASSES%\data"
if not exist "%MYBIBLE_CLASSES%\server" mkdir "%MYBIBLE_CLASSES%\server"
if not exist "%MYBIBLE_CLASSES%\util" mkdir "%MYBIBLE_CLASSES%\util"

echo.
echo Environment Variables Set:
echo   MYBIBLE_HOME    = %MYBIBLE_HOME%
echo   MYBIBLE_BIN     = %MYBIBLE_BIN%
echo   MYBIBLE_CLASSES = %MYBIBLE_CLASSES%
echo   JAC_HOME        = %JAC_HOME%
echo   JAC_BUILD       = %JAC_BUILD%
echo   JAC_SCRIPTS     = %JAC_SCRIPTS%
echo   JAVA_HOME       = %JAVA_HOME%
echo ========================================================================
