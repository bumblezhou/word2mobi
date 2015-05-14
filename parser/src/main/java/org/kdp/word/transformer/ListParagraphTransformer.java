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
import java.util.Iterator;
import java.util.List;

import org.jdom2.Content;
import org.jdom2.Content.CType;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.jdom2.Text;
import org.kdp.word.Transformer;
import org.kdp.word.utils.JDOMUtils;

/**
 * Transforms an MsoListParagraph in an unordered list   
 */
public class ListParagraphTransformer implements Transformer {
    
    @Override
    public void transform(Context context) {
        Element root = context.getSourceRoot();
        for (Element el : root.getChildren()) {
            transformInternal(context, el);
        }
    }

    private void transformInternal(Context context, Element el) {
        if (hasListItems(el)) {
            processListItems(context, el);
        } else {
            for (Element ch : el.getChildren()) {
                transformInternal(context, ch);
            }
        }
    }
    
    private void processListItems(Context context, Element parent) {
        
        // Get the list of list item elements
        List<Element> listItems = new ArrayList<>();
        Iterator<Element> itch = parent.getChildren().iterator();
        while (itch.hasNext()) {
            Element ch = itch.next();
            if (JDOMUtils.isElement(ch, "p", "class", "MsoListParagraph")) {
                listItems.add(ch);
            } else if (!listItems.isEmpty()) {
                processListItemBatch(context, parent, listItems);
                itch = parent.getChildren().iterator();
                listItems.clear();
            }
        }
        if (!listItems.isEmpty()) {
            processListItemBatch(context, parent, listItems);
        }
    }

    private void processListItemBatch(Context context, Element parent, List<Element> listItems) {
        boolean ordered = false;
        for (Element el : listItems) {
            removeNestedSpanElements(el);
            normalizeListItemText(el);
            ordered = processItemMarker(el);
            el.getAttributes().clear();
        }
        Element firstItem = listItems.get(0);
        int index = parent.indexOf(firstItem);
        for (Element el : listItems) {
            parent.removeContent(el);
        }
        JDOMFactory factory = context.getJDOMFactory();
        Element ul = factory.element(ordered ? "ol" : "ul");
        for (Element el : listItems) {
            Element li = factory.element("li");
            li.setAttribute("class", "MsoListParagraph");
            for (Content co : el.getContent()) {
                li.addContent(co.clone());
            }
            ul.addContent(li);
        }
        parent.addContent(index, ul);
    }

    private void removeNestedSpanElements(Element el) {
        Iterator<Element> itch = el.getChildren().iterator();
        while (itch.hasNext()) {
            Element ch = itch.next();
            if ("span".equals(ch.getName())) {
                itch.remove();
            } else {
                removeNestedSpanElements(ch);
            }
        }
    }

    private void normalizeListItemText(Element el) {
        for (Content co : el.getContent()) {
            if (co.getCType() == CType.Text) {
                Text tco = (Text) co;
                String text = tco.getText().trim();
                tco.setText(text + " ");
            }
        }
    }

    private boolean processItemMarker(Element liItem) {
        String first = "";
        int cosize = liItem.getContentSize();
        Content co = liItem.getContent(0);
        if (co.getCType() == CType.Text) {
            Text text = (Text) co;
            String value = text.getText();
            int index = value.indexOf(" ");
            first = value.substring(0, index);
            value = value.substring(index + 1);
            text.setText(value);
        } else if (cosize > 1 && co.getCType() == CType.Element) {
            Element el = (Element) co;
            first = el.getText();
            el.getParent().removeContent(co);
        }
        return first.endsWith(".");
    }

    private boolean hasListItems(Element el) {
        for (Element ch : el.getChildren()) {
            if (JDOMUtils.isElement(ch, "p", "class", "MsoListParagraph")) {
                return true;
            }
        }
        return false;
    }
}