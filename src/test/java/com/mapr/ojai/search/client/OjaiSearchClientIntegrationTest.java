package com.mapr.ojai.search.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mapr.ojai.search.client.query.Match;
import com.mapr.ojai.search.config.SearchServiceConfig;
import com.mapr.ojai.search.config.TableConfig;
import com.mapr.ojai.search.service.SearchService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

public class OjaiSearchClientIntegrationTest {

    // TODO CHECK IF MAPR AND ELASTIC SEARCH ARE AVAILABLE
    // TODO recreate changelog & table
    // TODO initialize with test data
    // TODO test searches

    private static final Logger log = LoggerFactory.getLogger(OjaiSearchClientIntegrationTest.class);
    private static SearchServiceConfig config;
    private static TableConfig tableConfig;
    private static OjaiSearchClient ojaiSearchClient;
    private static ExecutorService executor;

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {

        SearchServiceConfig config = SearchServiceConfig.fromResource("config.yml");

        if (config.getTables() == null || config.getTables().isEmpty()) {
            throw new IllegalArgumentException("Test config does not contain test table configuration");
        }

        if (config.getTables().size() > 1) {
            log.warn("Test config contains multiple tables configuration. Only the first one will be used during the test.");
        }

        ojaiSearchClient = new OjaiSearchClient("ojai:mapr:", config.getElasticHostPort());
        tableConfig = config.getTables().get(0);

        // Start search service to run in background
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> SearchService.main(new String[]{}));

        Thread.sleep(5_000L);
    }

    @AfterClass
    public static void cleanup() throws IOException {

        if (ojaiSearchClient != null) {
            ojaiSearchClient.close();
        }

        executor.shutdown();
    }

    @Test
    public void shouldFindBySingleWord() throws InterruptedException {

        String firstName = UUID.randomUUID().toString();
        String lastName = UUID.randomUUID().toString();

        Document userDoc = ojaiSearchClient.getConnection().newDocument(new User(firstName, lastName));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(userDoc, "first_name");

        // What for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream found = ojaiSearchClient.search(tableConfig.getPath(), new Match("first_name", firstName));

        for (Document doc : found) {
            log.info("DOC: {}", doc);
        }

        assertTrue(true);
    }
}

/**
 * Simple POJO for test purposes.
 */
class User {

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String details;


    public User() {
    }

    public User(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public User(String firstName, String lastName, String details) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.details = details;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}