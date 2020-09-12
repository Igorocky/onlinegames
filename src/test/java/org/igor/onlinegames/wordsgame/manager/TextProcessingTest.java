package org.igor.onlinegames.wordsgame.manager;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TextProcessingTest {
    @Test
    public void splitOnSentences_should_work_correctly() throws IOException {
        //given
        String text = IOUtils.resourceToString("/text-parsing/text-to-parse-1.txt", StandardCharsets.UTF_8);

        //when
        List<List<TextToken>> paragraphs = TextProcessing.splitOnParagraphs(text, "ignored\n");

        //then
        assertEquals(3, paragraphs.size());
        assertEquals("!…", paragraphs.get(0).get(paragraphs.get(0).size()-2).getValue());
        assertEquals(System.lineSeparator(), paragraphs.get(0).get(paragraphs.get(0).size()-1).getValue());
        assertEquals("abc", paragraphs.get(1).get(paragraphs.get(1).size()-2).getValue());
        assertEquals(System.lineSeparator(), paragraphs.get(1).get(paragraphs.get(1).size()-1).getValue());
        assertEquals("C", paragraphs.get(2).get(paragraphs.get(2).size()-1).getValue());

        List<TextToken> tokens = new ArrayList<>();
        paragraphs.forEach(tokens::addAll);
        int i = 0;
        assertEquals(TextToken.builder().value("Word1").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("1word2").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("word3").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("!").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("    ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("1Word").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(": ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("[").meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("1word3").active(true).unsplittable(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("]").meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("-").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("ignored").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("!…").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(System.lineSeparator()).meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("\"").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("the").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("1word").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("\", ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("phrase").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("to").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("learn").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("no").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(".").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("[").meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("un split table").active(true).unsplittable(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("]").meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("abc").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(System.lineSeparator()).meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("A").active(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("{").meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("B").ignored(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("}").meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value(" ").build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("C").active(true).build(), tokens.get(i++));
        assertEquals(i, tokens.size());
    }

    @Test
    public void splitOnSentences_should_work_correctly_2() throws IOException {
        //given
        String text = "[site.com]";

        //when
        List<TextToken> tokens = TextProcessing.splitOnTokens(text, null);

        //then
        assertEquals(3, tokens.size());
        int i = 0;

        assertEquals(TextToken.builder().value("[").meta(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("site.com").active(true).unsplittable(true).build(), tokens.get(i++));
        assertEquals(TextToken.builder().value("]").meta(true).build(), tokens.get(i++));
        assertEquals(i, tokens.size());
    }
}