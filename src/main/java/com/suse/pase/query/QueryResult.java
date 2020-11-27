package com.suse.pase.query;

public class QueryResult {
    public final String path;
    public final float score;


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
