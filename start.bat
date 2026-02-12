@echo off
echo Starting UTEM Core...
echo.

echo [1/2] Starting Spring Boot backend on port 8080...
start "UTEM-Backend" cmd /c "cd /d %~dp0 && mvnw.cmd spring-boot:run"

echo [2/2] Starting React frontend on port 5173...
start "UTEM-Frontend" cmd /c "cd /d %~dp0frontend && npm run dev"

echo.
echo Both services starting:
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:5173
echo.
echo Close the opened windows to stop individual services,
echo or run stop.bat to stop both.
