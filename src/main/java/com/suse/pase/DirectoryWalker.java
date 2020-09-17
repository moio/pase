package com.suse.pase;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;

/** Utils to walk directory trees */
public class DirectoryWalker {

    /** Allows to consume an InputStream for all text files found in a path (recursively) */
    public static void forEachTextFileIn(Path path, BiConsumer<String, InputStream> consumer) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (attrs.isRegularFile() && !attrs.isSymbolicLink() && isText(path)) {
                    try (InputStream stream = Files.newInputStream(path)) {
                        consumer.accept(path.toString(), stream);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static byte[] buf = new byte[4096];

    private static boolean isText(Path path) throws IOException {
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
}
