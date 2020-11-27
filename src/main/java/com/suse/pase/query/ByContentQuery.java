package com.suse.pase.query;

import java.util.List;

/** A query to find indexed files with the same content (or close). */
public class ByContentQuery {
    private final List<String> content;

    public ByContentQuery(List<String> content) {
        this.content = content;
    }

    public List<String> getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ByContentQuery{" +
                "content=" + content +
                '}';
    }
}
