package org.kdp.word;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

public final class Options {

    @Option(name = "--help", help = true)
    boolean help;

    @Option(name = "--bookdir", usage = "Path to the output book directory")
    private Path bookdir = Paths.get("book").toAbsolutePath();

    @Option(name = "--output", usage = "Path to the processed output")
    private Path output;
    
    @Option(name = "--opf", usage = "Path to the generaged OPF file")
    private Path opfTarget;

    @Option(name = "--css", usage = "Path to an external CSS file")
    private Path externalCSS;

    @Option(name = "--opf-template", usage = "Path to the OPF template")
    private Path opfTemplate = Paths.get("opf-template.xml");

    @Argument(hidden = true, handler = FileOptionHandler.class)
    List<File> arguments = new ArrayList<>();
    
    public Path getOutput() {
        return output;
    }

    void setOutput(Path output) {
        this.output = output.isAbsolute() ? output : bookdir.resolve(output);
    }

    public Path getBookDir() {
        return bookdir;
    }

    void setBookDir(Path bookdir) {
        this.bookdir = bookdir.toAbsolutePath();
    }

    public Path getOpfTarget() {
        return opfTarget;
    }

    void setOpfTarget(Path target) {
        this.opfTarget = target.isAbsolute() ? target : bookdir.resolve(target);
    }

    public Path getOpfTemplate() {
        return opfTemplate;
    }

    void setOpfTemplate(Path template) {
        this.opfTemplate = template;
    }

    public Path getExternalCSS() {
        return externalCSS;
    }

    void setExternalCSS(Path css) {
        this.externalCSS = css;
    }

    void helpScreen(CmdLineParser cmdParser) {
        System.err.println("java -jar word2mobi.jar [options...] MyInput.html");
        cmdParser.printUsage(System.err);
    }
}