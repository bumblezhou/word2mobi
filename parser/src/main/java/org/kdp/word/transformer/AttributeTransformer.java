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

/**
 * Transforms element attributes according the word2mobi.properties 
 */
public class AttributeTransformer implements Transformer {
    
    @Override
    public void transform(Parser parser, Path basedir, Element root) {
        Set<Replace> replace = new HashSet<>();
        for (String key : parser.getPropertyKeys()) {
            String value = parser.getProperty(key);
            if (key.startsWith(Parser.PROPERTY_ATTRIBUTE_REPLACE)) {
                String attid = key.substring(Parser.PROPERTY_ATTRIBUTE_REPLACE.length() + 1);
                String[] keyItems = attid.split("\\.");
                if (keyItems.length > 2) {
                    int firstIndex = attid.indexOf('.');
                    int secondIndex = attid.indexOf('.', firstIndex + 1);
                    attid = attid.substring(0, secondIndex);
                }
                String[] valueItems = value.split(",");
                if (valueItems.length == 2) {
                    replace.add(new Replace(attid, valueItems[0].trim(), valueItems[1].trim()));
                } else if (valueItems.length == 1) {
                    replace.add(new Replace(attid, null, valueItems[0].trim()));
                } else {
                    throw new IllegalStateException("Invalid attribute replace: " + key + "=" + value);
                }
            }
        }
        for (Element el : root.getChildren()) {
            transformInternal(parser, el, replace);
        }
    }

    private void transformInternal(Parser parser, Element el, Set<Replace> replace) {
        String elname = el.getName();
        for (Attribute att : new ArrayList<Attribute>(el.getAttributes())) {
            String attname = att.getName();
            String attvalue = att.getValue();
            String attid = elname + "." + attname;
            for (Replace rep : replace) {
                if (attid.equals(rep.attid)) {
                    if (rep.substr == null || attvalue.contains(rep.substr)) {
                        if (rep.newval == null || rep.newval.length() == 0) {
                            el.getAttributes().remove(att);
                        } else {
                            att.setValue(rep.newval);
                        }
                        break;
                    }
                }
            }
        }
        for (Element ch : el.getChildren()) {
            transformInternal(parser, ch, replace);
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