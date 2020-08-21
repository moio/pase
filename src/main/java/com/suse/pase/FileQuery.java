package com.suse.pase;

import java.util.List;

/** A query for a file */
class FileQuery {
    private final String file;
    private final List<String> chunks;

    public FileQuery(String file, List<String> chunks) {
        this.file = file;
        this.chunks = chunks;
    }

    public String getFile() {
        return file;
    }

    public List<String> getChunks() {
        return chunks;
    }

    @Override
    public String toString() {
        return "FileQuery{" +
                "file='" + file + '\'' +
                ", chunks=" + chunks +
                '}';
    }
}
