package app;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private String title = "Untitled";
    private List<String> pages = new ArrayList<>();
    private int currentPage = 0;
    private String fontFamily = "Serif";
    private int fontSize = 16;
    private String style = "contemporary"; // old, contemporary, future
    private java.util.List<String> pageBackgrounds = new ArrayList<>();

    public Book(){
        // start with two empty pages
        pages.add("");
        pages.add("");
        pageBackgrounds.add("");
        pageBackgrounds.add("");
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int idx) { if (idx >= 0 && idx < pages.size()) currentPage = idx; }

    public void nextPage() { if (currentPage < pages.size() - 1) currentPage++; }
    public void prevPage() { if (currentPage > 0) currentPage--; }

    public void addPageAfterCurrent() { pages.add(currentPage + 1, ""); currentPage++; }
    public void removeCurrentPage() { if (pages.size() > 1) { pages.remove(currentPage); if (currentPage >= pages.size()) currentPage = pages.size() - 1; } else { pages.set(0, ""); } }

    public String getCurrentPageContent() { return pages.get(currentPage); }
    public void setCurrentPageContent(String text) { pages.set(currentPage, text); }

    public void ensureBackgroundsSize() {
        while (pageBackgrounds.size() < pages.size()) pageBackgrounds.add("");
    }

    public String getBackgroundForPage(int idx) {
        if (idx < 0 || idx >= pageBackgrounds.size()) return "";
        return pageBackgrounds.get(idx);
    }

    public void setBackgroundForPage(int idx, String bg) {
        ensureBackgroundsSize();
        if (idx >= 0 && idx < pageBackgrounds.size()) pageBackgrounds.set(idx, bg);
    }

    public java.util.List<String> getPageBackgrounds() { return pageBackgrounds; }
    public void setPageBackgrounds(java.util.List<String> bgs) { this.pageBackgrounds = bgs; ensureBackgroundsSize(); }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getPages() { return pages; }
    public void setPages(List<String> pages) { this.pages = pages; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
}
