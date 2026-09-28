@echo off
setlocal

set JAVA_MAJOR=0
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
  for /f "delims=.-" %%m in ("%%~v") do set JAVA_MAJOR=%%m
)
set EXTRA_OPTS=--enable-native-access=ALL-UNNAMED
if %JAVA_MAJOR% GTR 23 set EXTRA_OPTS=%EXTRA_OPTS% --sun-misc-unsafe-memory-access=allow

java -cp "%~dp0app\*;%~dp0lib\*" %EXTRA_OPTS% --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED com.github.zeldigas.text2confl.cli.MainKt %*
