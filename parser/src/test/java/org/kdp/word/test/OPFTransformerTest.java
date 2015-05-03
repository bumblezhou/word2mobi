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
 * Tests the {@see OPFTransformer}
 */
public class OPFTransformerTest {
    
    @Test
    public void testGeneratedOPF() throws Exception {
        
        ParserBuilder builder = ParserBuilderFactory.newInstance();
        Parser parser = builder.output("test.xhtml").opfTarget("test-book.opf").pretty().build();
        
        File infile = new File("src/test/resources/WebPage07.html");
        parser.process(infile);
        
        File opffile = new File("target/book/test-book.opf");
        Assert.assertTrue("File exists: " + opffile, opffile.isFile());

        List<String> lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(opffile));
        String line = br.readLine();
        while (line != null) {
            //System.out.println(line);
            lines.add(line.trim());
            line = br.readLine();
        }
        br.close();
        
        Assert.assertTrue(contains(lines, "<dc:title>Blumen für Alle</dc:title>"));
        Assert.assertTrue(contains(lines, "<dc:creator xmlns:epub=\"http://www.idpf.org/2007/opf\" epub:role=\"aut\">Peter Post</dc:creator>"));
        Assert.assertTrue(contains(lines, "<dc:language>DE</dc:language>"));

        Assert.assertTrue(contains(lines, "id=\"CoverImage\" href=\"images/book-cover.jpg\""));
        Assert.assertTrue(contains(lines, "id=\"Content\" href=\"test.xhtml\""));
    }

    private boolean contains(List<String> lines, String substr) {
        for (String line : lines) {
            if (line.contains(substr)) {
                return true;
            }
                
        }
        return false;
    }
}
