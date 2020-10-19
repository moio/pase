package com.suse.pase.index;

import java.io.BufferedInputStream;
import java.util.Optional;

@FunctionalInterface
/** A function that can walk a single path. */
public interface WalkerConsumer {
    /**
     * Walk a single path.
     *
     * @param path the path to walk
     * @param fingerprint a String that changes whenever contents in path changes (between program invocations)
     *                    note that this could be a hash of the contents, but that is not necessarily required
     * @param stream the stream of bytes associated with path
     * @return true if the content is new to the index, false if it was already present
     */
    boolean add(String path, String fingerprint, Optional<BufferedInputStream> stream);
}
