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
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.kdp.word.Parser;
import org.kdp.word.Transformer;

/**
 * Add an anchor for each 'MsoToc1' style element 
 */
public class TOCTransformer implements Transformer {
    
    private final JDOMFactory factory = new DefaultJDOMFactory();
    
    @Override
    public void transform(Parser parser, Path basedir, Element root) {
        for (Element el : root.getChildren()) {
            transformInternal(root, el);
        }
    }

    private void transformInternal(Element root, Element el) {
        Attribute att = el.getAttribute("class");
        String clname = att != null ? att.getValue() : null; 
        if ("p".equals(el.getName()) && "MsoToc1".equals(clname)) {
            Element tocel = getFirstTextElement(el);
            String tocname = tocel.getText();
            int dotidx = tocname.indexOf("...");
            if (dotidx > 0) {
                tocname = tocname.substring(0, dotidx).trim();
            }
            String aname = findAnchorName(root, tocname);
            if (aname != null) {
                Element anchor = factory.element("a");
                anchor.getAttributes().add(factory.attribute("href", "#" + aname));
                anchor.setText(tocname);
                el.getChildren().clear();
                el.setText(null);
                el.getChildren().add(anchor);
            }
        } else {
            for (Element ch : el.getChildren()) {
                transformInternal(root, ch);
            }
        }
    }

    private Element getFirstTextElement(Element el) {
        String result = el.getText();
        if (result.length() == 0) {
            for (Element ch : el.getChildren()) {
                return getFirstTextElement(ch);
            }
        }
        return el;
    }

    private String findAnchorName(Element el, String targetName) {
        String result = null;
        if ("h1".equals(el.getName())) {
            String h1Name = getFirstTextElement(el).getText();
            if (targetName.equals(h1Name)) {
                result = getAnchorName(el);
            }
        } else {
            for (Element ch : el.getChildren()) {
                result = findAnchorName(ch, targetName);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }
    
    private String getAnchorName(Element el) {
        String result = null;
        Attribute att = el.getAttribute("name");
        if ("a".equals(el.getName()) && att != null) {
            result = att.getValue();
        } else {
            for (Element ch : el.getChildren()) {
                result = getAnchorName(ch);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }
}