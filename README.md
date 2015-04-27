# Word2Mobi

An HTML processor that transforms content exported by MS Word such that it can be processed by [kindlegen](http://www.amazon.com/gp/feature.html?docId=1000765211).

## The Problem

For the master document, we like to use a word processor that allows online collaboration like [Google Docs](https://www.google.com/docs/about), which also supports export to 

* Microsoft Word (*.docx)
* Web Page (*.html)

Unfortunately both of these formats produce unusable kindle books when uploaded on [KDP](https://kdp.amazon.com). 

Amazon recomends to use MS Word to provide book content. I'm using a Mac with Word:Mac 2011, which also supports HTML export.

The resulting web HTML file (when exported with "Save only display information to HTML") is much more concise than what Google Docs produces.

However, the resulting Kindle book still has various issues:

* The Table of Content has no links
* Images are replaced and resized
* Font sizes are absolute
* Lists don't display properly

## The Solution

Before processing the HTML output with [kindlegen](http://www.amazon.com/gp/feature.html?docId=1000765211) we can run a small pre-processor (this tool) like this

```
> word2mobi MyBook.htm > MyBook-filtered.htm
```

The resulting (fitered) output should br ready for processing with [kindlegen](http://www.amazon.com/gp/feature.html?docId=1000765211).

```
> kindlegen MyBook-filtered.htm
```

You can now email `MyBook-filtered.mobi` to your Kindle Cloud storage or transfer it directly to your Kindle devices using USB. 

## Configuration

Word2Mobi uses the concept of pluggable transformers, which use simple property configuration like [this](/distro/etc/config/word2mobi.properties)

```
# Word2Mobi Properties

..

# Attribute removal
#attribute.remove.img.height = true
#attribute.remove.img.width = true

# Attribute replace
# attribute.replace.img.src.1 = generated/image001.jpg, images/Arabia600AD-550w.jpg
# attribute.replace.meta.content.1 = charset=macintosh, text/html;charset=utf-8

# Style replace
style.replace.p.MsoNormal = font-size:100%
style.replace.p.MsoFootnoteText = font-size:100%;text-align:left
style.replace.p.MsoListParagraph = font-size:100%;margin-left:0cm
style.replace.p.MsoQuote = font-size:100%
style.replace.p.MsoTitle = font-size:400%
style.replace.p.MsoToc1 = font-size:100%
style.replace.h1 = font-size:160%
style.replace.h2 = font-size:120%
style.replace.ul = margin-left:0.5cm;
```
