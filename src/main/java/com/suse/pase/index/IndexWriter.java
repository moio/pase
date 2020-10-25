package com.suse.pase.index;

import static com.suse.pase.index.IndexCommons.FINGERPRINT_FIELD;
import static com.suse.pase.index.IndexCommons.LAST_UPDATED_FIELD;
import static com.suse.pase.index.IndexCommons.PATH_FIELD;
import static com.suse.pase.index.IndexCommons.SOURCE_FIELD;
import static java.util.Collections.singleton;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE_OR_APPEND;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;

import com.suse.pase.index.IndexCommons.SourceAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

/** Encapsulates Lucene details about writing indexes */
public class IndexWriter implements AutoCloseable {
    private static Logger LOG = Logger.getLogger(IndexWriter.class.getName());

    private final org.apache.lucene.index.IndexWriter writer;
    private final DirectoryReader reader;
    private final org.apache.lucene.search.IndexSearcher searcher;
    private final long lastUpdated;


    /** Opens an index for writing at the specified path */
    public IndexWriter(Path path) throws IOException {
        var config = new IndexWriterConfig(new SourceAnalyzer())
            .setOpenMode(CREATE_OR_APPEND)
            .setRAMBufferSizeMB(512);

        var dir = FSDirectory.open(path);
        writer = new org.apache.lucene.index.IndexWriter(dir, config);

        // read the index at the current point in time
        writer.commit();
        reader = DirectoryReader.open(dir);
        searcher = new org.apache.lucene.search.IndexSearcher(reader);

        lastUpdated = System.currentTimeMillis();
    }

    /**
     * Adds a file to the index.
     * @return true if the file was added the index, false if it was already known or in case of errors
     */
    public boolean add(String path, String fingerprint, Optional<BufferedInputStream> stream) {
        try {
            // if we already have this file and content did not change, mark it as updated
            var pathQuery = new PrefixQuery(new Term(PATH_FIELD, path));
            var fingerprintQuery = new TermQuery(new Term(FINGERPRINT_FIELD, fingerprint));

            var query = new BooleanQuery.Builder()
                    .add(pathQuery, MUST)
                    .add(fingerprintQuery, MUST)
                    .build();

            var results = searcher.search(query, Integer.MAX_VALUE);
            if (results.totalHits.value > 0) {
                for (int i = 0; i < results.scoreDocs.length; i++) {
                    var doc = searcher.doc(results.scoreDocs[i].doc, singleton(PATH_FIELD));
                    var docPath = doc.getField(PATH_FIELD).stringValue();
                    writer.updateNumericDocValue(new Term(PATH_FIELD, docPath), LAST_UPDATED_FIELD, lastUpdated);
                }
                return false;
            }

            // otherwise make a new, empty document
            Document doc = new Document();

            // Add the fingerprint of the file as an indexed (i.e. searchable), but not tokenized field
            doc.add(new StringField(FINGERPRINT_FIELD, fingerprint, Field.Store.YES));

            // Add the path of the file as an indexed (i.e. searchable), but not tokenized field
            doc.add(new StringField(PATH_FIELD, path, Field.Store.YES));

            // Add the last update time a DocValue field
            // Those are not indexed nor tokenized but they are updatable
            // This is needed for pruning, see close()
            doc.add(new NumericDocValuesField(LAST_UPDATED_FIELD, lastUpdated));
            //doc.add(new StoredField(LAST_UPDATED_FIELD + "stored", lastUpdated));

            // Add the contents of the file to a indexed and tokenized field
            stream.ifPresent(s -> {
                doc.add(new TextField(SOURCE_FIELD, new InputStreamReader(s, StandardCharsets.UTF_8){
                    @Override
                    public void close() throws IOException {
                        // we do not want Lucene to close the underlying stream
                    }
                }));
            });

            // Create or update if existing
            this.writer.addDocument(doc);
        }
        catch (IOException e) {
            LOG.warning("Could not index path, error during read: " + path);
            return false;
        }

        return true;
    }

    @Override
    public void close() throws Exception {
        // close reader first, so that writer closes faster
        reader.close();

        // prune
        writer.flush();
        writer.deleteDocuments(NumericDocValuesField.newSlowRangeQuery(LAST_UPDATED_FIELD, Long.MIN_VALUE, lastUpdated -1));

        // commit and close writer
        writer.close();
    }
}
