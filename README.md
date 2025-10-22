# Book Writer

Simple JavaFX Book Writer demo.

Features
- Double-page editor (left and right pages)
- Toolbar: font family, font size, save/load (JSON), style selector (old/contemporary/future)

Run

1. Ensure JDK 11+ is installed and `java`/`javac` are on PATH.
2. Place any required external jars in `speciessimulator\bin` (relative to the project root).

Quick start (Windows):

Run the included batch which will build (via Maven) if needed and then launch the app:

```powershell
src\main\run.bat
```

Alternatively, build and run with Maven (if you have Maven installed):

```powershell
mvn -DskipTests package
mvn javafx:run
```

Notes
- The `run.bat` script expects external libraries to be in `speciessimulator\bin` and will include them on the classpath.
- The app saves and loads books as JSON files.

Next steps
- Add icons for toolbar actions
- Add pagination and multipage support
- Add export to PDF/print
# Book Writer

Small JavaFX app that simulates a double-page book editor with a toolbar for font family, size, save/load, and style themes (old, contemporary, future).

Requirements
- JDK 17+
- Maven

Run
1. From project root (where `pom.xml` is):

```powershell
mvn clean javafx:run
```

Files changed
- `src/main/java/app/BookWriterApp.java` - main JavaFX application
- `src/main/java/app/Book.java` - simple model
- `src/main/resources/styles.css` - CSS themes
- `pom.xml` - Maven project with JavaFX dependencies
