@echo off
setlocal

rem ---------------------------------------------------------------
rem  构建 Controlify 1.20.1 Forge 版本
rem  产物: build\finalJars\controlify-2.0.3+1.20.1-forge.jar
rem        build\finalJars\controlify-2.0.3+1.20.1-forge-offline.jar
rem ---------------------------------------------------------------

cd /d "%~dp0"

echo [1/3] 检查 Java 版本...
java -version 2>&1 | findstr /R /C:"version \"21" >nul
if errorlevel 1 (
    echo.
    echo [错误] 未检测到 JDK 21。
    echo        请安装 JDK 21 并设置 JAVA_HOME 后重试。
    echo        详见 README-DEV.md 第 1 节。
    echo.
    pause
    exit /b 1
)
echo       OK

echo [2/3] 设置构建目标为 1.20.1-forge ...
> "versions\current" echo 1.20.1-forge

echo [3/3] 开始构建（首次构建较慢，请耐心等待）...
echo.
call gradlew.bat chiseledBuildAndCollect %*
set BUILD_RESULT=%errorlevel%

echo.
if %BUILD_RESULT% neq 0 (
    echo [失败] 构建未通过，退出码 %BUILD_RESULT%。
    echo        常见原因见 README-DEV.md 第 4 节。
) else (
    echo [成功] 产物位于 build\finalJars\
    dir /b "build\finalJars\*.jar" 2>nul
)

echo.
pause
exit /b %BUILD_RESULT%
