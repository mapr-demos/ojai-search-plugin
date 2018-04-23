package com.mapr.ojai.examples;

import com.mapr.ojai.search.client.OjaiSearchClient;
import com.mapr.ojai.search.client.query.QueryString;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.ojai.store.Connection;
import org.ojai.store.DriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QueryStringExample {

    public static final String ELASTIC_SEARCH_HOST_PORT = "localhost:9300";

    private static Logger log = LoggerFactory.getLogger(QueryStringExample.class);

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

        // Full-text search on 'indexed_field' field using QueryString query
        DocumentStream found = searchClient.search("/apps/test_table",
                new QueryString("indexed_field", "(new york city) OR (search entry)"));

        for (Document document : found) {
            log.info("Document found: {}", document);
        }

        searchClient.close();
    }
}
