package com.suse.pase;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Encapsulates Lucene details about writing indexes */
public class IndexWriter implements AutoCloseable {

    org.apache.lucene.index.IndexWriter writer;

    /** Opens an index for writing at the specified path */
    public IndexWriter(Path path) throws IOException {
        var dir = FSDirectory.open(path);

        var config = new IndexWriterConfig(new SourceAnalyzer())
            .setOpenMode(CREATE)
            .setRAMBufferSizeMB(256);

        this.writer = new org.apache.lucene.index.IndexWriter(dir, config);
    }

    /** Adds a file to the index */
    public void add(Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            // make a new, empty document
            Document doc = new Document();

            // Add the path of the file as a field named "path"
            // This is indexed (i.e. searchable), but not tokenized
            doc.add(new StringField("path", file.toString(), Field.Store.YES));

            // Add the contents of the file to a field named "source"
            doc.add(new TextField("source", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

            // Create or update if existing
            this.writer.updateDocument(new Term("path", file.toString()), doc);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        this.writer.close();
    }

    /** Divides text by line (treats the whole line as a term). */
    private class SourceCodeTokenizer extends CharTokenizer {
        public SourceCodeTokenizer() {
            super(DEFAULT_TOKEN_ATTRIBUTE_FACTORY, 1024);
        }

        @Override
        protected boolean isTokenChar(int cp) {
            return cp != 0x0D && cp != 0x0A;
        }
    }

    /** Wraps a SourceCodeTokenizer and nothing else */
    private class SourceAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            return new TokenStreamComponents(new SourceCodeTokenizer());
        }
    }
}
