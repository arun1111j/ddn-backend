@echo off
echo ========================================
echo  Spring Boot Project Compilation
echo ========================================
echo.

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found in PATH!
    echo Please install JDK 17 or add Java to your PATH
    pause
    exit /b 1
)

echo [INFO] Java found!
java -version
echo.

REM Try to find JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo [WARNING] JAVA_HOME not set, attempting to auto-detect...
    for /f "tokens=*" %%i in ('where java') do set JAVA_BIN=%%i
    for %%i in ("%JAVA_BIN%") do set JAVA_HOME=%%~dpi..
    echo [INFO] Auto-detected JAVA_HOME: %JAVA_HOME%
)

echo.
echo ========================================
echo  Starting Compilation...
echo ========================================
echo.

mvn clean compile

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo  ✓ COMPILATION SUCCESSFUL!
    echo ========================================
    echo.
    echo You can now:
    echo   1. Run: mvn spring-boot:run
    echo   2. Package: mvn clean package
    echo.
) else (
    echo.
    echo ========================================
    echo  ✗ COMPILATION FAILED
    echo ========================================
    echo Check the errors above
    echo.
)

pause
