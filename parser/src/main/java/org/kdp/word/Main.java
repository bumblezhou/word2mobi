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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

/**
 *
 */
public class Main {

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        CmdLineParser cmdParser = new CmdLineParser(options);
        try {
            cmdParser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            helpScreen(cmdParser);
            return;
        }

        if (options.help) {
            helpScreen(cmdParser);
            return;
        }
        
        Parser parser = new ParserBuilder().build();
        File infile = options.arguments.get(0);
        String result = parser.process(infile);
        System.out.println(result);
    }

    private static void helpScreen(CmdLineParser cmdParser) {
        System.err.println("java -jar word2mobi.jar [options...] MyInput.html");
        cmdParser.printUsage(System.err);
    }

    static class Options {

        @Option(name = "--help", help = true )
        private boolean help;
        
        @Argument( usage = "MyInput.html", required = true, handler = FileOptionHandler.class )
        private List<File> arguments = new ArrayList<>();
    }
}
