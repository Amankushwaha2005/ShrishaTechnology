@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo.
echo  ========================================
echo   Shrisha Technology — Java + PostgreSQL
echo  ========================================
echo.
echo  Open http://127.0.0.1:3000 when Tomcat starts.
echo.

if exist ".tools\apache-maven-3.9.9\bin\mvn.cmd" (
  ".tools\apache-maven-3.9.9\bin\mvn.cmd" -f backend\pom.xml spring-boot:run
) else (
  mvn -f backend\pom.xml spring-boot:run
)
if errorlevel 1 (
  echo.
  echo  Start failed. Install Java 21+ and Maven, and set DATABASE_URL in .env
  echo.
)
pause
