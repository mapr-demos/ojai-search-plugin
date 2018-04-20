package com.mapr.ojai.search.client.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * The multi_match query builds on the match query to allow multi-field queries. Fields can be specified with
 * wildcards (eg: "*_name").
 */
public class MultiMatch implements OjaiSearchQuery {

    public enum Operator {
        AND,
        OR
    }

    private String[] fields;
    private Object text;
    private Operator operator;

    public MultiMatch(Object text, String... fields) {
        this(text, Operator.OR, fields);
    }

    public MultiMatch(Object text, Operator operator, String... fields) {
        this.fields = fields;
        this.text = text;
        this.operator = operator;
    }

    @Override
    public QueryBuilder query() {
        return QueryBuilders.multiMatchQuery(text, fields).operator(operator());
    }

    private org.elasticsearch.index.query.Operator operator() {
        return (this.operator == Operator.AND)
                ? org.elasticsearch.index.query.Operator.AND
                : org.elasticsearch.index.query.Operator.OR;
    }
}
