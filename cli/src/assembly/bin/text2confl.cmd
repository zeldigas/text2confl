@echo off

java -cp "%~dp0app\*;%~dp0lib\*" --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED com.github.zeldigas.text2confl.cli.MainKt %*
