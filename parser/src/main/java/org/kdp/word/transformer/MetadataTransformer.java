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
package org.kdp.word.transformer;

import java.nio.file.Path;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.kdp.word.Parser;
import org.kdp.word.Transformer;

/**
 * Transforms the Generator meta element  
 */
public class MetadataTransformer implements Transformer {
    
    @Override
    public void transform(Parser parser, Path basedir, Element root) {
        
        for (Element el : root.getChildren()) {
            transformInternal(parser, el);
        }
    }

    private void transformInternal(Parser parser, Element el) {
        if ("meta".equals(el.getName())) {
            Attribute att = el.getAttribute("name");
            String attval = att != null ? att.getValue() : null;
            if ("Generator".equals(attval)) {
                att = el.getAttribute("content");
                attval = att.getValue();
                att.setValue(attval + " - word2mobi");
            }
        }
        for (Element ch : el.getChildren()) {
            transformInternal(parser, ch);
        }
    }
}