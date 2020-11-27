package com.suse.pase.query;

import java.util.Collection;
import java.util.List;

/** A query to find indexed files on which a patch can be applied (or almost). */
public class PatchTargetQuery {
    private final String path;
    private final List<List<String>> chunks;

    public PatchTargetQuery(String path, List<List<String>> chunks) {
        this.path = path;
        this.chunks = chunks;
    }

    public String getPath() {
        return path;
    }

    public List<List<String>> getChunks() {
        return chunks;
    }

    /** Returns the total count of lines in all chunks. */
    public int getLineCount() { return chunks.stream().mapToInt(Collection::size).sum(); }

    @Override
    public String toString() {
        return "PatchTargetQuery{" +
                "name='" + path + '\'' +
                ", chunks=" + chunks +
                '}';
    }
}
