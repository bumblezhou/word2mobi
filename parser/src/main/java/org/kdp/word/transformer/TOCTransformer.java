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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.kdp.word.Transformer;
import org.kdp.word.utils.IOUtils;
import org.kdp.word.utils.IllegalStateAssertion;
import org.kdp.word.utils.JDOMUtils;

/**
 * Add an anchor for each 'MsoToc1' style element 
 */
public class TOCTransformer implements Transformer {
    
    @Override
    public void transform(Context context) {
        JDOMFactory factory = context.getJDOMFactory();
        
        Element root = context.getSourceRoot();
        for (Element el : root.getChildren()) {
            transformInternal(context, el);
        }
        
        Element first = JDOMUtils.findElement(root, "p", "class", "MsoToc1");
        if (first != null) {
            Element parent = first.getParentElement();
            List<Element> children = parent.getChildren();
            
            // Add the nav element
            Element nav = factory.element("nav");
            nav.getAttributes().add(factory.attribute("type", "toc", OPFTransformer.NS_OPF));
            int index = children.indexOf(first);
            children.add(index, nav);
            
            // Add the ol element
            Element ol = factory.element("ol");
            ol.setAttribute("class", "Toc");
            nav.getChildren().add(ol);
            
            Iterator<Element> itel = children.iterator();
            while (itel.hasNext()) {
                Element el = itel.next();
                if (JDOMUtils.isElement(el, "p", "class", "MsoToc1")) {
                    Element li = factory.element("li");
                    li.getAttributes().add(factory.attribute("class", "MsoToc1"));
                    li.addContent(el.cloneContent());
                    ol.getChildren().add(li);
                    itel.remove();
                }
            }
        }
    }

    private void transformInternal(Context context, Element el) {
        JDOMFactory factory = context.getJDOMFactory();
        Element root = context.getSourceRoot();
        if (JDOMUtils.isElement(el, "p", "class", "MsoToc1")) {
            Element tocel = getFirstTextElement(el);
            String tocname = tocel.getText();
            int dotidx = tocname.indexOf("...");
            if (dotidx > 0) {
                tocname = tocname.substring(0, dotidx).trim();
            }
            String aname = findAnchorName(root, tocname);
            IllegalStateAssertion.assertNotNull(aname, "Cannot find anchor for: " + tocname);
            Element anchor = factory.element("a");
            Path targetPath = IOUtils.bookRelative(context, context.getTarget());
            anchor.getAttributes().add(factory.attribute("href", targetPath + "#" + aname));
            anchor.setText(tocname);
            el.getChildren().clear();
            el.setText(null);
            el.getChildren().add(anchor);
        } else {
            for (Element ch : el.getChildren()) {
                transformInternal(context, ch);
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
            if (equalsIgnoreWhitespace(targetName, h1Name)) {
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
    
    private boolean equalsIgnoreWhitespace(String target, String candidate) {
        List<String> tlist = Arrays.asList(target.split("[\\s]"));
        List<String> clist = Arrays.asList(candidate.split("[\\s]"));
        return tlist.equals(clist);
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