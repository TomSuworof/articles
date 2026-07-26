package com.salat.allarticles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MdLinkConverter {

    private static final Pattern MD_LINK = Pattern.compile("(href=\")(.*?)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern MD_LINK_SINGLE = Pattern.compile("(href=')(.*?)(')", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws IOException {
        Path buildDir = Path.of(System.getProperty("user.dir")).resolve("target/html");
        if (!Files.isDirectory(buildDir)) {
            System.out.println("MdLinkConverter: " + buildDir + " not found, skipping");
            return;
        }

        try (var stream = Files.list(buildDir)) {
            stream.filter(p -> p.toString().endsWith(".html")).forEach(MdLinkConverter::convertFile);
        }
        System.out.println("MdLinkConverter: done");
    }

    private static void convertFile(Path file) {
        try {
            String content = Files.readString(file);
            String converted = convertMdLinks(content);
            if (!converted.equals(content)) {
                Files.writeString(file, converted);
                System.out.println("MdLinkConverter: converted links in " + file.getFileName());
            }
        } catch (IOException e) {
            System.err.println("MdLinkConverter: failed to process " + file + ": " + e.getMessage());
        }
    }

    static String convertMdLinks(String html) {
        html = replaceMdLinks(MD_LINK, html);
        html = replaceMdLinks(MD_LINK_SINGLE, html);
        return html;
    }

    private static String replaceMdLinks(Pattern pattern, String html) {
        StringBuffer sb = new StringBuffer();
        Matcher m = pattern.matcher(html);
        while (m.find()) {
            String url = m.group(2);
            if (url.endsWith(".md")) {
                m.appendReplacement(sb, m.group(1) + url.substring(0, url.length() - 3) + ".html" + m.group(3));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
