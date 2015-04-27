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
package org.kdp.word;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.jdom2.Attribute;
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.jdom2.output.EscapeStrategy;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.kdp.word.utils.IllegalArgumentAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public final class Parser {

    private static Logger log = LoggerFactory.getLogger(Parser.class);
    
    public static final String SYSTEM_PROPERTY_CONFIGURATION = "word2mobi.configuration";
    public static final String CONFIGURATION_PROPERTIES = "word2mobi.properties";
    
    public static final String PROPERTY_ATTRIBUTE_REMOVE = "attribute.remove";
    public static final String PROPERTY_ATTRIBUTE_REPLACE = "attribute.replace";
    public static final String PROPERTY_ESCAPED_CHARS = "escaped.chars";
    public static final String PROPERTY_INPUT_CHARSET = "input.charset";
    public static final String PROPERTY_OUTPUT_ENCODING = "output.encoding";
    public static final String PROPERTY_OUTPUT_FORMAT = "output.format";
    public static final String PROPERTY_STYLE_REPLACE = "style.replace";
    public static final String PROPERTY_TRANSFORMER = "transformer";
    
    public static final String OUTPUT_FORMAT_COMPACT = "compact";
    public static final String OUTPUT_FORMAT_PRETTY = "pretty";

    private List<Transformer> transformers = new ArrayList<>();
    private Properties properties = new Properties();

    // hide ctor
    Parser() {
    }

    void init(Properties properties) {
        for (String name : getPropertyKeys(properties)) {
            String value = properties.getProperty((String) name);
            if (name.toString().startsWith(Parser.PROPERTY_TRANSFORMER)) {
                try {
                    addTransformer((Transformer) Class.forName(value).newInstance());
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                    throw new IllegalStateException("Cannot load transformer: " + value, ex);
                }
            } else {
                addProperty(name, value);
            }
        }
    }

    void addTransformer(Transformer tr) {
        transformers.add(tr);
    }

    void addProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public Set<String> getPropertyKeys() {
        return getPropertyKeys(properties);
    }

    private Set<String> getPropertyKeys(Properties props) {
        List<String> names = new ArrayList<>();
        for (Object name : props.keySet()) {
            names.add((String) name);
        }
        Collections.sort(names);
        return Collections.unmodifiableSet(new LinkedHashSet<String>(names));
    }

    public String process(File infile) throws SAXException, IOException {

        IllegalArgumentAssertion.assertNotNull(infile, "infile");
        log.info("Process: {}", infile);
        
        Path basedir = Paths.get(infile.toURI()).getParent();
        initDefaults(basedir);

        log.info("Using properties:");
        for (String key : getPropertyKeys()) {
            log.info(" " + key + " = " + getProperty(key));
        }
        
        // Parse input file to Document
        Document doc = parse(infile);

        // Transform the Document
        for (Transformer tr : transformers) {
            log.debug("Transforming with: {}", tr);
            tr.transform(this, basedir, doc.getRootElement());
        }

        // Write the document to stdout
        return writeDocument(doc);
    }

    /**
     * Parse the input file and return a well formed document
     */
    private Document parse(File infile) throws SAXException, IOException {

        final JDOMFactory factory = new DefaultJDOMFactory();
        final AtomicReference<Document> docref = new AtomicReference<>();

        String charset = getProperty(PROPERTY_INPUT_CHARSET);
        charset = charset != null ? charset : "UTF-8";
        
        InputStream inputStream = new FileInputStream(infile);
        Reader reader = new InputStreamReader(inputStream, charset);
        InputSource source = new InputSource(reader);
        source.setEncoding(charset);

        SAXParserImpl.newInstance(null).parse(source, new DefaultHandler() {

            Stack<Element> stack = new Stack<>();

            public void startElement(String uri, String localName, String name, Attributes inatts) {
                Element element = factory.element(localName.toLowerCase());
                List<Attribute> outatts = element.getAttributes();
                for (int i = 0; i < inatts.getLength(); i++) {
                    String att = inatts.getLocalName(i);
                    String val = inatts.getValue(i);
                    outatts.add(factory.attribute(att, val));
                }
                if (docref.get() == null) {
                    docref.set(factory.document(element));
                } else {
                    Element parent = stack.peek();
                    parent.getChildren().add(element);
                }
                stack.push(element);
            }

            public void endElement(String uri, String localName, String name) {
                stack.pop();
            }

            public void characters(char[] arr, int start, int length) {
                StringBuilder sb = new StringBuilder(length);
                for (int i = 0; i < length; i++) {
                    char ch = arr[start + i];
                    sb.append(ch);
                }
                Element parent = stack.peek();
                parent.addContent(sb.toString());
            }
        });
        return docref.get();
    }

    private void initDefaults(Path basedir) {
        File configFile = basedir.resolve(CONFIGURATION_PROPERTIES).toFile();
        log.debug("Search configuration: {}", configFile);
        if (configFile.isFile()) {
            log.info("Load configuration from: {}", configFile);
            Properties properties = new Properties();
            try {
                InputStream input = new FileInputStream(configFile);
                properties.load(input);
                input.close();
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot parse config", ex);
            }
            init(properties);
        }
    }
    
    private String writeDocument(Document doc) throws IOException {
        String outputEncoding = properties.getProperty(PROPERTY_OUTPUT_ENCODING);
        outputEncoding = outputEncoding != null ? outputEncoding : "UTF-8";
        String outputFormat = properties.getProperty(PROPERTY_OUTPUT_FORMAT);
        boolean pretty = OUTPUT_FORMAT_PRETTY.equals(outputFormat);

        XMLOutputter xo = new XMLOutputter();
        Format format = pretty ? Format.getPrettyFormat() : Format.getCompactFormat();
        format.setEncoding(outputEncoding);
        format.setEscapeStrategy(new OutputEscapeStrategy(format.getEscapeStrategy()));
        xo.setFormat(format.setOmitDeclaration(true));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xo.output(doc, baos);

        return new String(baos.toByteArray());
    }

    class OutputEscapeStrategy implements EscapeStrategy {
        private final EscapeStrategy encodingStrateg;
        private final Set<Integer> escaped = new HashSet<>();
        
        OutputEscapeStrategy(EscapeStrategy encodingStrategy) {
            this.encodingStrateg = encodingStrategy;
            String chars = getProperty(PROPERTY_ESCAPED_CHARS);
            if (chars != null) {
                for (String hexcode : chars.split(",")) {
                    escaped.add(Integer.decode(hexcode));
                }
            }
        }

        @Override
        public boolean shouldEscape(char ch) {
            Integer hexcode = new Integer(ch);
            if (escaped.contains(hexcode)) {
                return true;
            }
            return encodingStrateg.shouldEscape(ch);
        }
    }
}
