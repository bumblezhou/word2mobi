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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.kdp.word.Options;
import org.kdp.word.Parser;
import org.kdp.word.Transformer;
import org.kdp.word.transformer.SectionTransformer.Section;
import org.kdp.word.transformer.SectionTransformer.Sections;
import org.kdp.word.utils.IOUtils;
import org.kdp.word.utils.IllegalStateAssertion;
import org.kdp.word.utils.JDOMUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate an OPF file from book sections   
 */
public class OPFTransformer implements Transformer {
    
    public static Namespace NS_DC = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
    public static Namespace NS_OPF = Namespace.getNamespace("epub", "http://www.idpf.org/2007/opf");
    
    private static Logger log = LoggerFactory.getLogger(OPFTransformer.class);
    
    @Override
    public void transform(Context context) {
        JDOMFactory factory = context.getJDOMFactory();
        Path basedir = context.getBasedir();
        Options options = context.getOptions();
        File templateFile = basedir.resolve(options.getOpfTemplate()).toFile();
        if (templateFile.isFile()) {
            log.debug("OPF template: {}", templateFile);
            Document doc = JDOMUtils.parse(factory, templateFile);
            Element opf = doc.getRootElement();
            processMetadata(context, opf);
            processManifest(context, opf);
            processSpine(context, opf);
            writeOPFDocument(context, doc);
        } else {
            log.warn("Cannot find template: {}", templateFile);
        }
    }

    private void processMetadata(Context context, Element opf) {

        Element metadata = JDOMUtils.findElement(opf, "metadata");
        JDOMFactory factory = context.getJDOMFactory();
        Parser parser = context.getParser();
        
        // Title
        String title = parser.getProperty(Parser.PROPERTY_OPF_METADATA_TITLE);
        IllegalStateAssertion.assertNotNull(title, "Cannot obtain property: " + Parser.PROPERTY_OPF_METADATA_TITLE);
        Element elTitle = factory.element("title", NS_DC);
        elTitle.setText(title != null ? title : "Undefined Title");
        metadata.getChildren().add(elTitle);

        // Creator
        String author = parser.getProperty(Parser.PROPERTY_OPF_METADATA_AUTHOR);
        IllegalStateAssertion.assertNotNull(author, "Cannot obtain property: " + Parser.PROPERTY_OPF_METADATA_AUTHOR);
        Element elCreator = factory.element("creator", NS_DC);
        elCreator.getAttributes().add(factory.attribute("role", "aut", NS_OPF));
        elCreator.setText(author != null ? author : "Undefined Author");
        metadata.getChildren().add(elCreator);
        
        // Language
        String language = parser.getProperty(Parser.PROPERTY_OPF_METADATA_LANGUAGE);
        if (language != null && language.length() > 0) {
            Element elLanguage = factory.element("language", NS_DC);
            elLanguage.setText(language);
            metadata.getChildren().add(elLanguage);
        }
    }

    private void processManifest(Context context, Element opf) {
        Element manifest = JDOMUtils.findElement(opf, "manifest");
        JDOMFactory factory = context.getJDOMFactory();
        Parser parser = context.getParser();

        // Cover Image
        String imgsrc = parser.getProperty(Parser.PROPERTY_OPF_MANIFEST_COVER_IMAGE);
        IllegalStateAssertion.assertNotNull(imgsrc, "Cannot obtain property: " + Parser.PROPERTY_OPF_MANIFEST_COVER_IMAGE);
        String imgtype = parser.getProperty(Parser.PROPERTY_OPF_MANIFEST_COVER_IMAGE_TYPE);
        IllegalStateAssertion.assertNotNull(imgsrc, "Cannot obtain property: " + Parser.PROPERTY_OPF_MANIFEST_COVER_IMAGE_TYPE);
        Element item = factory.element("item");
        item.getAttributes().add(factory.attribute("id", "CoverImage"));
        item.getAttributes().add(factory.attribute("href", imgsrc));
        item.getAttributes().add(factory.attribute("properties", "cover-image"));
        item.getAttributes().add(factory.attribute("media-type", imgtype));
        manifest.getChildren().add(item);

        // Write sections
        Sections sections = context.getAttribute(Sections.class);
        if (sections != null) {
            for (Section section : sections) {
                item = factory.element("item");
                item.setAttribute("id", section.name);
                Path targetPath = IOUtils.bookRelative(context, section.target);
                item.setAttribute("href", targetPath.toString());
                if (section.isnav) {
                    item.setAttribute("properties", "nav");
                }
                item.setAttribute("media-type", "application/xhtml+xml");
                manifest.getChildren().add(item);
            }
        }

        // Write Content
        item = factory.element("item");
        item.setAttribute("id", "Content");
        Path targetPath = IOUtils.bookRelative(context, context.getTarget());
        item.setAttribute("href", targetPath.toString());
        item.setAttribute("media-type", "application/xhtml+xml");
        manifest.getChildren().add(item);
    }

    private void processSpine(Context context, Element opf) {
        Element spine = JDOMUtils.findElement(opf, "spine");
        JDOMFactory factory = context.getJDOMFactory();

        // Cover Image
        Element itemref = factory.element("itemref");
        itemref.setAttribute("idref", "CoverImage");
        spine.getChildren().add(itemref);

        // Write sections
        Sections sections = context.getAttribute(Sections.class);
        if (sections != null) {
            for (Section section : sections) {
                itemref = factory.element("itemref");
                itemref.setAttribute("idref", section.name);
                spine.getChildren().add(itemref);
            }
        }

        // Content
        itemref = factory.element("itemref");
        itemref.setAttribute("idref", "Content");
        spine.getChildren().add(itemref);
    }
    
    private void writeOPFDocument(Context context, Document doc) {
        Options options = context.getOptions();
        Path basedir = context.getBasedir();
        Path filePath = options.getOpfTarget();
        if (filePath == null) {
            String filename = context.getSource().getFileName().toString();
            filename = filename.substring(0, filename.lastIndexOf('.')) + ".opf";
            filePath = options.getBookDir().resolve(filename);
        }
        try {
            log.info("Writing OPF: {}", filePath);
            File outfile = basedir.resolve(filePath).toFile();
            outfile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(outfile);
            
            XMLOutputter xo = new XMLOutputter();
            Format format = Format.getPrettyFormat();
            xo.setFormat(format.setOmitDeclaration(false));
            format.setEncoding("UTF-8");
            xo.output(doc, fos);
            
            fos.close();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot write OPF file: " + filePath, ex);
        }
    }
}