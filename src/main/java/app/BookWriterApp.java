package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class BookWriterApp extends Application {

    private Book book = new Book();
    private TextArea leftPage = new TextArea();
    private TextArea rightPage = new TextArea();
    private static final Logger LOGGER = Logger.getLogger("BookWriterApp");

    private BorderPane root;

    @Override
    public void start(Stage stage) {
        // initialize logging
        initLogging();
        LOGGER.info("Starting BookWriterApp");

        // global uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((t, ex) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + t.getName(), ex);
        });

        ToolBar toolbar = buildToolbar(stage);

        HBox pages = new HBox(10, wrapPage(leftPage), wrapPage(rightPage));
        pages.setPadding(new Insets(10));
        pages.setAlignment(Pos.CENTER);

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(pages);
        root.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        applyBookToUI();

        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("Book Writer");
        stage.setScene(scene);
        stage.show();
    }

    private ToolBar buildToolbar(Stage stage) {
        ComboBox<String> fontFamily = new ComboBox<>();
        fontFamily.getItems().addAll("Serif", "SansSerif", "Monospaced", "Georgia", "Times New Roman");
        fontFamily.setValue(book.getFontFamily());

        // font size presets + spinner
        ComboBox<String> sizePresets = new ComboBox<>();
        sizePresets.getItems().addAll("12","14","16","18","20","24","28","32");
        sizePresets.setValue(String.valueOf(book.getFontSize()));
        Spinner<Integer> fontSize = new Spinner<>(8, 72, book.getFontSize());

    Button saveBtn = new Button("Save");
    saveBtn.setGraphic(new Label("💾"));
    saveBtn.setTooltip(new Tooltip("Save book (Ctrl+S)"));

    Button loadBtn = new Button("Load");
    loadBtn.setGraphic(new Label("📂"));
    loadBtn.setTooltip(new Tooltip("Load book"));

        ToggleGroup styleGroup = new ToggleGroup();
        RadioButton oldStyle = new RadioButton("Old");
        RadioButton contStyle = new RadioButton("Contemporary");
        RadioButton futStyle = new RadioButton("Future");
        oldStyle.setToggleGroup(styleGroup);
        contStyle.setToggleGroup(styleGroup);
        futStyle.setToggleGroup(styleGroup);
        switch (book.getStyle()) {
            case "old": oldStyle.setSelected(true); break;
            case "future": futStyle.setSelected(true); break;
            default: contStyle.setSelected(true);
        }

        fontFamily.setOnAction(e -> {
            book.setFontFamily(fontFamily.getValue());
            applyBookToUI();
        });
        fontSize.valueProperty().addListener((obs, oldV, newV) -> {
            book.setFontSize(newV);
            applyBookToUI();
        });

        saveBtn.setOnAction(e -> {
            LOGGER.info("Save button clicked");
            doSave(stage);
        });
        loadBtn.setOnAction(e -> {
            LOGGER.info("Load button clicked");
            doLoad(stage);
        });

        // Project mode toggle (enables multi-page controls)
        ToggleButton projectMode = new ToggleButton("Project Mode");

        // Pagination controls
        Button prevBtn = new Button("◀ Prev");
        Button nextBtn = new Button("Next ▶");
        Button addPageBtn = new Button("+ Page");
        Button removePageBtn = new Button("- Page");

        // Color picker for text fill
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setTooltip(new Tooltip("Text color"));

        prevBtn.setOnAction(e -> {
            updateBookFromUI();
            book.prevPage();
            applyBookToUI();
            LOGGER.info("Moved to previous page: " + book.getCurrentPage());
        });
        nextBtn.setOnAction(e -> {
            updateBookFromUI();
            book.nextPage();
            applyBookToUI();
            LOGGER.info("Moved to next page: " + book.getCurrentPage());
        });
        addPageBtn.setOnAction(e -> {
            updateBookFromUI();
            book.addPageAfterCurrent();
            applyBookToUI();
            LOGGER.info("Added page, current: " + book.getCurrentPage());
        });
        removePageBtn.setOnAction(e -> {
            updateBookFromUI();
            book.removeCurrentPage();
            applyBookToUI();
            LOGGER.info("Removed page, current: " + book.getCurrentPage());
        });

        colorPicker.setOnAction(e -> {
            String color = toRgbString(colorPicker.getValue());
            leftPage.setStyle("-fx-text-fill: " + color + ";");
            rightPage.setStyle("-fx-text-fill: " + color + ";");
        });

        styleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == oldStyle) book.setStyle("old");
            else if (newT == futStyle) book.setStyle("future");
            else book.setStyle("contemporary");
            applyBookToUI();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

    ToolBar tb = new ToolBar(
        new Label("Font:"), fontFamily,
        new Label("Size:"), sizePresets, fontSize,
        new Separator(),
        saveBtn, loadBtn,
        spacer,
        projectMode,
        prevBtn, nextBtn, addPageBtn, removePageBtn,
        new Separator(),
        colorPicker,
        oldStyle, contStyle, futStyle
    );

        return tb;
    }

    private VBox wrapPage(TextArea ta) {
        ta.setWrapText(true);
        ta.setPrefWidth(440);
        ta.setPrefHeight(600);
        ta.getStyleClass().add("page-textarea");
        VBox box = new VBox(ta);
        box.getStyleClass().add("page");
        return box;
    }

    private void applyBookToUI() {
        // show two pages around currentPage when in project mode; otherwise show page 0/1
        int cp = book.getCurrentPage();
        if (cp < 0) cp = 0;
        String left = "";
        String right = "";
        if (book.getPages().size() == 0) {
            left = right = "";
        } else {
            left = book.getPages().get(cp);
            if (cp + 1 < book.getPages().size()) right = book.getPages().get(cp + 1);
        }
        leftPage.setText(left);
        rightPage.setText(right);

    Font f = Font.font(book.getFontFamily(), book.getFontSize());
    leftPage.setFont(f);
    rightPage.setFont(f);

        // apply style class on root so CSS selectors can style pages
        if (root != null) {
            root.getStyleClass().removeAll("old", "contemporary", "future");
            switch (book.getStyle()) {
                case "old": root.getStyleClass().add("old"); break;
                case "future": root.getStyleClass().add("future"); break;
                default: root.getStyleClass().add("contemporary");
            }
        }
    }

    private void doSave(Stage stage) {
        // update model from UI
        updateBookFromUI();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Book");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Book JSON", "*.json"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            // simple text format (no external libs required)
            try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                w.write("title=" + (book.getTitle() == null ? "" : book.getTitle()));
                w.newLine();
                w.write("font=" + book.getFontFamily());
                w.newLine();
                w.write("size=" + book.getFontSize());
                w.newLine();
                w.write("style=" + book.getStyle());
                w.newLine();
                w.write("----PAGE1----"); w.newLine();
                w.write(book.getPages().size() > 0 ? book.getPages().get(0) : "");
                w.newLine();
                w.write("----PAGE2----"); w.newLine();
                w.write(book.getPages().size() > 1 ? book.getPages().get(1) : "");
            }
            LOGGER.info("Book saved to " + file.getAbsolutePath());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Save failed", ex);
            showError("Save failed", ex.getMessage());
        }
    }

    private void doLoad(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Book");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Book File", "*.json", "*.book", "*.txt"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            // simple parser
            Book loaded = new Book();
            String[] parts = content.split("\r?\n");
            int i = 0;
            while (i < parts.length && !parts[i].startsWith("----PAGE1----")) {
                String line = parts[i++];
                if (line.startsWith("title=")) loaded.setTitle(line.substring("title=".length()));
                else if (line.startsWith("font=")) loaded.setFontFamily(line.substring("font=".length()));
                else if (line.startsWith("size=")) {
                    try { loaded.setFontSize(Integer.parseInt(line.substring("size=".length()))); } catch (NumberFormatException ignored) {}
                } else if (line.startsWith("style=")) loaded.setStyle(line.substring("style=".length()));
            }
            StringBuilder p1 = new StringBuilder();
            StringBuilder p2 = new StringBuilder();
            // read page1
            if (i < parts.length && parts[i].startsWith("----PAGE1----")) {
                i++;
                while (i < parts.length && !parts[i].startsWith("----PAGE2----")) {
                    p1.append(parts[i++]);
                    if (i < parts.length && !parts[i].startsWith("----PAGE2----")) p1.append(System.lineSeparator());
                }
            }
            if (i < parts.length && parts[i].startsWith("----PAGE2----")) {
                i++;
                while (i < parts.length) {
                    p2.append(parts[i++]);
                    if (i < parts.length) p2.append(System.lineSeparator());
                }
            }
            loaded.setPages(Arrays.asList(p1.toString(), p2.toString()));
            this.book = loaded;
            applyBookToUI();
            LOGGER.info("Book loaded from " + file.getAbsolutePath());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Load failed", ex);
            showError("Load failed", ex.getMessage());
        }
    }

    private void updateBookFromUI() {
        // write back current left/right to the book pages based on currentPage
        int cp = book.getCurrentPage();
        // ensure pages list long enough
        while (book.getPages().size() <= cp) book.getPages().add("");
        book.getPages().set(cp, leftPage.getText());
        if (book.getPages().size() <= cp + 1) book.getPages().add("");
        book.getPages().set(cp + 1, rightPage.getText());
    }

    private String toRgbString(javafx.scene.paint.Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("rgb(%d,%d,%d)", r, g, b);
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }

    private void initLogging() {
        try {
            Path logPath = Path.of("bookwriter.log");
            FileHandler fh = new FileHandler(logPath.toString(), true);
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
            LOGGER.setLevel(Level.ALL);
            LOGGER.info("Logging initialized");
        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
