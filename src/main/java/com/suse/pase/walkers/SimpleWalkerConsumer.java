package com.suse.pase.walkers;

import java.io.BufferedInputStream;
import java.nio.file.Path;

@FunctionalInterface
public interface SimpleWalkerConsumer {
    /**
     * Walk a single path.
     *
     * @param path the path to walk
     * @param fingerprint a String that changes whenever contents in path changes (between program invocations)
     *                    note that this could be a hash of the contents, but that is not necessarily required
     * @param stream the stream of bytes associated with path
     */
    void accept(Path path, String fingerprint, BufferedInputStream stream);
}
