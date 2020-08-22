package org.igor.onlinegames.tools;

import org.apache.commons.io.FileUtils;
import org.igor.onlinegames.common.OnlinegamesUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexReplace {
    public static void main(String[] args) throws IOException {
        new RegexReplace().run("roboto-300-400-500.css");
        new RegexReplace().run("mui-icons.css");
    }

    public void run(String cssFileName) throws IOException {
        Map<String, String> urlMap = new HashMap<>();
        String localVersionDirName = "local-version";
        String cssDir = "./src/main/webapp/css/";
        String localVersionDir = cssDir + localVersionDirName + "/";
        replace(
                new File(cssDir + cssFileName),
                Pattern.compile("url\\(([^\\(\\)]+)\\)"),
                matcher -> {
                    System.out.println(matcher.group(0));
                    String oldUrl = matcher.group(1);
                    if (!urlMap.containsKey(oldUrl)) {
                        String fileName = UUID.randomUUID().toString();
                        urlMap.put(oldUrl, "/css/" + localVersionDirName + "/" + fileName);
                        copyUrlToFile(oldUrl, new File(localVersionDir + fileName));
                    }
                    return "url(" + urlMap.get(oldUrl) + ")";
                },
                new File(localVersionDir + cssFileName)
        );
    }

    public void copyUrlToFile(String url, File destination) {
        try {
            FileUtils.copyURLToFile(new URL(url), destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void replace(File srcFile, Pattern pattern, Function<Matcher, String> replacement, File dstFile) throws IOException {
        String content = FileUtils.readFileToString(srcFile, StandardCharsets.UTF_8);
        String newContent = OnlinegamesUtils.replace(content, pattern, replacement);
        dstFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(dstFile, newContent, StandardCharsets.UTF_8);
    }
}
