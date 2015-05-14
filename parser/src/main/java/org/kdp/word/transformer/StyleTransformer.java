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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.kdp.word.Options;
import org.kdp.word.Parser;
import org.kdp.word.Transformer;
import org.kdp.word.utils.IllegalStateAssertion;
import org.kdp.word.utils.JDOMUtils;

/**
 * Transform style definitions
 */
public class StyleTransformer implements Transformer {

    private List<Replacement> replacements;
    private Set<String> whitelist = new HashSet<>();
    
    @Override
    public void transform(Context context) {
        Parser parser = context.getParser();
        Options options = context.getOptions();
        Path cssPath = options.getExternalCSS();
        if (cssPath == null)
            return;
        
        // Parse the external styles
        replacements = parseStyleReplacements(context);
        parseExternalStyles(context, cssPath);
        
        String wltoks = parser.getProperty(Parser.PROPERTY_STYLE_REPLACE_WHITELIST);
        if (wltoks != null) {
            for (String tok : wltoks.split(",")) {
                whitelist.add(tok.trim());
            }
        }
        
        // Remove existing style definitions
        Element root = context.getSourceRoot();
        Element elStyle = JDOMUtils.findElement(root, "style");
        if (elStyle != null) {
            elStyle.getParentElement().removeContent(elStyle);
        }
        
        // Copy the CSS to the book dir
        Path cssName = cssPath.getFileName();
        try {
            Path bookDir = options.getBookDir();
            Path cssBook = bookDir.resolve(cssName);
            bookDir.toFile().mkdirs();
            Files.copy(cssPath, cssBook, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        
        // Add reference to external styles
        Element elHead = JDOMUtils.findElement(root, "head");
        if (elHead != null) {
            JDOMFactory factory = context.getJDOMFactory();
            Element elLink = factory.element("link");
            elLink.setAttribute("rel", "stylesheet");
            elLink.setAttribute("type", "text/css");
            elLink.setAttribute("href", cssName.toString());
            elHead.addContent(elLink);
            
            transformStyles(context, root);
        }
    }

    private List<Replacement> parseStyleReplacements(Context context) {
        Parser parser = context.getParser();
        List<Replacement> result = new ArrayList<>();
        for (String key : parser.getPropertyKeys()) {
            if (key.startsWith(Parser.PROPERTY_STYLE_REPLACE)) {
                String attname = key.substring(Parser.PROPERTY_STYLE_REPLACE.length() + 1);
                attname = attname.split("\\.")[0];
                String propval = parser.getProperty(key);
                String[] tokens = propval.split(",");
                IllegalStateAssertion.assertEquals(2, tokens.length, "Illegal style replace spec: " + key + "=" + propval);
                Pattern pattern = Pattern.compile(tokens[0].trim());
                String value = tokens[1].trim();
                result.add(new Replacement(attname, pattern, value));
            }
        }
        return result;
    }

    private Styles parseExternalStyles(Context context, Path cssPath) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            BufferedReader br = new BufferedReader(new FileReader(cssPath.toFile()));
            try {
                String line = br.readLine();
                while (line != null) {
                    line = line.trim();
                    if (line.length() > 0) {
                        pw.print(line);
                    }
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        
        Styles result = new Styles();
        String allstyles = sw.toString();
        String[] tokens = allstyles.split("[}{]");
        for (int i = 0; i < tokens.length / 2; i += 2) {
            String keys = tokens[i];
            String atts = tokens[i + 1];
            Style style = new Style(keys);
            style.addAttributes(atts);
            result.add(style);
        }
        return result;
    }

    private void transformStyles(Context context, Element element) {
        Attribute attClass = element.getAttribute("class");
        if (attClass != null) {
            classStyleReplace(context, element, attClass);
        }
        for (Element ch : element.getChildren()) {
            transformStyles(context, ch);
        }
    }

    private void classStyleReplace(Context context, Element element, Attribute attClass) {
        String value = null;
        String attname = attClass.getName();
        String attvalue = attClass.getValue();
        for (Replacement rep : replacements) {
            if (attname.equals(rep.attname)) {
                if (isWhitelisted(attvalue)) {
                    value = attvalue;
                } else if (rep.pattern.matcher(attvalue).matches()) {
                    value = rep.value;
                    break;
                }
            }
        }
        if (value != null) {
            attClass.setValue(value);
        } else {
            element.removeAttribute(attClass);
        }
    }

    private boolean isWhitelisted(String attvalue) {
        for (String substr : whitelist) {
            if (attvalue.contains(substr)) {
                return true;
            }
        }
        return false;
    }

    class Replacement {
        final String attname;
        final Pattern pattern;
        final String value;
        Replacement(String attname, Pattern pattern, String value) {
            this.attname = attname;
            this.pattern = pattern;
            this.value = value;
        }
    }
    
    @SuppressWarnings("serial")
    class Styles extends LinkedHashSet<Style> {
        
        Style findStyle(String sname) {
            for (Style style : this) {
                if (style.names.contains(sname)) {
                    return style;
                }
            }
            return null;
        }
    }
    
    class Style {
        final Set<String> names = new LinkedHashSet<>();;
        final Map<String, String> attributes = new LinkedHashMap<>();

        Style(String namelist) {
            for (String name : namelist.split(",")) {
                names.add(name.trim());
            }
        }

        void addAttributes(String values) {
            for (String keyval : values.split(";")) {
                if (keyval.trim().length() > 0) {
                    String[] tuple = keyval.split(":");
                    IllegalStateAssertion.assertEquals(2, tuple.length, "Illegal style attribute: " + values);
                    attributes.put(tuple[0].trim(), tuple[1].trim());
                }
            }
        }

        @Override
        public String toString() {
            return "Style [name=" + names + ", attributes=" + attributes + "]";
        }
    }
}