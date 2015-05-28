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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.jdom2.Parent;
import org.kdp.word.Transformer;
import org.kdp.word.utils.IllegalArgumentAssertion;
import org.kdp.word.utils.JDOMUtils;

/**
 * Transform footnode references
 */
public class FootnodeTransformer implements Transformer {

    @Override
    public void transform(Context context) {

        Map<String, Footnode> footnodes = new LinkedHashMap<>();

        Element root = context.getSourceRoot();
        for (Element el : root.getChildren()) {
            findFootnodes(context, el, footnodes);
        }

        JDOMFactory factory = context.getJDOMFactory();
        for (Footnode fn : footnodes.values()) {
            
            // Footnode Ref
            Element fnref = fn.fnref;
            String text = getFootnodeText(fnref);
            Parent parent = fnref.getParent();
            int index = parent.indexOf(fnref);
            parent.removeContent(index);
            Element span = factory.element("span");
            span.setAttribute("class", "MsoFootnoteReference");
            Element a = fnref.clone();
            a.removeContent();
            a.setText(text);
            //a.setAttribute("type", "noteref", OPFTransformer.NS_OPF);
            span.addContent(a);
            parent.addContent(index, span);
            
            /* Footnode Text
            Element fntxt = fn.fntxt;
            Element p = findMsoFootnoteText(fntxt); 
            text = getFootnodeText(p);
            p.getAttributes().clear();
            p.removeContent();
            p.setText(text);
            String divid = fn.id.substring(1);
            Element div = JDOMUtils.findElement(root, "div", "id", divid);
            IllegalStateAssertion.assertSame(p.getParentElement(), div, "Unexpected parent: " + div);
            Parent divparent = div.getParent();
            Element aside = factory.element("aside");
            aside.setAttribute("type", "footnote", OPFTransformer.NS_OPF);
            aside.setAttribute("id", fn.id);
            index = divparent.indexOf(div);
            divparent.removeContent(div);
            aside.addContent(p.clone());
            divparent.addContent(index, aside);
            */
        }
    }

    private void findFootnodes(Context context, Element el, Map<String, Footnode> footnodes) {
        String id = isFootnodeRef(el);
        if (id != null) {
            Element root = context.getSourceRoot();
            Element fntxt = JDOMUtils.findElement(root, "a", "name", id);
            footnodes.put(id, new Footnode(id, el, fntxt));
        } else {
            for (Element ch : el.getChildren()) {
                findFootnodes(context, ch, footnodes);
            }
        }
    }

    private String isFootnodeRef(Element el) {
        String result = null;
        if (JDOMUtils.isElement(el, "a", null, null)) {
            Attribute att = el.getAttribute("href");
            if (att != null) {
                String value = att.getValue();
                if (value.startsWith("#_ftn")) { 
                    result = value.substring(1);
                }
            }
        }
        return result;
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

    static class Footnode {
        
        final String id;
        final Element fnref;
        final Element fntxt;
        
        Footnode(String id, Element fnref, Element fntxt) {
            IllegalArgumentAssertion.assertNotNull(id, "id");
            IllegalArgumentAssertion.assertNotNull(fnref, "fnref");
            IllegalArgumentAssertion.assertNotNull(fntxt, "fntxt");
            this.id = id;
            this.fnref = fnref;
            this.fntxt = fntxt;
        }
    }
}