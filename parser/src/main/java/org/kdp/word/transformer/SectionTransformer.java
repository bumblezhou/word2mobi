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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.jdom2.Parent;
import org.kdp.word.Transformer;
import org.kdp.word.utils.IOUtils;
import org.kdp.word.utils.JDOMUtils;

/**
 * Transforms the source into multiple section files
 */
public class SectionTransformer implements Transformer {
    
    @Override
    public void transform(Context context) {
        JDOMFactory factory = context.getJDOMFactory();
        
        Sections sections = new Sections();
        context.putAttribute(Sections.class, sections);
        
        Element root = context.getSourceRoot();
        for (Element el : root.getChildren()) {
            findWordSections(context, sections, el);
        }

        boolean navfound = false;
        Iterator<Section> itsec = sections.iterator();
        while (itsec.hasNext()) {
            Section section = itsec.next();

            if (navfound) {
                itsec.remove();
                continue;
            }
            navfound = section.isnav;
            
            // Remove the section from the original document
            Element element = section.element;
            Parent parent = element.getParent();
            parent.removeContent(element);
            
            // Build the target document 
            Element rootClone = root.clone();
            Element bodyClone = JDOMUtils.findElement(rootClone, "body");
            bodyClone.removeContent();
            bodyClone.addContent(element.clone());
            
            // Write the section document
            Document doc = factory.document(rootClone);
            File outfile = section.target.toFile();
            try {
                outfile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(outfile);
                IOUtils.writeDocument(context, doc, fos);
                fos.close();
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }


    private void findWordSections(Context context, Sections sections, Element el) {
        String sectionName = getSectionName(el);
        if (sectionName != null) {
            Section section = new Section(context, sectionName, el);
            sections.add(section);
        } else {
            for (Element ch : el.getChildren()) {
                findWordSections(context, sections, ch);
            }
        }
    }

    private String getSectionName(Element el) {
        if (!JDOMUtils.isElement(el, "div", null, null)) {
            return null;
        }
        Attribute att = el.getAttribute("class"); 
        if (att == null) {
            return null;
        }
        String name = att.getValue();
        return name.startsWith("WordSection") ? name : null;
    }
    
    @SuppressWarnings("serial")
    static class Sections extends ArrayList<Section> {
    }
    
    static class Section {
        final String name;
        final Element element;
        final Path target;
        final boolean isnav;
        Section(Context context, String name, Element element) {
            this.name = name;
            this.element = element;
            this.isnav = JDOMUtils.findElement(element, "li", "class", "MsoToc1") != null;
            Path bookdir = context.getOptions().getBookDir();
            this.target = bookdir.resolve(name + ".xhtml");
        }
    }
}