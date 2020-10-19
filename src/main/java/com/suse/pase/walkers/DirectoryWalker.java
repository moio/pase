package com.suse.pase.walkers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Walks a directory (recursively), allowing a consumer to read any file inside of it */
public class DirectoryWalker {

    private static final int BUFFER_SIZE = 4 * 1024 * 1024;

    private final Path path;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public DirectoryWalker(Path path){
        this.path = path;
    }

    /** Calls the consumer for all files in the directory. The consumer receives each file's path and a stream of its bytes. */
    public void withFilesIn(WalkerConsumer consumer) {
        try {
            final List<Callable<Object>> todo = new LinkedList<>();

            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile() && !attrs.isSymbolicLink()) {
                        todo.add(() -> {
                            try (var fileStream = Files.newInputStream(path);
                                 var stream = new BufferedInputStream(fileStream, BUFFER_SIZE)) {
                                consumer.accept(path, stream);
                            }
                            catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return Optional.empty();
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
