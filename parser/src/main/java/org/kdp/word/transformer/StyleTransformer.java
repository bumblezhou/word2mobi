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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom2.Element;
import org.kdp.word.Parser;
import org.kdp.word.Transformer;
import org.kdp.word.utils.IllegalArgumentAssertion;
import org.kdp.word.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transform style definitions
 */
public class StyleTransformer implements Transformer {

    private static Logger log = LoggerFactory.getLogger(StyleTransformer.class);
    
    @Override
    public void transform(Parser parser, Path basedir, Element root) {
        Map<String, Style> replace = new HashMap<>();
        for (String key : parser.getPropertyKeys()) {
            if (key.startsWith(Parser.PROPERTY_STYLE_REPLACE)) {
                String name = key.substring(Parser.PROPERTY_STYLE_REPLACE.length() + 1);
                Style style = new Style (name);
                String values = parser.getProperty(key);
                addStyleAttributes(style, values);
                replace.put(name, style);
            }
        }
        replace = Collections.unmodifiableMap(replace);
        for (Element el : root.getChildren()) {
            transformInternal(el, replace);
        }
    }

    private void transformInternal(Element el, Map<String, Style> replace) {
        if ("style".equals(el.getName())) {
            String content = transformStyles(el.getTextTrim(), replace);
            el.setText(content);
        } else {
            for (Element ch : el.getChildren()) {
                transformInternal(ch, replace);
            }
        }
    }

    private String transformStyles(String content, Map<String, Style> replace) {
        List<Style> styles = new ArrayList<>();
        
        // Parse the list of syles
        BufferedReader br = new BufferedReader(new StringReader(content));
        try {
            String line = br.readLine();
            while (line != null) {
                if (line.contains("--") || line.contains("/*") || line.trim().length() == 0) {
                    line = br.readLine();
                    continue;
                }
                styles.add(parseStyle(br, line));
                line = br.readLine();
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        
        // Replace/Add style attributes
        for (Style style : styles) {
            for (Style repstyle : replace.values()) {
                if (style.name.contains(repstyle.name)) {
                    style.merge(repstyle);
                }
            }
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (Style style : styles) {
            pw.println(style.name + " {");
            for (Entry<String, String> entry : style.attributes.entrySet()) {
                pw.println("   " + entry.getKey() + ":" + entry.getValue() + ";");
            }
            pw.println("}");
        }
        return sw.toString();
    }

    private Style parseStyle(BufferedReader br, String firstLine) throws IOException {
        Style result;
        StringBuffer values = new StringBuffer();
        int brackidx = firstLine.indexOf("{");
        if (brackidx > 0) {
            values.append(firstLine.substring(brackidx + 1));
            result = new Style(firstLine.substring(0, brackidx));
        } else {
            result = new Style(firstLine.trim());
        }
        String line = br.readLine();
        while (line != null) {
            brackidx = line.indexOf("{");
            if (brackidx >= 0) {
                line = line.substring(brackidx + 1);
            }
            brackidx = line.indexOf("}");
            if (brackidx >= 0) {
                values.append(line.substring(0, brackidx));
                break;
            } else {
                values.append(line.trim());
            }
            line = br.readLine();
        }
        addStyleAttributes(result, values.toString());
        return result;
    }

    private void addStyleAttributes(Style style, String values) {
        for (String keyval : values.split(";")) {
            String[] tuple = keyval.split(":");
            IllegalStateAssertion.assertEquals(2, tuple.length, "Illegal style attribute: " + keyval);
            style.attributes.put(tuple[0].trim(), tuple[1].trim());
        }
    }

    class Style {
        final String name;
        final Map<String, String> attributes = new LinkedHashMap<>();

        Style(String name) {
            this.name = name;
        }

        void merge(Style other) {
            IllegalArgumentAssertion.assertTrue(name.contains(other.name), "Style name missmatch: " + this + " <> " + other);
            log.debug("Merge style: {}", other);
            for (Entry<String, String> entry : other.attributes.entrySet()) {
                attributes.put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public String toString() {
            return "Style [name=" + name + ", attributes=" + attributes + "]";
        }
    }
}