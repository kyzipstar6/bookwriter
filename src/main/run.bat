
@echo off
rem Run BookWriterApp from project. This script expects the external libraries
rem to be located at <project-root>\speciessimulator\bin

setlocal
rem Resolve script directory and project root (two levels up from src\main)
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%\..\.."
set "PROJECT_ROOT=%CD%"

rem Allow overriding JavaFX SDK lib path via environment variable JAVA_FX_LIB_PATH
if "%JAVA_FX_LIB_PATH%"=="" (
	set "JAVA_FX_LIB_PATH=C:\Users\georg\Documents\java\speciesevolutionsimulator\lib\javafx-sdk-23.0.2\lib"
)

rem If classes are not built, try to build with Maven. If Maven isn't available,
rem attempt a basic javac compile as a fallback. When compiling or running with
rem JavaFX on the classpath, we provide --module-path (for java) or include jars
rem on the classpath for javac if necessary.
if not exist "%PROJECT_ROOT%\target\classes" (
	echo target\classes not found - attempting to build with Maven...
	mvn -q -DskipTests package
	if errorlevel 1 (
		echo Maven build failed or Maven not found. Trying javac compile as fallback...
		md "target\classes" 2>nul
		rem Try to include JavaFX jars for compilation if the SDK path exists
		if exist "%JAVA_FX_LIB_PATH%" (
			javac -d "target\classes" -cp "speciessimulator\bin\*;%JAVA_FX_LIB_PATH%\*" src\main\java\app\*.java
		) else (
			javac -d "target\classes" -cp "speciessimulator\bin\*" src\main\java\app\*.java
		)
		if errorlevel 1 (
			echo Compilation failed. Please build the project with Maven or ensure JDK is available.
			popd
			endlocal
			exit /b 1
		)
	)
)
)

set "LIB_DIR=%PROJECT_ROOT%\speciessimulator\bin"
if not exist "%LIB_DIR%" (
	echo Library directory "%LIB_DIR%" not found.
	echo Please place required jars in %PROJECT_ROOT%\\speciessimulator\\bin
	popd
	endlocal
	exit /b 1
)


rem Build classpath: compiled classes + all jars in the external lib folder
set "CP=%PROJECT_ROOT%\target\classes;%LIB_DIR%\*"

echo Using JavaFX SDK lib path: %JAVA_FX_LIB_PATH%
set "JAVAFX_EXISTS=0"
if exist "%JAVA_FX_LIB_PATH%" set "JAVAFX_EXISTS=1"
if "%JAVAFX_EXISTS%"=="1" (
	echo Running with --module-path %JAVA_FX_LIB_PATH% --add-modules javafx.controls,javafx.fxml
	java --module-path "%JAVA_FX_LIB_PATH%" --add-modules javafx.controls,javafx.fxml -cp "%CP%" app.BookWriterApp %*
) 
if "%JAVAFX_EXISTS%"=="0" (
	echo JavaFX SDK not found at %JAVA_FX_LIB_PATH%. Attempting to run with jars from %LIB_DIR% on classpath.
	java -cp "%CP%" app.BookWriterApp %*
)

popd
endlocal
