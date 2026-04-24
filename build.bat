@echo off
setlocal

set DO_CLEAN=false
set DO_INSTALL=false

:parse_args
if "%~1"=="" goto end_parse_args
if /i "%~1"=="clean" set DO_CLEAN=true
if /i "%~1"=="install" set DO_INSTALL=true
shift
goto parse_args
:end_parse_args

if "%DO_CLEAN%"=="true" (
    echo Stopping Gradle Daemons...
    call gradlew.bat --stop

    echo Cleaning build artifacts and temporary files...
    if exist .gradle rd /s /q .gradle
    if exist build rd /s /q build
    if exist app\build rd /s /q app\build
    if exist app\release rd /s /q app\release
    call gradlew.bat clean
)

echo Building Release APK...
call gradlew.bat :app:assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

set APK_DIR=app\build\outputs\apk
set SIGNED_SRC=%APK_DIR%\release\app-release.apk
set SIGNED_DEST=%APK_DIR%\release\app-release-signed.apk

if exist "%SIGNED_SRC%" (
    copy /y "%SIGNED_SRC%" "%SIGNED_DEST%"
)

echo.
echo ------------------------------------------------
echo Build complete!

if "%DO_INSTALL%"=="true" (
    if exist "%SIGNED_DEST%" (
        echo Installing %SIGNED_DEST% to device...
        adb install -r "%SIGNED_DEST%"
    ) else (
        echo Error: Signed APK not found at %SIGNED_DEST%
        exit /b 1
    )
)

endlocal
