/*
 * #%L
 * Word2Mobi :: Parser
 * %%
 * Copyright (C) 2015 Private
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.kdp.word.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.kdp.word.Parser;
import org.kdp.word.ParserBuilder;

/**
 * Tests the {@see TOCTransformer}
 */
public class TOCTransformerTest {
    
    @Test
    public void testTOC() throws Exception {
        
        ParserBuilder builder = ParserBuilderFactory.newInstance();
        Parser parser = builder.pretty().build();
        
        File infile = new File("src/test/resources/WebPage02.html");
        String result = parser.process(infile);
        //System.out.println(result);
        Assert.assertFalse("No WordSection1", result.contains("<div class=\"WordSection1\">"));
        Assert.assertTrue("Contains WordSection2", result.contains("<div class=\"WordSection2\">"));
        
        File tocfile = new File("target/book/WordSection1-TOC.html");
        Assert.assertTrue("Exists WordSection1-TOC.html", tocfile.exists());
        
        List<String> lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(tocfile));
        String line = br.readLine();
        while (line != null) {
            lines.add(line.trim());
            line = br.readLine();
        }
        br.close();
        
        Assert.assertTrue("Contains ol class", contains(lines, "<ol class=\"Toc\">"));
        Assert.assertTrue("Contains li class", contains(lines, "<li class=\"MsoToc1\">"));
        Assert.assertTrue("Contains #_Toc2", contains(lines, "<a href=\"WebPage02.html#_Toc2\">Chapter 2</a>"));
        Assert.assertTrue("Contains #_Toc3", contains(lines, "<a href=\"WebPage02.html#_Toc3\">Chapter 3</a>"));
        Assert.assertTrue("Contains #_Toc4", contains(lines, "<a href=\"WebPage02.html#_Toc4\">Chapter 4</a>"));
    }

    private boolean contains(List<String> lines, String substring) {
        for (String line : lines) {
            if (line.contains(substring)) {
                return true;
            }
        }
        return false;
    }
}
