@echo off

java -cp "%~dp0app\*;%~dp0lib\*" com.github.zeldigas.text2confl.cli.MainKt %*
