@echo off
setlocal

rem ---------------------------------------------------------------
rem  启动 Controlify 1.20.1 Forge 开发客户端
rem  首次启动会下载资源与原生库，耗时较长。
rem ---------------------------------------------------------------

cd /d "%~dp0"

echo [1/3] 检查 Java 版本...
java -version 2>&1 | findstr /R /C:"version \"21" >nul
if errorlevel 1 (
    echo.
    echo [错误] 未检测到 JDK 21。详见 README-WINDOWS.md 第 1 节。
    echo.
    pause
    exit /b 1
)
echo       OK

echo [2/3] 设置构建目标为 1.20.1-forge ...
> "versions\current" echo 1.20.1-forge

echo [3/3] 启动客户端...
echo.
call gradlew.bat runClientActive %*
set RUN_RESULT=%errorlevel%

echo.
if %RUN_RESULT% neq 0 (
    echo [失败] 客户端退出码 %RUN_RESULT%。
    echo        首次运行请确认网络可用（需下载 Minecraft 资源与 SDL 原生库）。
)

echo.
pause
exit /b %RUN_RESULT%
