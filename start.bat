@echo off
echo Starting UTEM Core...
echo.

REM Check Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH.
    echo Please install Java 17 or later from https://adoptium.net/
    pause
    exit /b 1
)

echo Dashboard will open at: http://localhost:8080
echo Press Ctrl+C to stop.
echo.

REM Open browser after 5 seconds (gives Spring Boot time to start)
start "" cmd /c "timeout /t 5 >nul && start http://localhost:8080"

java -jar "%~dp0target\utem-core-0.1.0.jar"
