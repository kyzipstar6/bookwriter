package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
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
    private VBox leftBox;
    private VBox rightBox;
    private HBox thumbnailBar = new HBox(8);
    private ToggleButton projectModeToggle;

    @Override
    public void start(Stage stage) {
        // initialize logging
        initLogging();
        LOGGER.info("Starting BookWriterApp");

        // global uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((t, ex) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + t.getName(), ex);
        });
            Label pginfo = new Label(" Pages "+ book.getCurrentPage() + " & " + (book.getCurrentPage() + 1) + " shown, total pages: " + book.getPages().size());

    ToolBar toolbar = buildToolbar(stage, pginfo);

    leftBox = wrapPage(leftPage);
    leftPage.addEventHandler(KeyEvent.KEY_TYPED, e->{
        if (leftPage.getScrollTop()>0){
            rightPage.requestFocus();
            rightPage.positionCaret(0);
            leftPage.setScrollTop(0);
            e.consume();
        }
    });
    rightPage.addEventHandler(KeyEvent.KEY_PRESSED, e->{
        if (rightPage.getScrollTop()>0){
             updateBookFromUI();
            book.addPageAfterCurrent();
            book.setCurrentPage(book.getCurrentPage()+1);
            applyBookToUI();
            refreshThumbnails();
            pginfo.setText(" Pages "+ book.getCurrentPage() + " & " + (book.getCurrentPage() + 1) + " shown, total pages: " + book.getPages().size());

            leftPage.requestFocus();
            leftPage.positionCaret(leftPage.getText().length());
            rightPage.setScrollTop(0);
            e.consume();
        }
        if(e.getCode()==KeyCode.DELETE && rightPage.getCaretPosition()==rightPage.getText().length()){
            leftPage.requestFocus();
            leftPage.positionCaret(leftPage.getText().length());
                        refreshThumbnails();

            e.consume();
    
        }
    });
    rightBox = wrapPage(rightPage);
    HBox pages = new HBox(10, leftBox, rightBox);
        pages.setPadding(new Insets(10));
        pages.setAlignment(Pos.CENTER);

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(pages);
    thumbnailBar.setPadding(new Insets(6));
    thumbnailBar.setAlignment(Pos.CENTER_LEFT);
    
    pginfo.setStyle("-fx-font-size: 14; -fx-text-fill: #333;");

    root.setBottom(new HBox(5, thumbnailBar, pginfo));
        root.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        applyBookToUI();

        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("Book Writer");
        stage.setScene(scene);
        registerShortcuts(scene, stage);
        stage.setOnCloseRequest(e -> {
            styleSave();
        });
        refreshThumbnails();
        stage.show();
        try{
        styleLoad(book);}catch(Exception ex){ex.printStackTrace();}
    }

    private void registerShortcuts(Scene scene, Stage stage) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), () -> doSave(stage));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), () -> doLoad(stage));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN), () -> { updateBookFromUI(); book.nextPage(); applyBookToUI(); refreshThumbnails(); });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN), () -> { updateBookFromUI(); book.prevPage(); applyBookToUI(); refreshThumbnails(); });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> { updateBookFromUI(); book.addPageAfterCurrent(); applyBookToUI(); refreshThumbnails(); });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN), () -> { updateBookFromUI(); book.removeCurrentPage(); applyBookToUI(); refreshThumbnails(); });
    }

    private ToolBar buildToolbar(Stage stage, Label pginfo) {
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
    projectModeToggle = new ToggleButton("Project Mode");
    

        // Pagination controls
        Button prevBtn = new Button("◀ Prev");
        Button nextBtn = new Button("Next ▶");
        Button addPageBtn = new Button("+ Page");
        Button removePageBtn = new Button("- Page");

    // Color picker for text fill
    ColorPicker colorPicker = new ColorPicker();
        colorPicker.setTooltip(new Tooltip("Text color"));

    // Background controls for current page
    ColorPicker bgColorPicker = new ColorPicker();
    bgColorPicker.setTooltip(new Tooltip("Set page background color"));
    Button bgImageBtn = new Button("Set Background Image");

    // Separate left/right background controls
    ColorPicker leftBgColor = new ColorPicker();
    leftBgColor.setTooltip(new Tooltip("Left page background color"));
    Button leftBgImage = new Button("Left Img");

    ColorPicker rightBgColor = new ColorPicker();
    rightBgColor.setTooltip(new Tooltip("Right page background color"));
    Button rightBgImage = new Button("Right Img");

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
            pginfo.setText(" Pages "+ book.getCurrentPage() + " & " + (book.getCurrentPage() + 1) + " shown, total pages: " + book.getPages().size());
            LOGGER.info("Moved to next page: " + book.getCurrentPage());
        });
        addPageBtn.setOnAction(e -> {
            updateBookFromUI();
            book.addPageAfterCurrent();
            applyBookToUI();
            pginfo.setText(" Pages "+ book.getCurrentPage() + " & " + (book.getCurrentPage() + 1) + " shown, total pages: " + book.getPages().size());

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
            book.setFill(color);
            rightPage.setStyle("-fx-text-fill: " + color + ";");
        });

        bgColorPicker.setOnAction(e -> {
            String col = toRgbString(bgColorPicker.getValue());
            int cp = book.getCurrentPage();
            book.setBackgroundForPage(cp, col);
            applyBookToUI();
            refreshThumbnails();
        });

        bgImageBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                int cp = book.getCurrentPage();
                book.setBackgroundForPage(cp, "img:" + f.getAbsolutePath());
                applyBookToUI();
                refreshThumbnails();
            }
        });

        leftBgColor.setOnAction(e -> {
            String col = toRgbString(leftBgColor.getValue());
            int cp = book.getCurrentPage();
            book.setBackgroundForPage(cp, col);
            applyBookToUI();
            refreshThumbnails();
        });
        leftBgImage.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                int cp = book.getCurrentPage();
                book.setBackgroundForPage(cp, "img:" + f.getAbsolutePath());
                applyBookToUI();
                refreshThumbnails();
            }
        });

        rightBgColor.setOnAction(e -> {
            String col = toRgbString(rightBgColor.getValue());
            int cp = book.getCurrentPage() + 1;
            book.setBackgroundForPage(cp, col);
            applyBookToUI();
            refreshThumbnails();
        });
        rightBgImage.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                int cp = book.getCurrentPage() + 1;
                book.setBackgroundForPage(cp, "img:" + f.getAbsolutePath());
                applyBookToUI();
                refreshThumbnails();
            }
        });

        styleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == oldStyle) book.setStyle("old");
            else if (newT == futStyle) book.setStyle("future");
            else book.setStyle("contemporary");
            
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar tb = new ToolBar(
        new Label("Font:"), fontFamily,
        new Label("Size:"), sizePresets, fontSize,
        new Separator(),
        saveBtn, loadBtn,
        spacer,
        projectModeToggle,
        prevBtn, nextBtn, addPageBtn, removePageBtn,
        new Separator(),
        colorPicker,
        new Separator(),
        new Label("L Bg:"), leftBgColor, leftBgImage,
        new Label("R Bg:"), rightBgColor, rightBgImage,
        new Separator(),
        new Label("Page Bg:"), bgColorPicker, bgImageBtn,
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
        // ensure backgrounds list size
        book.ensureBackgroundsSize();

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
    try{
    leftPage.setStyle("-fx-text-fill: " + book.getTextFill() + ";");
    rightPage.setStyle("-fx-text-fill: " +  book.getTextFill() + ";");
    }catch(Exception ex){ex.printStackTrace();}
    // apply per-page backgrounds
    String leftBg = book.getBackgroundForPage(cp);
    String rightBg = book.getBackgroundForPage(cp + 1);
    applyBackgroundToBox(leftBox, leftBg);
    applyBackgroundToBox(rightBox, rightBg);

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
    public void styleSave(){
         try (BufferedWriter w = Files.newBufferedWriter(new File("files\\book_style.json").toPath(), StandardCharsets.UTF_8)) {
                w.write("title=" + (book.getTitle() == null ? "" : book.getTitle())); w.newLine();
                w.write("font=" + book.getFontFamily()); w.newLine();
                w.write("size=" + book.getFontSize()); w.newLine();
                w.write("style=" + book.getStyle()); w.newLine();
                w.write("pages=" + book.getPages().size()); w.newLine();
                w.write("textFill=" + book.getTextFill());
    } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Style Save failed", ex);
            showError("Style Save failed", ex.getMessage());
        }
    }
        public void styleLoad(Book loaded) {
        try {
            String content = Files.readString(new File("files\\book_style.json").toPath(), StandardCharsets.UTF_8);
            String[] parts = content.split("\r?\n");
            
            int i = 0;
            while (i < parts.length) {
                String line = parts[i++];
                if (line.startsWith("title=")) loaded.setTitle(line.substring("title=".length()));
                else if (line.startsWith("font=")) loaded.setFontFamily(line.substring("font=".length()));
                else if (line.startsWith("size=")) {
                    try { loaded.setFontSize(Integer.parseInt(line.substring("size=".length()))); } catch (NumberFormatException ignored) {}
                } else if (line.startsWith("style=")) loaded.setStyle(line.substring("style=".length()));
                else if (line.startsWith("textFill=")) {
                    loaded.setFill(line.substring("textFill=".length()) + ";");
                }
            }
            this.book = loaded;
            applyBookToUI();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Style Load failed", ex);
            showError("Style Load failed", ex.getMessage());
        }
}

    private void doSave(Stage stage) {
        // update model from UI
         String lTextCont = leftPage.getText();
        String rTextCont = leftPage.getText();
        updateBookFromUI();


        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Book");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Book JSON", "*.json"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            // simple multi-page format
            try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                w.write("title=" + (book.getTitle() == null ? "" : book.getTitle())); w.newLine();
                w.write("font=" + book.getFontFamily()); w.newLine();
                w.write("size=" + book.getFontSize()); w.newLine();
                w.write("style=" + book.getStyle()); w.newLine();
                w.write("pages=" + book.getPages().size()); w.newLine();
                w.write("textFill=" + book.getTextFill()); w.newLine();

                book.ensureBackgroundsSize();
                for (int i = 0; i < book.getPages().size(); i++) {
                    w.write("----PAGE----"); w.newLine();
                    w.write("bg=" + book.getBackgroundForPage(i)); w.newLine();
                    w.write(book.getPages().get(i) == null ? "" : book.getPages().get(i)); w.newLine();
                }
            }
            LOGGER.info("Book saved to " + file.getAbsolutePath());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Save failed", ex);
            showError("Save failed", ex.getMessage());
        }
        //if (!leftPage.getText().equals(lTextCont))leftPage.setText(lTextCont);
        //if (!rightPage.getText().equals(rTextCont))rightPage.setText(rTextCont);
    }

    private void doLoad(Stage stage) {
       

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Book");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Book File", "*.json", "*.book", "*.txt"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String[] parts = content.split("\r?\n");
            Book loaded = new Book();
            loaded.getPages().clear();
            loaded.getPageBackgrounds().clear();
            int i = 0;
            while (i < parts.length) {
                String line = parts[i++];
                if (line.startsWith("title=")) loaded.setTitle(line.substring("title=".length()));
                else if (line.startsWith("font=")) loaded.setFontFamily(line.substring("font=".length()));
                else if (line.startsWith("size=")) {
                    try { loaded.setFontSize(Integer.parseInt(line.substring("size=".length()))); } catch (NumberFormatException ignored) {}
                } else if (line.startsWith("style=")) loaded.setStyle(line.substring("style=".length()));
                else if (line.startsWith("pages=")) {
                    // ignore, we'll parse pages
                } 
                 else if (line.startsWith("textFill=")) {
                    loaded.setFill(line.substring("textFill=".length()) + ";");
                }else if (line.startsWith("----PAGE----")) {
                    String bg = "";
                    if (i < parts.length && parts[i].startsWith("bg=")) {
                        bg = parts[i++].substring(3);
                    }
                    StringBuilder pageContent = new StringBuilder();
                    while (i < parts.length && !parts[i].startsWith("----PAGE----")) {
                        pageContent.append(parts[i++]);
                        if (i < parts.length && !parts[i].startsWith("----PAGE----")) pageContent.append(System.lineSeparator());
                    }
                    loaded.getPages().add(pageContent.toString());
                    loaded.getPageBackgrounds().add(bg);
                }
            }
            if (loaded.getPages().isEmpty()) {
                loaded.getPages().add("");
                loaded.getPageBackgrounds().add("");
            }
            this.book = loaded;
            applyBookToUI();
            refreshThumbnails();
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
        while (book.getPages().size() <= cp) { book.getPages().add(""); book.getPageBackgrounds().add(""); }
        book.getPages().set(cp, leftPage.getText());
        if (book.getPages().size() <= cp + 1) { book.getPages().add(""); book.getPageBackgrounds().add(""); }
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

    private void applyBackgroundToBox(VBox box, String bg) {
        if (bg == null || bg.isEmpty()) {
            box.setBackground(Background.EMPTY);
            box.setStyle("");
            return;
        }
        if (bg.startsWith("img:")) {
            String path = bg.substring(4);
            try {
                Image img = new Image(new FileInputStream(path));
                BackgroundSize sz = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false);
                BackgroundImage bi = new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, sz);
                box.setBackground(new Background(bi));
            } catch (FileNotFoundException e) {
                box.setBackground(Background.EMPTY);
            }
            return;
        }
        box.setStyle("-fx-background-color: " + bg + ";");
    }

    private void refreshThumbnails() {
        thumbnailBar.getChildren().clear();
        for (int i = 0; i < book.getPages().size(); i++) {
            int idx = i;
            VBox thumb = new VBox();
            thumb.setPrefSize(100, 60);
            thumb.setStyle("-fx-border-color:#ccc; -fx-padding:6; -fx-alignment:center;");
            Label label = new Label("Page " + (i+1));
            thumb.getChildren().add(label);
            String bg = book.getBackgroundForPage(i);
            if (bg != null && !bg.isEmpty()) {
                if (bg.startsWith("img:")) {
                    try { Image im = new Image(new FileInputStream(bg.substring(4)), 96, 56, true, true); ImageView iv = new ImageView(im); thumb.getChildren().clear(); thumb.getChildren().add(iv); } catch (FileNotFoundException ignored) {}
                } else {
                    thumb.setStyle(thumb.getStyle() + "-fx-background-color: " + bg + ";");
                }
            }
            thumb.setOnMouseClicked(ev -> {
                book.setCurrentPage(idx);
                applyBookToUI();
            });
            thumbnailBar.getChildren().add(thumb);
        }
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
