package com.suse.pase;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * Walks a directory (recursively), allowing a consumer to read any text file inside of it.
 * Also visits any archives found in the directory, up to a specified recursion level of nested archives, and allows the same consumer to read any text file in them.
 */
public class TextFileWalker {

    private static Logger LOG = Logger.getLogger(TextFileWalker.class.getName());
    private static byte[] buf = new byte[4096];
    private static final int RECURSION_LIMIT = 2;

    private final Path root;

    public TextFileWalker(Path path) {
        this.root = path;
    }

    /**
     * Calls the consumer for all text files in the directory, and also any text file in any archive found in the directory,
     * up to the specified recursion level of nested archives.
     * The consumer receives each file's path and a stream of its bytes.
     */
    public void withTextFilesIn(BiConsumer<Path, BufferedInputStream> consumer) {
        new DirectoryWalker(root).withFilesIn((path, stream) -> {
            if (isText(path, stream)) {
                consumer.accept(path, stream);
            }
            else {
                System.out.println("Processing: " + path);
                walkArchive(path, stream, RECURSION_LIMIT, consumer);
            }
        });
    }

    private void walkArchive(Path archivePath, BufferedInputStream archiveStream, int recursionLevel, BiConsumer<Path, BufferedInputStream> consumer) {
        new ArchiveWalker(archivePath, archiveStream).withFilesIn((path, stream) -> {
            if (isText(path, stream)) {
                consumer.accept(path, stream);
            }
            else if (recursionLevel > 1) {
                walkArchive(path, stream, recursionLevel - 1, consumer);
            }
        });
    }

    private boolean isText(Path path, BufferedInputStream stream) {
        // same heuristic used by diff
        // https://dev.to/sharkdp/what-is-a-binary-file-2cf5
        try {
            stream.mark(buf.length);
            var count = stream.read(buf, 0, buf.length);
            for (int i = 0; i < count; i++) {
                if (buf[i] == 0) {
                    stream.reset();
                    return false;
                }
            }
            stream.reset();
        }
        catch (IOException e) {
            LOG.warning("I/O error during text file check on " + path + ", skipping");
        }
        return true;
    }
}
