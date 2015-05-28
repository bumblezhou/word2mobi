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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.kdp.word.Transformer.Context;
import org.kdp.word.utils.IOUtils;
import org.kdp.word.utils.IllegalArgumentAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class Parser {

    private static Logger log = LoggerFactory.getLogger(Parser.class);
    
    public static final String SYSTEM_PROPERTY_CONFIGURATION = "word2mobi.configuration";
    public static final String CONFIGURATION_PROPERTIES = "word2mobi.properties";
    
    public static final String PROPERTY_ATTRIBUTE_REPLACE = "attribute.replace";
    public static final String PROPERTY_ESCAPED_CHARS = "escaped.chars";
    public static final String PROPERTY_INPUT_CHARSET = "input.charset";
    public static final String PROPERTY_OPF_MANIFEST_COVER_IMAGE = "opf.manifest.cover.image";
    public static final String PROPERTY_OPF_MANIFEST_COVER_IMAGE_TYPE = "opf.manifest.cover.image.type";
    public static final String PROPERTY_OPF_METADATA_AUTHOR = "opf.metadata.author";
    public static final String PROPERTY_OPF_METADATA_LANGUAGE = "opf.metadata.language";
    public static final String PROPERTY_OPF_METADATA_TITLE = "opf.metadata.title";
    public static final String PROPERTY_OUTPUT_ENCODING = "output.encoding";
    public static final String PROPERTY_OUTPUT_FORMAT = "output.format";
    public static final String PROPERTY_STYLE_REPLACE = "style.replace";
    public static final String PROPERTY_STYLE_REPLACE_WHITELIST = "style.replace.whitelist";
    public static final String PROPERTY_TRANSFORMER = "transformer";
    
    public static final String OUTPUT_FORMAT_COMPACT = "compact";
    public static final String OUTPUT_FORMAT_PRETTY = "pretty";

    private final List<Transformer> transformers = new ArrayList<>();
    private final Properties properties = new Properties();
    private final Options options;
    
    Parser(Options options) {
        this.options = options;
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
        
        final Path source = Paths.get(infile.toURI());
        final Path basedir = source.getParent();
        initDefaults(basedir);

        log.debug("Using properties:");
        for (String key : getPropertyKeys()) {
            log.debug(" " + key + " = " + getProperty(key));
        }
        
        // Parse input file to Document
        final JDOMFactory factory = new DefaultJDOMFactory();
        final Document doc = parseHTML(factory, infile);

        Context context = new Context() {
            
            private Map<String, Object> attributes = new HashMap<>();
            
            @Override
            public JDOMFactory getJDOMFactory() {
                return factory;
            }

            @Override
            public Parser getParser() {
                return Parser.this;
            }

            @Override
            public Options getOptions() {
                return options;
            }

            @Override
            public Path getBasedir() {
                return basedir;
            }

            @Override
            public Path getSource() {
                return source;
            }

            @Override
            public Path getTarget() {
                Path outpath = options.getOutput();
                if (outpath == null) {
                    String fname = source.getFileName().toString();
                    fname = fname.substring(0, fname.lastIndexOf('.')) + ".html";
                    outpath = Paths.get(fname);
                }
                return outpath;
            }

            @Override
            public Element getSourceRoot() {
                return doc.getRootElement();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getAttribute(Class<T> type) {
                return (T) getAttributes().get(type.getName());
            }

            @Override
            public <T> void putAttribute(Class<T> type, T value) {
                attributes.put(type.getName(), value);
            }

            @Override
            public Map<String, Object> getAttributes() {
                return attributes;
            }
        };
        
        // Transform the Document
        for (Transformer tr : transformers) {
            log.debug("Transforming with: {}", tr);
            tr.transform(context);
        }

        // Get the result
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.writeDocument(context, doc, baos);
        String result = new String(baos.toByteArray());
        
        // Write output file 
        File outfile = options.getBookDir().resolve(context.getTarget()).toFile();
        outfile.getParentFile().mkdirs();
        log.debug("Writing output to: {}", outfile);
        FileOutputStream fos = new FileOutputStream(outfile);
        IOUtils.writeDocument(context, doc, fos);
        fos.close();
        
        return result;
    }

    /**
     * Parse the input file and return a well formed document
     */
    private Document parseHTML(final JDOMFactory factory, final File infile) throws SAXException, IOException {

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
            log.debug("Load configuration from: {}", configFile);
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
}
