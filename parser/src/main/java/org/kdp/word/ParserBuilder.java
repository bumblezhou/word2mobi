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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class ParserBuilder {
    
    private static Logger log = LoggerFactory.getLogger(Parser.class);
    
    private Properties properties = new Properties();
    private Parser parser = new Parser();
    
    public ParserBuilder() {
        try {
            String configLocation = System.getProperty(Parser.SYSTEM_PROPERTY_CONFIGURATION);
            if (configLocation != null) {
                log.info("Load configuration from: {}", configLocation);
                InputStream input = new URL(configLocation).openStream();
                try {
                    properties.load(input);
                } finally {
                    input.close();
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot parse config", ex);
        }
    }
    
    public ParserBuilder transformWith(Transformer transformer) {
        parser.addTransformer(transformer);
        return this;
    }
    
    public ParserBuilder compact() {
        properties.setProperty(Parser.PROPERTY_OUTPUT_FORMAT, Parser.OUTPUT_FORMAT_COMPACT);
        return this;
    }

    public ParserBuilder pretty() {
        properties.setProperty(Parser.PROPERTY_OUTPUT_FORMAT, Parser.OUTPUT_FORMAT_PRETTY);
        return this;
    }
    
    public Parser build() {
        parser.init(properties);
        return parser;
    }
}
