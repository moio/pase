import static java.util.Optional.ofNullable;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

class SourceCodeTokenizer extends CharTokenizer {
    public SourceCodeTokenizer() {
        super(DEFAULT_TOKEN_ATTRIBUTE_FACTORY, 1024);
    }

    @Override
    protected boolean isTokenChar(int cp) {
        return cp != 0x0D && cp != 0x0A;
    }
}

class SourceAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return new TokenStreamComponents(new SourceCodeTokenizer());
    }
}

public class Main {
    public static void main(String[] args) throws IOException {

        var sourcePath = ".";
        var indexPath = "index";
        var buffer = 256;

        var dir = FSDirectory.open(Paths.get(indexPath));

        var config = new IndexWriterConfig(new SourceAnalyzer())
                .setOpenMode(CREATE)
                .setRAMBufferSizeMB(buffer);

        try (var writer = new IndexWriter(dir, config)) {
            visitTextFilesIn(Path.of(sourcePath), path -> {
                indexFile(writer, path);
            });
        }
    }

    static void visitTextFilesIn(Path path, Consumer<Path> consumer) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                try {
                    var type = isText(path);
                    System.out.println(path + "  -->  " + type);
                    if (type) {
                        consumer.accept(path);
                    }
                }
                catch (IOException e) {
                    System.out.println("Not visiting: " + path);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static byte[] buf = new byte[1024];

    static boolean isText(Path path) throws IOException {
        // same heuristic used by diff
        // https://dev.to/sharkdp/what-is-a-binary-file-2cf5
        try (var file = new RandomAccessFile(path.toFile(), "r")) {
            var count = file.read(buf, 0, buf.length);
            for (int i = 0; i < count; i++) {
                if (buf[i] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Indexes a single document */
    static void indexFile(IndexWriter writer, Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            // make a new, empty document
            Document doc = new Document();

            // Add the path of the file as a field named "path"
            // This is indexed (i.e. searchable), but not tokenized
            doc.add(new StringField("path", file.toString(), Field.Store.YES));

            // Add the contents of the file to a field named "source"
            doc.add(new TextField("source", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

            // Create or update if existing
            writer.updateDocument(new Term("path", file.toString()), doc);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
