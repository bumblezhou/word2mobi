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

import java.nio.file.Path;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.JDOMFactory;

public interface Transformer {

    void transform(Context context);
    
    interface Context {
        
        /**
         * Get the JDOMFactory
         */
        JDOMFactory getJDOMFactory();
        
        /** 
         * Get the parser instance
         */
        Parser getParser();
        
        /** 
         * Get the parser options
         */
        Options getOptions();
        
        /**
         * Get the input base dir
         */
        Path getBasedir();
        
        /**
         * Get the path to the source document
         */
        Path getSource();
        
        /**
         * Get the path to the target document
         */
        Path getTarget();
        
        /**
         * Get the source root element
         */
        Element getSourceRoot();
        
        /**
         * Get a context attribute
         */
        <T> T getAttribute(Class<T> type);
        
        /**
         * Put a context attribute
         */
        <T> void putAttribute(Class<T> type, T value);
        
        /**
         * A map of arbitrary context attributes
         */
        Map<String, Object> getAttributes();
    }
}