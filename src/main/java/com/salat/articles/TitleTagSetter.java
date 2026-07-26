package com.salat.articles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleTagSetter {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>");
    private static final Pattern ENTRY_PATTERN = Pattern.compile("\\[(.+?)]\\(\\./(.+?)\\.md\\)");
    private static final Pattern H1_TEXT_PATTERN = Pattern.compile("<h1>(.*?)</h1>");

    public static void main(String[] args) throws IOException {
        Path baseDir = Path.of(System.getProperty("user.dir"));
        Path srcDir = baseDir.resolve("src/main/resources/markdown");
        Path buildDir = baseDir.resolve("target/html");

        if (!Files.isDirectory(buildDir)) {
            System.out.println("TitleTagSetter: " + buildDir + " not found, skipping");
            return;
        }

        Map<String, String> slugToTitle = parseArticles(srcDir.resolve("index.md"));

        try (var stream = Files.list(buildDir)) {
            stream.filter(p -> p.toString().endsWith(".html")).forEach(file ->
                    setTitle(file, slugToTitle));
        }
        System.out.println("TitleTagSetter: done");
    }

    private static void setTitle(Path file, Map<String, String> slugToTitle) {
        try {
            String content = Files.readString(file);
            String slug = file.getFileName().toString().replace(".html", "");

            String newTitle = null;

            // Try to get title from <h1> in the article content
            Matcher h1 = H1_TEXT_PATTERN.matcher(content);
            if (h1.find()) {
                newTitle = stripTags(h1.group(1));
            }

            // Fallback to index.md mapping
            if (newTitle == null || newTitle.isBlank()) {
                newTitle = slugToTitle.get(slug);
            }

            // Skip files we don't have a title for
            if (newTitle == null || newTitle.isBlank()) {
                return;
            }

            Matcher titleMatcher = TITLE_PATTERN.matcher(content);
            if (titleMatcher.find()) {
                String updated = titleMatcher.replaceFirst("<title>" + escapeHtml(newTitle) + "</title>");
                Files.writeString(file, updated);
                System.out.println("TitleTagSetter: set title for " + file.getFileName() + " -> " + newTitle);
            }
        } catch (IOException e) {
            System.err.println("TitleTagSetter: failed to process " + file + ": " + e.getMessage());
        }
    }

    private static Map<String, String> parseArticles(Path indexFile) throws IOException {
        String text = Files.readString(indexFile);
        Map<String, String> articles = new LinkedHashMap<>();
        Matcher m = ENTRY_PATTERN.matcher(text);
        while (m.find()) {
            articles.put(m.group(2), m.group(1));
        }
        return articles;
    }

    private static String stripTags(String html) {
        return html.replaceAll("<[^>]+>", "").trim();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
