package com.suse.pase.index;

import static com.suse.pase.index.IndexCommons.PATH_FIELD;
import static com.suse.pase.index.IndexCommons.SOURCE_FIELD;
import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import com.suse.pase.query.ByContentQuery;
import com.suse.pase.query.PatchQuery;
import com.suse.pase.query.QueryResult;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Encapsulates Lucene details about searching indexes */
public class IndexSearcher implements AutoCloseable {

    /** Maximum number of results per file */
    private final int HIT_LIMIT = 50;

    /**
     * Document frequency cutoff percentage, this is used to filter queries with over-popular results.
     *
     * We define a "cutoff score" as the score of a file with n lines, all of which are present in 10% of the indexed
     * files or more.
     *
     * Any result scoring worse than that will be cut off.
     */
    private static final double P = 0.1;

    /** Maximum number of unexpected lines between matching lines */
    private static final int SLOP = 3;

    private static Logger LOG = Logger.getLogger(IndexSearcher.class.getName());
    private final DirectoryReader reader;
    private final org.apache.lucene.search.IndexSearcher searcher;
    private final boolean explain;
    private final double minTermScore;

    public IndexSearcher(Path path, boolean explain) throws IOException {
        this.reader = DirectoryReader.open(FSDirectory.open(path));
        this.searcher = new org.apache.lucene.search.IndexSearcher(reader);
        // what we really want here is IDF, without any term frequency part
        // setting k1 = 0 (and b to any value) simplifies the formula to a pure IDF
        // https://en.wikipedia.org/wiki/Okapi_BM25
        this.searcher.setSimilarity(new BM25Similarity(0,0));
        this.explain = explain;

        // Calculate the score for a term present in P% of the documents
        // N is the total number of documents in the index
        var N = searcher.getIndexReader().getDocCount(PATH_FIELD);
        // now, per definition of BM25 with k1=0 we have
        // IDF=ln(((N-n) + 0.5) / (N + 0.5) +1)
        this.minTermScore = Math.log((N - P * N + 0.5)/ (P * N + 0.5) + 1);
    }

    /** Searches the index for files that match the patch target
     * @return a map from filename (as specified in the patch) to list of results
     */
    public Map<String, List<QueryResult>> search(List<PatchQuery> targets) {
        return targets.stream()
                .collect(toMap(
                        target -> target.getPath(),
                        target -> {
                            var query = buildQuery(target);
                            var termCount = target.getLineCount();
                            return search(query, termCount);
                        }
                ));
    }

    /** Searches the index for files with the same contents */
    public List<QueryResult> search(ByContentQuery patchableSite) {
        var query = buildQuery(patchableSite);
        return search(query, patchableSite.getContent().size());
    }

    private Query buildQuery(PatchQuery target) {
        // a query for a file SHOULD contain every chunk:
        //   - files with more matching chunks score higher
        //   - files no matching chunks are never returned
        BooleanQuery.setMaxClauseCount(1024*1024);
        return target.getChunks().stream()
                .map(this::buildQuery)
                .reduce(new BooleanQuery.Builder(), (builder, query) -> builder.add(query, SHOULD), (b1, b2) -> b2)
                .build();
    }

    private Query buildQuery(List<String> tokens) {
        // a query for a chunk SHOULD contain every line close to one another
        //   - files with matching lines close together will score higher
        //   - files without all the lines matching will not be returned
        //   - line ordering must be respected
        if (tokens.size() == 1) {
            return new SpanTermQuery(new Term(SOURCE_FIELD, tokens.get(0)));
        }
        return tokens.stream()
                .map(token -> new SpanTermQuery(new Term(SOURCE_FIELD, token)))
                .reduce(new SpanNearQuery.Builder(SOURCE_FIELD, true), SpanNearQuery.Builder::addClause, (b1, b2) -> b2)
                .setSlop(SLOP)
                .build();
    }

    private Query buildQuery(ByContentQuery file) {
        // a query for a file SHOULD contain every chunk
        BooleanQuery.setMaxClauseCount(1024*1024);
        return file.getContent().stream()
                .map(t -> new Term(SOURCE_FIELD, t))
                .reduce(new BooleanQuery.Builder(), (builder, term) -> builder.add(new TermQuery(term), SHOULD), (b1, b2) -> b2)
                .build();
    }

    /** Searches the index for a patch (list of chunks */
    private List<QueryResult> search(Query query, int termCount) {
        try {
            var results = searcher.search(query, HIT_LIMIT);

            return stream(results.scoreDocs)
                    .map(scoreDoc -> {
                        try {
                            var doc = searcher.doc(scoreDoc.doc, singleton(PATH_FIELD));
                            var path = doc.getField(PATH_FIELD).stringValue();
                            if (explain) {
                                LOG.info("Score explanation for: " + path + " (score: " + scoreDoc.score + ")");
                                LOG.info(searcher.explain(query, scoreDoc.doc).toString());
                            }
                            return new QueryResult(path, scoreDoc.score);
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(r -> r.score > minTermScore * termCount)
                    .collect(toList());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
