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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.kdp.word.Parser;
import org.kdp.word.Transformer;
import org.kdp.word.utils.IllegalStateAssertion;

/**
 * Transforms element attributes according the word2mobi.properties 
 */
public class AttributeTransformer implements Transformer {
    
    @Override
    public void transform(Parser parser, Path basedir, Element root) {
        
        Set<String> remove = new HashSet<>();
        Set<Replace> replace = new HashSet<>();
        for (String key : parser.getPropertyKeys()) {
            String value = parser.getProperty(key);
            if (key.startsWith(Parser.PROPERTY_ATTRIBUTE_REMOVE) && Boolean.parseBoolean(value)) {
                String attid = key.substring(Parser.PROPERTY_ATTRIBUTE_REMOVE.length() + 1);
                remove.add(attid);
            }
            if (key.startsWith(Parser.PROPERTY_ATTRIBUTE_REPLACE)) {
                String attid = key.substring(Parser.PROPERTY_ATTRIBUTE_REPLACE.length() + 1);
                attid = attid.substring(0, attid.lastIndexOf('.'));
                String[] items = value.split(",");
                IllegalStateAssertion.assertEquals(2, items.length, "Invalid attribute replace spec: " + value);
                String substr = items[0].trim();
                String newval = items[1].trim();
                Replace repitem = new Replace(attid, substr, newval);
                replace.add(repitem);
            }
        }
        for (Element el : root.getChildren()) {
            transformInternal(parser, el, remove, replace);
        }
    }

    private void transformInternal(Parser parser, Element el, Set<String> remove, Set<Replace> replace) {
        String elname = el.getName();
        for (Attribute att : new ArrayList<Attribute>(el.getAttributes())) {
            String attname = att.getName();
            String attvalue = att.getValue();
            String attid = elname + "." + attname;
            if (remove.contains(attid)) {
                el.removeAttribute(att);
            } else {
                for (Replace rep : replace) {
                    if (attid.equals(rep.attid) && attvalue.contains(rep.substr)) {
                        att.setValue(rep.newval);
                    }
                }
            }
        }
        for (Element ch : el.getChildren()) {
            transformInternal(parser, ch, remove, replace);
        }
    }
    
    class Replace {
        final String attid;
        final String substr;
        final String newval;
        Replace(String attid, String substr, String newval) {
            this.attid = attid;
            this.substr = substr;
            this.newval = newval;
        }
    }
}