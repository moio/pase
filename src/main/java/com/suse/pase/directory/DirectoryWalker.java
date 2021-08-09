package com.suse.pase.directory;

import static java.util.Optional.empty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/** Walks a directory, recursively, with many threads. */
public class DirectoryWalker {

    private static final int BUFFER_SIZE = 4 * 1024 * 1024;

    private final Path path;
    private final boolean followSymlinks;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public DirectoryWalker(Path path, boolean followSymlinks) {
        this.path = path;
        this.followSymlinks = followSymlinks;
    }

    /** Calls the consumer for all files in the directory. The consumer receives each file's path and a stream of its bytes. */
    public void walkFiles(BiConsumer<Path, BufferedInputStream> consumer) {
        try {
            final List<Callable<Object>> todo = new LinkedList<>();

            Set<FileVisitOption> fileVisitOptions = Collections.emptySet();
            if (followSymlinks) {
                fileVisitOptions = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
            }
            Files.walkFileTree(path, fileVisitOptions, 200, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile() && (followSymlinks || !attrs.isSymbolicLink())) {
                        todo.add(() -> {
                            try (var fileStream = Files.newInputStream(path);
                                 var stream = new BufferedInputStream(fileStream, BUFFER_SIZE)) {
                                consumer.accept(path, stream);
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

}
