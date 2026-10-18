@echo off
REM Starts LearnPilot (API + web app) on http://localhost:8080
REM Optional: set JWT_SECRET=...   set GEMINI_API_KEY=...   (before running this file)
cd /d "%~dp0"
mvn spring-boot:run
pause
