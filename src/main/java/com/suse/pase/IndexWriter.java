package com.suse.pase;

import static com.suse.pase.IndexCommons.PATH_FIELD;
import static com.suse.pase.IndexCommons.SOURCE_FIELD;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;

import com.suse.pase.IndexCommons.SourceAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Encapsulates Lucene details about writing indexes */
public class IndexWriter implements AutoCloseable {

    private final org.apache.lucene.index.IndexWriter writer;

    /** Opens an index for writing at the specified path */
    public IndexWriter(Path path) throws IOException {
        var dir = FSDirectory.open(path);

        var config = new IndexWriterConfig(new SourceAnalyzer())
            .setOpenMode(CREATE)
            .setRAMBufferSizeMB(512);

        this.writer = new org.apache.lucene.index.IndexWriter(dir, config);
    }

    /** Adds a file to the index */
    public void add(String path, InputStream stream) {
        try {
            // make a new, empty document
            Document doc = new Document();

            // Add the path of the file as a field named "path"
            // This is indexed (i.e. searchable), but not tokenized
            doc.add(new StringField(PATH_FIELD, path, Field.Store.YES));

            // Add the contents of the file to a field named "source"
            doc.add(new TextField(SOURCE_FIELD, new InputStreamReader(stream, StandardCharsets.UTF_8){
                @Override
                public void close() throws IOException {
                    // we do not want Lucene to close the underlying stream
                }
            }));

            // Create or update if existing
            this.writer.updateDocument(new Term(PATH_FIELD, path), doc);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        this.writer.close();
    }


}
