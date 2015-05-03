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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.output.EscapeStrategy;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.kdp.word.Options;
import org.kdp.word.Parser;
import org.kdp.word.Transformer.Context;

public final class IOUtils {

    // hide ctor
    private IOUtils() {
    }
    
    public static void writeDocument(Context context, Document doc, OutputStream out) throws IOException {
        Parser parser = context.getParser();
        String outputEncoding = parser.getProperty(Parser.PROPERTY_OUTPUT_ENCODING);
        outputEncoding = outputEncoding != null ? outputEncoding : "UTF-8";
        String outputFormat = parser.getProperty(Parser.PROPERTY_OUTPUT_FORMAT);
        boolean pretty = Parser.OUTPUT_FORMAT_PRETTY.equals(outputFormat);

        XMLOutputter xo = new XMLOutputter();
        Format format = pretty ? Format.getPrettyFormat() : Format.getCompactFormat();
        format.setEncoding(outputEncoding);
        EscapeStrategy strategy = new OutputEscapeStrategy(context, format.getEscapeStrategy());
        format.setEscapeStrategy(strategy);
        xo.setFormat(format.setOmitDeclaration(true));
        xo.output(doc, out);
    }

    public static Path bookRelative(Context context, Path targetPath) {
        Options options = context.getOptions();
        if (targetPath.startsWith(options.getBookDir())) {
            int index = options.getBookDir().toString().length();
            String relativePath = targetPath.toString();
            relativePath = relativePath.substring(index + 1, relativePath.length());
            targetPath = Paths.get(relativePath);
        }
        return targetPath;
    }

    static class OutputEscapeStrategy implements EscapeStrategy {
        private final EscapeStrategy encodingStrateg;
        private final Set<Integer> escaped = new HashSet<>();
        
        OutputEscapeStrategy(Context context, EscapeStrategy encodingStrategy) {
            this.encodingStrateg = encodingStrategy;
            Parser parser = context.getParser();
            String chars = parser.getProperty(Parser.PROPERTY_ESCAPED_CHARS);
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
