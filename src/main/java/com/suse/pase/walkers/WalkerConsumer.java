package com.suse.pase.walkers;

import java.io.BufferedInputStream;
import java.nio.file.Path;

@FunctionalInterface
public interface WalkerConsumer {
    /**
     * Walk a single path.
     *
     * @param path the path to walk
     * @param stream the stream of bytes associated with path
     */
    void accept(Path path, BufferedInputStream stream);
}
