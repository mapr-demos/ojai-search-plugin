package com.mapr.ojai.search.client.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * The match_phrase_prefix is the same as match_phrase, except that it allows for prefix matches on the last term in
 * the text. In addition, it also accepts a max_expansions parameter (default 50) that can control to how many suffixes
 * the last term will be expanded. It is highly recommended to set it to an acceptable value to control the execution
 * time of the query.
 */
public class MatchPhrasePrefix implements OjaiSearchQuery {

    public static final int MAX_EXPANSIONS = 50;

    private String field;
    private Object text;
    private int maxExpansions;

    public MatchPhrasePrefix(String field, Object text) {
        this(field, text, MAX_EXPANSIONS);
    }

    public MatchPhrasePrefix(String field, Object text, int maxExpansions) {
        this.field = field;
        this.text = text;
        this.maxExpansions = maxExpansions;
    }

    @Override
    public QueryBuilder query() {
        return QueryBuilders.matchPhrasePrefixQuery(field, text).maxExpansions(maxExpansions);
    }
}
