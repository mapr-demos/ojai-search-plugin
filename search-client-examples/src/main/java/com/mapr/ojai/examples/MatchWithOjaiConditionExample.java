package com.mapr.ojai.examples;

import com.mapr.ojai.search.client.OjaiSearchClient;
import com.mapr.ojai.search.client.query.Match;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.ojai.store.Connection;
import org.ojai.store.DriverManager;
import org.ojai.store.QueryCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MatchWithOjaiConditionExample {

    public static final String ELASTIC_SEARCH_HOST_PORT = "localhost:9300";

    private static Logger log = LoggerFactory.getLogger(MatchWithOjaiConditionExample.class);

    public static void main(String[] args) throws IOException {

        // Get OJAI connection
        Connection connection = DriverManager.getConnection("ojai:mapr:");

        // Working with MapR-DB JSON Documents
        Map<String, String> data = new HashMap<>();
        data.put("_id", UUID.randomUUID().toString());
        data.put("indexed_field", "Some text, which contains search entry");

        // Create new document
        Document doc = connection.newDocument(data);
        connection.getStore("/apps/test_table").insertOrReplace(doc);

        // Instantiate OJAI Search Client from existing Connection instance
        OjaiSearchClient searchClient = new OjaiSearchClient(connection, ELASTIC_SEARCH_HOST_PORT);

        // Full-text search on 'indexed_field' field using Match query and OJAI Condition
        DocumentStream found = searchClient
                .search("/apps/test_table", new Match("indexed_field", "entry"))
                .find(searchClient.getConnection().newCondition().is("_id", QueryCondition.Op.EQUAL, data.get("_id")));

        for (Document document : found) {
            log.info("Document found: {}", document);
        }

        searchClient.close();
    }
}
