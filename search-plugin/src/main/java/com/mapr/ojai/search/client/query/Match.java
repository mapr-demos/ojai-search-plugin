package com.mapr.ojai.search.client.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Used for full text search on some field. Operator flag can be 'AND' or 'OR' to control the boolean clauses
 * (defaults to 'OR').
 */
public class Match implements OjaiSearchQuery {

    public enum Operator {
        AND,
        OR
    }

    private String field;
    private Object text;
    private Operator operator;

    public Match(String field, Object text) {
        this(field, text, Operator.OR);
    }

    public Match(String field, Object text, Operator operator) {
        this.field = field;
        this.text = text;
        this.operator = operator;
    }

    @Override
    public QueryBuilder query() {
        return QueryBuilders.matchQuery(field, text).operator(operator());
    }

    private org.elasticsearch.index.query.Operator operator() {
        return (this.operator == Operator.AND)
                ? org.elasticsearch.index.query.Operator.AND
                : org.elasticsearch.index.query.Operator.OR;
    }
}
