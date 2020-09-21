package com.suse.pase;

public class QueryResult {
    final String path;
    final float score;


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
