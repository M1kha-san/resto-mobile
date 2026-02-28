@echo off
echo ========================================
echo   Push RestoApp ke GitHub
echo ========================================
echo.

cd /d "D:\Projek\MOBILE\ANDROIDAPP\restoAppMain"

echo [1/6] Initializing git...
git init

echo [2/6] Adding all files...
git add .

echo [3/6] Creating commit...
git commit -m "Initial commit - RestoApp Mobile"

echo [4/6] Setting branch to main...
git branch -M main

echo [5/6] Adding remote...
git remote add origin https://github.com/M1kha-san/resto-mobile.git 2>nul
git remote set-url origin https://github.com/M1kha-san/resto-mobile.git

echo [6/6] Pushing to GitHub...
git push -u origin main

echo.
echo ========================================
echo   Selesai! Cek repo di:
echo   https://github.com/M1kha-san/resto-mobile
echo ========================================
pause

