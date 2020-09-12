package org.igor.onlinegames.wordsgame.manager;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.igor.onlinegames.common.OnlinegamesUtils.listOf;

public class TextProcessing {
    /*
       '—' - 2014
       '…' - 2026
       '„' - 201E
       '”' - 201D
      */
    private static final String BORDER_SYMBOL = "[\\.?!\\u2026\\s\\r\\n,:;\"\\(\\)\\[\\]\\\\/\\*\\u201E\\u201D]";
    private static final String SENTENCE_PARTS_DELIMITER = "((?<=" + BORDER_SYMBOL + ")(?!" + BORDER_SYMBOL + "))|((?<!" + BORDER_SYMBOL + ")(?=" + BORDER_SYMBOL + "))";
    private static final Pattern NOT_ACTIVE_PATTERN = Pattern.compile("^[\\(\\)-.,\\s–\":\\[\\]\\\\/;!?\\u2014\\u2026\\u201E\\u201D]+$");
    private static final List<String> SENTENCE_ENDS = listOf(".", "!", "?", "…");
    private static final List<String> R_N = listOf("\r", "\n");

    public static List<List<TextToken>> splitOnParagraphs(String text, String ignoreList) {
        final List<TextToken> tokens = splitOnTokens(text, ignoreList);
        List<List<TextToken>> paragraphs = new ArrayList<>();
        List<TextToken> paragraph = new ArrayList<>();
        for (TextToken token : tokens) {
            paragraph.add(token);
            if (isEndOfParagraph(token)) {
                paragraphs.add(paragraph);
                paragraph = new ArrayList<>();
            }
        }
        if (!paragraph.isEmpty()) {
            paragraphs.add(paragraph);
        }
        return paragraphs;
    }

    public static List<TextToken> splitOnTokens(String text, String ignoreList) {
        Set<String> substringsToIgnore = StringUtils.isNoneBlank(ignoreList)
                ? Stream.of(ignoreList.split("[\r\n]+"))
                    .filter(StringUtils::isNoneBlank)
                    .collect(Collectors.toSet())
                : Collections.emptySet();

        return splitOnTokens(text, substringsToIgnore);
    }

    private static List<TextToken> splitOnTokens(String text, Set<String> substringsToIgnore) {
        List<Object> tokensRaw = extractUnsplittable(text);
        tokensRaw = extractPredefinedParts(tokensRaw, substringsToIgnore);
        List<TextToken> tokens = tokenize(tokensRaw);
        tokens = splitByLongestSequence(tokens, R_N);
        tokens = splitByLongestSequence(tokens, SENTENCE_ENDS);

        for (TextToken token : tokens) {
            enhanceWithAttributes(token, substringsToIgnore);
        }
        return tokens;
    }

    private static List<TextToken> splitByLongestSequence(List<TextToken> tokens, List<String> substrings) {
        List<TextToken> res = new LinkedList<>();
        for (TextToken token : tokens) {
            String val = token.getValue();
            if (isSplittableBy(token, substrings)) {
                int s = 0;
                while (s < val.length() && !substrings.contains(val.substring(s,s+1))) {
                    s++;
                }
                int e = s+1;
                while (e < val.length() && substrings.contains(val.substring(e,e+1))) {
                    e++;
                }
                if (s > 0) {
                    res.add(TextToken.builder().value(val.substring(0,s)).build());
                }
                res.add(TextToken.builder().value(val.substring(s,e)).build());
                if (e < val.length()) {
                    res.addAll(splitByLongestSequence(listOf(TextToken.builder().value(val.substring(e)).build()), substrings));
                }
            } else {
                res.add(token);
            }
        }
        return res;
    }

    private static boolean containsOneOf(String str, List<String> substrings) {
        for (String substring : substrings) {
            if (str.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSplittableBy(TextToken token, List<String> substrings) {
        return !isUnsplittable(token) && containsOneOf(token.getValue(), substrings);
    }

    private static boolean isUnsplittable(TextToken token) {
        return token.getUnsplittable() != null && token.getUnsplittable();
    }

    private static void enhanceWithAttributes(TextToken token, Set<String> ignoreList) {
        String val = token.getValue();
        if (containsOneOf(val, R_N)) {
            token.setMeta(true);
        } else if (!(ignoreList.contains(val) || NOT_ACTIVE_PATTERN.matcher(val).matches())) {
            token.setActive(true);
        }
    }

    private static List<Object> extractPredefinedParts(List<Object> res, Set<String> predefinedParts) {
        for (String predefinedPart : predefinedParts) {
            res = extractPredefinedPart(res, predefinedPart);
        }
        return res;
    }

    private static List<Object> extractUnsplittable(String text) {
        List<Object> res = new LinkedList<>();
        String tail = text;
        int idxS = tail.indexOf("[[");
        int idxE = idxS < 0 ? -1 : tail.indexOf("]]", idxS+2);
        while (idxE >= 2) {
            if (idxS > 0) {
                res.add(tail.substring(0, idxS));
            }
            res.add(TextToken.builder().value("[[").meta(true).build());
            res.add(TextToken.builder().value(tail.substring(idxS+2, idxE)).unsplittable(true).build());
            res.add(TextToken.builder().value("]]").meta(true).build());
            tail = tail.substring(idxE + 2);
            idxS = tail.indexOf("[[");
            idxE = idxS < 0 ? -1 : tail.indexOf("]]", idxS+2);
        }
        if (!tail.isEmpty()) {
            res.add(tail);
        }
        return res;
    }

    private static List<Object> extractPredefinedPart(List<Object> text, String predefinedPart) {
        List<Object> res = new LinkedList<>();
        for (Object obj : text) {
            if (obj instanceof TextToken) {
                res.add(obj);
            } else {
                String tail = (String) obj;
                int idx = tail.indexOf(predefinedPart);
                while (idx >= 0) {
                    res.add(tail.substring(0, idx));
                    res.add(TextToken.builder().value(predefinedPart).build());
                    tail = tail.substring(idx + predefinedPart.length());
                    idx = tail.indexOf(predefinedPart);
                }
                if (!tail.isEmpty()) {
                    res.add(tail);
                }
            }
        }
        return res;
    }

    private static boolean isEndOfParagraph(TextToken token) {
        return isSplittableBy(token, R_N);
    }

    private static List<TextToken> tokenize(List<Object> text) {
        List<TextToken> res = new LinkedList<>();
        for (Object obj : text) {
            if (obj instanceof TextToken) {
                res.add((TextToken) obj);
            } else {
                String str = (String) obj;
                for (String part : str.split(SENTENCE_PARTS_DELIMITER)) {
                    res.add(TextToken.builder().value(part).build());
                }
            }
        }
        return res;
    }
}
