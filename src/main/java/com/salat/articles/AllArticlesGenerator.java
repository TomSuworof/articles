package com.salat.articles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AllArticlesGenerator {

    private static final String ARTICLE_DIV = "<div class=\"article-root col-md-8 offset-md-2\">";
    private static final Pattern ENTRY_PATTERN = Pattern.compile("\\[(.+?)]\\(\\./(.+?)\\.md\\)");
    private static final Pattern H1_PATTERN = Pattern.compile("<h1>(.*?)</h1>");

    public static void main(String[] args) throws IOException {
        Path baseDir = Path.of(System.getProperty("user.dir"));
        Path srcDir = baseDir.resolve("src/main/resources/markdown");
        Path buildDir = baseDir.resolve("target/html");

        String header = Files.readString(srcDir.resolve("html/header.html"))
                .replace("##SITE_BASE##", ".");
        String footer = Files.readString(srcDir.resolve("html/footer.html"));

        Map<String, String> articles = parseArticles(srcDir.resolve("index.md"));

        List<String> sections = new ArrayList<>();
        for (Map.Entry<String, String> entry : articles.entrySet()) {
            String slug = entry.getKey();
            String title = entry.getValue();

            Path htmlFile = buildDir.resolve(slug + ".html");
            if (!Files.exists(htmlFile)) {
                System.out.println("skip " + slug + ".html (not found)");
                continue;
            }

            String html = Files.readString(htmlFile);
            String content = extractContent(html);
            if (content == null) {
                System.out.println("skip " + slug + ".html (no content)");
                continue;
            }

            sections.add(linkTitle(content, slug, title));
        }

        String combined = header
                + String.join("\n<hr class=\"all-articles-separator\">\n", sections)
                + "\n" + footer;

        Path out = buildDir.resolve("all.html");
        Files.writeString(out, combined);
        System.out.println("written " + out);
    }

    private static Map<String, String> parseArticles(Path indexFile) throws IOException {
        String text = Files.readString(indexFile);
        Map<String, String> articles = new LinkedHashMap<>();
        Matcher m = ENTRY_PATTERN.matcher(text);
        while (m.find()) {
            String title = m.group(1);
            String slug = m.group(2);
            if (slug.equals("index")) continue;
            articles.put(slug, title);
        }
        return articles;
    }

    private static String extractContent(String html) {
        int idx = html.lastIndexOf(ARTICLE_DIV);
        if (idx == -1) return null;
        int start = idx + ARTICLE_DIV.length();

        int bodyEnd = html.lastIndexOf("</body>");
        if (bodyEnd == -1) return null;

        int lastDiv = html.lastIndexOf("</div>", bodyEnd);
        if (lastDiv == -1 || lastDiv < start) return null;

        return html.substring(start, lastDiv).strip();
    }

    private static String linkTitle(String content, String slug, String fallbackTitle) {
        Matcher m = H1_PATTERN.matcher(content);
        if (m.find()) {
            return m.replaceFirst("<h1><a href=\"" + slug + ".html\">$1</a></h1>");
        }
        String linked = "<h1><a href=\"" + slug + ".html\">" + escapeHtml(fallbackTitle) + "</a></h1>\n";
        return linked + content;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
