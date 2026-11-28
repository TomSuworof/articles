package com.salat.articles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RssFeedGenerator {

    private static final String ARTICLE_DIV = "<div class=\"article-root col-md-8 offset-md-2\">";
    private static final String FEED_FILE = "feed.xml";
    private static final int DESCRIPTION_LENGTH = 300;
    private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Moscow");

    private static final DateTimeFormatter SOURCE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter RSS_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private static final Pattern ENTRY_PATTERN =
            Pattern.compile("\\[([^\\]]+)\\]\\(\\./([^)]+)\\.(?:md|html?)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("<time datetime=\"([^\"]+)\">");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>");
    private static final Pattern H1_PATTERN = Pattern.compile("<h1>.*?</h1>", Pattern.DOTALL);
    private static final Pattern PUBLICATION_DATE_DIV =
            Pattern.compile("<div class=\"article-publication-date\">.*?</div>", Pattern.DOTALL);
    private static final Pattern SCRIPT_OR_STYLE =
            Pattern.compile("<(script|style)\\b.*?</\\1>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern RELATIVE_URL = Pattern.compile(
            "(?i)\\b(src|href)=(\"|')(?!https?://|//|/|#|mailto:|javascript:|data:)([^\"']+)\\2");

    private record Article(String title, String slug, LocalDateTime date, String description, String content) {
    }

    public static void main(String[] args) throws IOException {
        Path baseDir = Path.of(System.getProperty("user.dir"));
        Path srcDir = baseDir.resolve("src/main/resources/markdown");
        Path buildDir = baseDir.resolve("target/html");
        String siteUrl = normalizeBaseUrl(args.length > 0 ? args[0] : "https://tomsuworof.github.io/articles/");

        String header = Files.readString(srcDir.resolve("html/header.html"));
        Matcher titleMatcher = TITLE_PATTERN.matcher(header);
        String channelTitle = titleMatcher.find() ? titleMatcher.group(1).trim() : "Articles";

        List<Article> articles = parseArticles(srcDir.resolve("index.md"), buildDir, siteUrl);
        articles.sort(Comparator.comparing(Article::date).reversed());

        String xml = buildRss(channelTitle, siteUrl, articles);
        Path out = buildDir.resolve(FEED_FILE);
        Files.writeString(out, xml);
        System.out.println("RssFeedGenerator: written " + out + " (" + articles.size() + " items)");
    }

    private static List<Article> parseArticles(Path indexFile, Path buildDir, String siteUrl) throws IOException {
        String index = Files.readString(indexFile);
        List<Article> articles = new ArrayList<>();
        Matcher entryMatcher = ENTRY_PATTERN.matcher(index);

        while (entryMatcher.find()) {
            String title = entryMatcher.group(1).trim();
            String slug = entryMatcher.group(2);

            String tail = index.substring(entryMatcher.end(), Math.min(index.length(), entryMatcher.end() + 200));
            Matcher dateMatcher = DATE_PATTERN.matcher(tail);
            if (!dateMatcher.find()) {
                System.out.println("RssFeedGenerator: skip " + slug + " (no publication date)");
                continue;
            }

            LocalDateTime date;
            try {
                date = LocalDateTime.parse(dateMatcher.group(1), SOURCE_DATE_FORMAT);
            } catch (Exception e) {
                System.out.println("RssFeedGenerator: skip " + slug + " (bad publication date)");
                continue;
            }

            Path htmlFile = buildDir.resolve(slug + ".html");
            if (!Files.exists(htmlFile)) {
                System.out.println("RssFeedGenerator: skip " + slug + ".html (not found)");
                continue;
            }

            String content = extractContent(htmlFile, siteUrl);
            String description = excerpt(toPlainText(content), DESCRIPTION_LENGTH);
            articles.add(new Article(title, slug, date, description, content));
        }
        return articles;
    }

    private static String extractContent(Path htmlFile, String siteUrl) throws IOException {
        String html = Files.readString(htmlFile);
        int start = html.indexOf(ARTICLE_DIV);
        if (start == -1) {
            return "";
        }
        start += ARTICLE_DIV.length();

        int bodyEnd = html.indexOf("</body>", start);
        int contentEnd = bodyEnd == -1 ? html.length() : html.lastIndexOf("</div>", bodyEnd);
        if (contentEnd <= start) {
            return "";
        }

        String content = html.substring(start, contentEnd);
        content = H1_PATTERN.matcher(content).replaceAll("");
        content = PUBLICATION_DATE_DIV.matcher(content).replaceAll("");
        content = SCRIPT_OR_STYLE.matcher(content).replaceAll("");
        content = absoluteUrls(content, siteUrl);
        return content.strip();
    }

    private static String absoluteUrls(String html, String siteUrl) {
        Matcher matcher = RELATIVE_URL.matcher(html);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(3);
            while (url.startsWith("./")) {
                url = url.substring(2);
            }
            if (url.isEmpty()) {
                continue;
            }
            String absolute = siteUrl + url;
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    matcher.group(1) + "=" + matcher.group(2) + absolute + matcher.group(2)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String toPlainText(String html) {
        String text = HTML_TAG.matcher(html).replaceAll(" ");
        text = decodeEntities(text);
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }

    private static String excerpt(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int cut = text.lastIndexOf(' ', maxLength);
        if (cut < maxLength / 2) {
            cut = maxLength;
        }
        return text.substring(0, cut) + "…";
    }

    private static String buildRss(String channelTitle, String siteUrl, List<Article> articles) {
        StringBuilder rss = new StringBuilder();
        rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        rss.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\">\n");
        rss.append("  <channel>\n");
        tag(rss, "title", channelTitle);
        tag(rss, "link", siteUrl);
        tag(rss, "description", "Последние статьи");
        tag(rss, "language", "ru");
        rss.append("    <atom:link href=\"").append(escapeXml(siteUrl + FEED_FILE))
                .append("\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        ZonedDateTime buildDate = articles.isEmpty()
                ? ZonedDateTime.now(TIME_ZONE)
                : articles.get(0).date().atZone(TIME_ZONE);
        tag(rss, "lastBuildDate", RSS_DATE_FORMAT.format(buildDate));

        for (Article article : articles) {
            String link = siteUrl + article.slug() + ".html";
            rss.append("    <item>\n");
            tag(rss, "title", article.title());
            tag(rss, "link", link);
            tag(rss, "description", article.description());
            rss.append("      <content:encoded>").append(cdata(article.content())).append("</content:encoded>\n");
            tag(rss, "pubDate", RSS_DATE_FORMAT.format(article.date().atZone(TIME_ZONE)));
            rss.append("      <guid isPermaLink=\"true\">").append(escapeXml(link)).append("</guid>\n");
            rss.append("    </item>\n");
        }

        rss.append("  </channel>\n");
        rss.append("</rss>\n");
        return rss.toString();
    }

    private static void tag(StringBuilder xml, String name, String value) {
        xml.append("    <").append(name).append('>')
                .append(escapeXml(value))
                .append("</").append(name).append(">\n");
    }

    private static String cdata(String text) {
        return "<![CDATA[" + text.replace("]]>", "]]]]><![CDATA[>") + "]]>";
    }

    private static String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private static String decodeEntities(String text) {
        return text.replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
