package com.mapr.ojai.search.client.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * A query that uses a query parser in order to parse its content.
 * <br/>
 * <a href=https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html>ES documentation</a>
 */
public class QueryString implements OjaiSearchQuery {

    private String field;
    private String query;

    public QueryString(String field, String query) {
        this.field = field;
        this.query = query;
    }

    @Override
    public QueryBuilder query() {
        return QueryBuilders.queryStringQuery(query).field(field);
    }
}
