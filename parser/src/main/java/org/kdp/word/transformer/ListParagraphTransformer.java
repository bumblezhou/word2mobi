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
        int firstItemIndex = -1;
        List<Element> listItems = new ArrayList<>();
        Iterator<Element> itch = parent.getChildren().iterator();
        while (itch.hasNext()) {
            Element ch = itch.next();
            if (JDOMUtils.isElement(ch, "p", "class", "MsoListParagraph")) {
                if (listItems.isEmpty()) {
                    firstItemIndex = parent.getChildren().indexOf(ch);     
                }
                listItems.add(ch);
                itch.remove();
            } else if (!listItems.isEmpty()) {
                processListItemBatch(context, parent, listItems, firstItemIndex);
                itch = parent.getChildren().iterator();
                listItems = new ArrayList<>();
            }
        }
        if (!listItems.isEmpty()) {
            processListItemBatch(context, parent, listItems, firstItemIndex);
        }
    }

    private void processListItemBatch(Context context, Element parent, List<Element> listItems, int firstItemIndex) {
        JDOMFactory factory = context.getJDOMFactory();
        Element ul = factory.element("ul");
        for (Element liItem : listItems) {
            boolean ignoreFirstElement = true;
            boolean ignoreFirstText = true;
            Element li = factory.element("li");
            li.getAttributes().add(factory.attribute("class", "MsoListParagraph"));
            for (Content ch : liItem.getContent()) {
                if (ch.getCType() == CType.Element) {
                    if (ignoreFirstElement) {
                        ignoreFirstElement = false;
                    } else {
                        Element chel = (Element) ch;
                        li.addContent(chel.clone());
                    }
                } else if (ch.getCType() == CType.Text) {
                    if (ignoreFirstText) {
                        ignoreFirstText = false;
                    } else {
                        ignoreFirstElement = false;
                        Text txel = (Text) ch;
                        String text = txel.getText();
                        li.addContent(text);
                    }
                }
            }
            ul.getChildren().add(li);
        }
        parent.getChildren().add(firstItemIndex, ul);
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