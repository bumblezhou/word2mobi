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

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.jdom2.Parent;
import org.kdp.word.Transformer;
import org.kdp.word.utils.JDOMUtils;

/**
 * Transform footnode references
 */
public class FootnodeTransformer implements Transformer {
    
    
    @Override
    public void transform(Context context) {
        
        List<Element> footnodes = new ArrayList<>();
        
        Element root = context.getSourceRoot();
        for (Element el : root.getChildren()) {
            findFootnodeElements(context, footnodes, el);
        }
        
        JDOMFactory factory = context.getJDOMFactory();
        for (Element el : footnodes) {
            String text = getFootnodeText(el);
            Parent parent = el.getParent();
            int index = parent.indexOf(el);
            parent.removeContent(index);
            Element span = factory.element("span");
            span.setAttribute("class", "MsoFootnoteReference");
            Element a = el.clone();
            a.removeContent();
            a.setText(text);
            span.addContent(a);
            parent.addContent(index, span);
        }
    }

    private void findFootnodeElements(Context context, List<Element> elements, Element el) {
        if (isFootnodeRef(el)) {
            elements.add(el);
        } else {
            for (Element ch : el.getChildren()) {
                findFootnodeElements(context, elements, ch);
            }
        }
    }

    private boolean isFootnodeRef(Element el) {
        if (!JDOMUtils.isElement(el, "a", null, null)) {
            return false;
        }
        Attribute att = el.getAttribute("href");
        return att != null && att.getValue().startsWith("#_ftn");
    }

    private String getFootnodeText(Element el) {
        String result = el.getText().trim();
        if (result.length() == 0) {
            for (Element ch : el.getChildren()) {
                return getFootnodeText(ch);
            }
        }
        return result;
    }
}