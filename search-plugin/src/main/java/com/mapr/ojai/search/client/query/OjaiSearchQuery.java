package com.mapr.ojai.search.client.query;

import org.elasticsearch.index.query.QueryBuilder;

public interface OjaiSearchQuery {

    QueryBuilder query();

}
