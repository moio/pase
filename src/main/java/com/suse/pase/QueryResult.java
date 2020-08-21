package com.suse.pase;

public class QueryResult {
    private final String path;
    private final float score;


    public QueryResult(String path, float score) {
        this.path = path;
        this.score = score;
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "path='" + path + '\'' +
                ", score=" + score +
                '}';
    }
}
