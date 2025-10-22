@echo off
rem Package the Book Writer app into a native installer or exe using jpackage.
rem This script requires JDK 14+ with jpackage available in PATH.

setlocal
set "APP_NAME=BookWriter"
set "MAIN_CLASS=app.BookWriterApp"
set "JAR=target\%APP_NAME%.jar"

if not exist target\classes (
  echo target\classes not found. Build the project first (mvn package or run.bat).
  exit /b 1
)

if not exist %JAR% (
  echo Creating jar with manifest...
  if exist manifest.txt del /q manifest.txt
  echo Main-Class: %MAIN_CLASS%> manifest.txt
  jar --create --file=%JAR% -C target\classes . -m manifest.txt
  if exist manifest.txt del /q manifest.txt
)

where jpackage >nul 2>&1
if errorlevel 1 (
  echo jpackage not found on PATH. Install JDK with jpackage or run packaging on a machine with jpackage.
  exit /b 1
)

echo Running jpackage...
if exist "%JAVA_FX_LIB_PATH%" (
  echo Using JavaFX SDK at %JAVA_FX_LIB_PATH%
  jpackage --name %APP_NAME% --input target --main-jar %APP_NAME%.jar --main-class %MAIN_CLASS% --type exe --dest installer --module-path "%JAVA_FX_LIB_PATH%" --add-modules javafx.controls,javafx.fxml
) else (
  jpackage --name %APP_NAME% --input target --main-jar %APP_NAME%.jar --main-class %MAIN_CLASS% --type exe --dest installer
)

endlocal
