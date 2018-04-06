package com.mapr.ojai.search.client.query;

import org.elasticsearch.index.query.QueryBuilder;

/**
 * SEARCH: size - The number of hits to return. Defaults to 10.
 * QUERY:
 * Leaf query clauses:
 * ===========================
 * 1. match: Used for full text search on some field.
 * operator flag can be 'and' OR 'or'.
 * <p>
 * 2. match_phrase
 * <p>
 * 3. match_phrase_prefix
 * <p>
 * 4. multi_match
 * "query": {
 * "multi_match" : {
 * "query":    "Will Smith",
 * "fields": [ "title", "*_name" ]
 * }
 * }
 * <p>
 * 5. query_string
 * <p>
 * "query_string" : {
 * "default_field" : "content",
 * "query" : "(new york city) OR (big apple)"
 * }
 * <p>
 * ?? simple_query_string
 * <p>
 * <p>
 * <p>
 * term: finds documents that contain the exact term specified in the inverted index. When querying full text fields,
 * use the match query instead. TODO: DO NOT USE IT AT ALL? ! IT"S NOT OBVIOUS, SINCE ANALYZER WILL PROCESS INDEXED DOCS
 * <p>
 * range:
 * <p>
 *
 */
public interface OjaiSearchQuery {

    QueryBuilder query();

}
