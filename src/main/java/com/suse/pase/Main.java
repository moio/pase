import com.suse.pase.DirectoryWalker;
import com.suse.pase.IndexWriter;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {

        var sourcePath = Path.of(".");
        var indexPath = Path.of("index");

        try (var writer = new IndexWriter(indexPath)) {
            DirectoryWalker.forEachTextFileIn(sourcePath, path -> {
                writer.add(path);
            });
        }
    }
}
