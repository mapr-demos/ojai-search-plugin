package com.mapr.ojai.search.client.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class Match implements OjaiSearchQuery {

    private String name;
    private Object text;

    public Match(String name, Object text) {
        this.name = name;
        this.text = text;
    }

    @Override
    public QueryBuilder query() {
        return QueryBuilders.matchQuery(name, text);
    }
}
