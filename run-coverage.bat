@echo off
set "JAVA_HOME=C:\Users\ZBARTIN\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo JAVA_HOME=%JAVA_HOME%
echo.
call gradlew.bat test jacocoTestReport --no-daemon --console=plain
