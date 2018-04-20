package com.mapr.ojai.search.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mapr.db.exceptions.TableNotFoundException;
import com.mapr.ojai.search.client.query.*;
import com.mapr.ojai.search.config.SearchServiceConfig;
import com.mapr.ojai.search.config.TableConfig;
import com.mapr.ojai.search.service.SearchIndexService;
import com.mapr.streams.Admin;
import com.mapr.streams.Streams;
import org.apache.hadoop.conf.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class OjaiSearchClientIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OjaiSearchClientIntegrationTest.class);
    private static SearchServiceConfig config;
    private static TableConfig tableConfig;
    private static OjaiSearchClient ojaiSearchClient;
    private static ExecutorService executor;

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {

        config = SearchServiceConfig.fromResource("config.yml");

        if (config.getTables() == null || config.getTables().isEmpty()) {
            throw new IllegalStateException("Test config does not contain test table configuration");
        }

        if (config.getTables().size() > 1) {
            log.warn("Test config contains multiple tables configuration. Only the first one will be used during the test.");
        }

        if (!Utils.portListening(config.getElasticHost(), config.getElasticPort())) {
            throw new IllegalStateException("Elastic search at '" + config.getElasticHost() + "':'" +
                    config.getElasticPort() + "' is not running.");
        }

        ojaiSearchClient = new OjaiSearchClient("ojai:mapr:", config.getElasticHostPort());
        tableConfig = config.getTables().get(0);

        try {
            ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).findById("dummy");
        } catch (TableNotFoundException e) {
            throw new IllegalStateException("Test table '" + tableConfig.getPath() + "' does not exist. Use " +
                    "./bin/create-table-changelog.sh to create test table and changelog", e);
        }

        Admin admin = Streams.newAdmin(new Configuration());
        String changelogStreamName = tableConfig.getChangelog().split(":")[0];
        if (!admin.streamExists(changelogStreamName)) {
            throw new IllegalStateException("Test changelog stream '" + tableConfig.getChangelog() + "' does not exist. Use " +
                    "./bin/create-table-changelog.sh to create test table and changelog");
        }

        // Start search service to run in background
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> SearchIndexService.main(new String[]

                {
                }));

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

        String indexedField = UUID.randomUUID().toString();
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(), new Match("indexed_field", indexedField));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(indexedField, found.getString("indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldFindFullText() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();

        String indexedField = "Multi-word filed, on which searching will be performed. " + searchEntry + " Contains search entry.";
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(), new Match("indexed_field", searchEntry));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(indexedField, found.getString("indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldFindByMultipleEntries() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();

        String firstEntry = "Multi-word field, on which searching will be performed. ";
        String indexedField = firstEntry + searchEntry + " Contains search entry.";
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(), new Match("indexed_field", searchEntry + firstEntry));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(indexedField, found.getString("indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldFindByMatchWithANDOperator() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();

        String firstEntry = "Multi-word field, on which searching will be performed. ";
        String indexedField = firstEntry + searchEntry + " Contains search entry.";
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new Match("indexed_field", searchEntry + " Contains field entry search", Match.Operator.AND));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(indexedField, found.getString("indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldNotFindByMatchWithANDOperator() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();

        String firstEntry = "Multi-word field, on which searching will be performed. ";
        String indexedField = firstEntry + searchEntry + " Contains search entry.";
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new Match("indexed_field", searchEntry + " Contains NON-EXISTENT entry search", Match.Operator.AND));
        Iterator<Document> iterator = documentStream.iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldFindByMatchWithRedundantEntry() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();

        String firstEntry = "Multi-word field, on which searching will be performed. ";
        String indexedField = firstEntry + searchEntry + " Contains search entry.";
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new Match("indexed_field", searchEntry + " Contains NON-EXISTENT field entry search", Match.Operator.OR));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(indexedField, found.getString("indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldFindByPhrase() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();

        String firstEntry = "Multi-word field, on which searching will be performed. ";
        String indexedField = firstEntry + searchEntry + " Contains search entry.";
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new MatchPhrase("indexed_field", firstEntry + searchEntry));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(indexedField, found.getString("indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldNotFindByPhrase() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();

        String firstEntry = "Multi-word field, on which searching will be performed. ";
        String lastEntry = " Contains search entry.";
        String indexedField = firstEntry + searchEntry + lastEntry;
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new MatchPhrase("indexed_field", lastEntry + firstEntry + searchEntry));
        Iterator<Document> iterator = documentStream.iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldFindByPhrasePrefix() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(searchEntry, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new MatchPhrasePrefix("indexed_field", searchEntry.substring(0, 20)));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(searchEntry, found.getString("indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldFindByMultiMatch() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(searchEntry, "some", details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new MultiMatch(searchEntry, "indexed_field", "second_indexed_field"));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(searchEntry, found.getString("indexed_field"));
        assertEquals("some", found.getString("second_indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldFindByMultiMatchWildcard() throws InterruptedException {

        String searchEntry = UUID.randomUUID().toString();
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(searchEntry, "some", details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insert(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new MultiMatch(searchEntry, "*_field"));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(searchEntry, found.getString("indexed_field"));
        assertEquals("some", found.getString("second_indexed_field"));
        assertEquals(details, found.getString("details"));
    }

    @Test
    public void shouldFindByQueryString() throws InterruptedException {

        String indexedField = "this is a pretty big apple";
        String details = UUID.randomUUID().toString();

        Document testDoc = ojaiSearchClient.getConnection().newDocument(new TestDocument(indexedField, details));
        ojaiSearchClient.getConnection().getStore(tableConfig.getPath()).insertOrReplace(testDoc, "indexed_field");

        // Wait for ElasticSearch to create index
        Thread.sleep(3_000L);

        DocumentStream documentStream = ojaiSearchClient.search(tableConfig.getPath(),
                new QueryString( "indexed_field", "(new york city) OR (big apple)"));
        Iterator<Document> iterator = documentStream.iterator();
        assertTrue(iterator.hasNext());

        Document found = iterator.next();
        assertNotNull(found);
        assertEquals(indexedField, found.getString("indexed_field"));
        assertEquals(details, found.getString("details"));
    }
}

/**
 * Simple POJO for test purposes.
 */
class TestDocument {

    @JsonProperty("indexed_field")
    private String indexedField;

    @JsonProperty("second_indexed_field")
    private String secondIndexedField;

    private String details;

    public TestDocument() {
    }

    public TestDocument(String indexedField, String details) {
        this.indexedField = indexedField;
        this.details = details;
    }

    public TestDocument(String indexedField, String secondIndexedField, String details) {
        this.indexedField = indexedField;
        this.secondIndexedField = secondIndexedField;
        this.details = details;
    }

    public String getIndexedField() {
        return indexedField;
    }

    public void setIndexedField(String indexedField) {
        this.indexedField = indexedField;
    }

    public String getSecondIndexedField() {
        return secondIndexedField;
    }

    public void setSecondIndexedField(String secondIndexedField) {
        this.secondIndexedField = secondIndexedField;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}