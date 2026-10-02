@echo off
setlocal
cd /d "%~dp0"
echo Dongu kayipsiz veri tasima araci baslatiliyor...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0MIGRATE-DONGU.ps1"
if errorlevel 1 (
  echo.
  echo ISLEM DURDU. Hata varsa eski uygulama verisi yedeklenmeden silinmez.
  pause
)
endlocal
