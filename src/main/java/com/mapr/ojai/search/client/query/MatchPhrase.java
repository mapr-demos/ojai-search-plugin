package com.mapr.ojai.search.client.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * In the same way that the match query is the go-to query for standard full-text search, the match_phrase query is the
 * one you should reach for when you want to find words that are near each other.
 */
public class MatchPhrase implements OjaiSearchQuery {

    private String field;
    private Object text;

    public MatchPhrase(String field, Object text) {
        this.field = field;
        this.text = text;
    }

    @Override
    public QueryBuilder query() {
        return QueryBuilders.matchPhraseQuery(field, text);
    }
}
