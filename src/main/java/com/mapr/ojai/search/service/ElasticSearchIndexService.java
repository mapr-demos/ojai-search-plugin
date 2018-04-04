package com.mapr.ojai.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mapr.ojai.search.config.SearchServiceConfig;
import com.mapr.ojai.search.config.TableConfig;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class ElasticSearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndexService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final SearchServiceConfig config;
    private final TransportClient client;

    public ElasticSearchIndexService(SearchServiceConfig config) {

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(config.getElasticHost());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }

        // Create ElasticSearch Client
        this.config = config;
        this.client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(inetAddress, config.getElasticPort()));
    }

    /**
     * Should be called on document insert/update.
     *
     * @param tablePath
     * @param documentId
     * @param changes
     */
    public void saveIndexForTable(String tablePath, String documentId, JsonNode changes) {

        String indexName = tablePathToIndexName(tablePath);
        // Create index
        try {
            IndicesExistsResponse existsResponse = client.admin().indices().prepareExists(indexName).get();
            if (!existsResponse.isExists()) {
                client.admin().indices().prepareCreate(indexName).get();
            }
        } catch (Exception e) {
            log.info("Can not create ElasticSearch index:'" + indexName + "'", e);
        }

        Set<String> allowedFields = getAllowedFieldsForTable(tablePath);
        JsonNode allowed = copyOnlyAllowedFields(allowedFields, changes);

        if (allowed == null) {
            log.info("Document with id: '{}' was changed, but none of the fields are allowed to be sent to the ES",
                    documentId);
            return;
        }

        IndexResponse response = client.prepareIndex(indexName, tablePath, documentId)
                .setSource(allowed.toString(), XContentType.JSON)
                .get();

        log.info("Elasticsearch Index Response: '{}'", response);
    }

    /**
     * Should be called on document delete.
     *
     * @param documentId
     */
    public void deleteIndexForTable(String tablePath, String documentId) {

        String indexName = tablePathToIndexName(tablePath);
        DeleteResponse response = client.prepareDelete(indexName, tablePath, documentId).get();
        log.info("Elasticsearch Delete Response: '{}'", response);
    }

    /**
     * Only specified fields will be sent to the ElasticSearch.
     *
     * @param original all the changes.
     * @return changes for the specified fields.
     */
    private JsonNode copyOnlyAllowedFields(Set<String> allowedFields, JsonNode original) {

        ObjectNode allowed = null;
        Iterator<String> fieldNamesIterator = original.fieldNames();
        while (fieldNamesIterator.hasNext()) {

            String fieldName = fieldNamesIterator.next();
            if (allowedFields != null && !allowedFields.isEmpty() && !allowedFields.contains(fieldName)) {
                continue;
            }

            if (allowed == null) {
                allowed = mapper.createObjectNode();
            }

            allowed.set(fieldName, original.get(fieldName));
        }

        return allowed;
    }

    private Set<String> getAllowedFieldsForTable(String tablePath) {

        if (config.getTables() == null || config.getTables().isEmpty()) {
            return Collections.emptySet();
        }

        Optional<TableConfig> optionalTable = config.getTables().stream()
                .filter(t -> tablePath.equals(t.getPath()))
                .findAny();

        if (!optionalTable.isPresent()) {
            return Collections.emptySet();
        }

        return optionalTable.get().getIndexedFields();
    }

    /**
     * Replaces all invalid characters from table path with underscore.
     *
     * @param tablePath
     * @return
     */
    private String tablePathToIndexName(String tablePath) {

        // , ", *, \, <, |, ,, >, /, ? - Elastic Search index name can not contain these chars
        String replaced = tablePath.replaceAll("[*,\"/\\\\<>|?]", "_");
        return (replaced.startsWith("_")) ? replaced.substring(1) : replaced;
    }

}
