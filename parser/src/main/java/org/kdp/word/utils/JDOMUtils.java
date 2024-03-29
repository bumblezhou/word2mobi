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
package org.kdp.word.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.JDOMFactory;
import org.jdom2.input.StAXStreamBuilder;

public final class JDOMUtils {

    // hide ctor
    private JDOMUtils() {
    }
    
    public static Document parse(JDOMFactory factory, File inputfile) {
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader reader = inputFactory.createXMLStreamReader(new FileInputStream(inputfile));
            StAXStreamBuilder staxBuilder = new StAXStreamBuilder();
            staxBuilder.setFactory(factory);
            return staxBuilder.build(reader);
        } catch (FileNotFoundException | XMLStreamException | JDOMException ex) {
            throw new IllegalStateException("Cannot parse XML: " + inputfile, ex);
        }
    }
    
    public static Element findElement(Element root, String name) {
        return findElement(root, name, null, null);
    }
    
    public static Element findElement(Element root, String name, String attname, String attvalue) {
        IllegalArgumentAssertion.assertNotNull(name, "name");
        if (isElement(root, name, attname, attvalue)) {
            return root;
        } else {
            for (Element ch : root.getChildren()) {
                Element el = findElement(ch, name, attname, attvalue);
                if (el != null) {
                    return el;
                }
            }
        }
        return null;
    }

    public static boolean isElement(Element el, String name, String attname, String attvalue) {
        if (el != null && el.getName().equals(name)) {
            if (attname != null && attvalue != null) {
                Attribute att = el.getAttribute(attname);
                return att != null && attvalue.equals(att.getValue());
            } else {
                return true;
            }
        } 
        return false;
    }
}
