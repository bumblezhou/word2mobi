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

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.kdp.word.Parser;
import org.kdp.word.ParserBuilder;

/**
 * Tests the {@see FootnodeTransformer}
 */
public class FootnodeTransformerTest {
    
    @Test
    public void testTOC() throws Exception {
        
        ParserBuilder builder = ParserBuilderFactory.newInstance();
        Parser parser = builder.pretty().build();
        
        File infile = new File("src/test/resources/WebPage08.html");
        String result = parser.process(infile);
        //System.out.println(result);
        
        Assert.assertTrue("Contains name=_ftnref1", result.contains("name=\"_ftnref1\""));
        Assert.assertTrue("Contains <a ...>[1]</a>", result.contains("<a ") && result.contains("[1]</a>"));
    }
}
