package com.suse.pase.walkers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Walks a directory (recursively), allowing a consumer to read any file inside of it */
public class DirectoryWalker {

    private static final int BUFFER_SIZE = 4 * 1024 * 1024;

    private final Path path;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public DirectoryWalker(Path path) {
        this.path = path;
    }

    /** Calls the consumer for all files in the directory. The consumer receives each file's path and a stream of its bytes. */
    public void withFilesIn(SimpleWalkerConsumer consumer) {
        try {
            final List<Callable<Object>> todo = new LinkedList<>();

            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile() && !attrs.isSymbolicLink()) {
                        todo.add(() -> {
                            try (var fileStream = Files.newInputStream(path);
                                 var stream = new BufferedInputStream(fileStream, BUFFER_SIZE)) {
                                consumer.accept(path, fingerprint(path), stream);
                            }
                            catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return empty();
                        });
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            executor.invokeAll(todo);
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String fingerprint(Path path) {
        // we assume contents changed based on filesystem metadata,
        // see rationale in https://apenwarr.ca/log/20181113
        try {
            Hasher hasher = Hashing.sha256().newHasher();
            var attributes = Files.readAttributes(path, PosixFileAttributes.class);

            // mtime
            hasher.putLong(attributes.lastModifiedTime().toMillis());
            // size
            hasher.putLong(attributes.size());
            // inode
            hasher.putString(attributes.fileKey().toString(), UTF_8);
            // mode
            for (var p : PosixFilePermission.values()){
                hasher.putBoolean(attributes.permissions().contains(p));
            }
            // owner
            hasher.putInt(attributes.owner().hashCode());
            // owner group
            hasher.putInt(attributes.group().hashCode());

            return hasher.hash().toString();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
