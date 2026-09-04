@echo off
chcp 65001 >nul
cd /d "%~dp0"
if exist ".tools\apache-maven-3.9.9\bin\mvn.cmd" (
  call ".tools\apache-maven-3.9.9\bin\mvn.cmd" -f backend\pom.xml spring-boot:run
) else (
  call mvn -f backend\pom.xml spring-boot:run
)
pause
