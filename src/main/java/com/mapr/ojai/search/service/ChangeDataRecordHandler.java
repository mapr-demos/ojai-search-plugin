package com.mapr.ojai.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mapr.ojai.search.config.SearchServiceConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ojai.FieldPath;
import org.ojai.KeyValue;
import org.ojai.Value;
import org.ojai.store.cdc.ChangeDataRecord;
import org.ojai.store.cdc.ChangeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class ChangeDataRecordHandler {

    private static final Logger log = LoggerFactory.getLogger(ChangeDataRecordHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final SearchServiceConfig config;
    private final ElasticSearchIndexService searchService;

    public ChangeDataRecordHandler(SearchServiceConfig config) {
        this.config = config;
        this.searchService = new ElasticSearchIndexService(config);
    }

    public void handleInsert(ConsumerRecord<byte[], ChangeDataRecord> consumerRecord) {

        ChangeDataRecord changeDataRecord = consumerRecord.value();
        String documentId = changeDataRecord.getId().getString();
        log.info("Inserted document with id = '{}'", documentId);

        Optional<String> optionalTablePath = config.tablePathForChangelog(consumerRecord.topic());
        if (!optionalTablePath.isPresent()) {
            log.warn("Can not get table path for changelog: '{}'. Ignoring insert change data record for document: {}",
                    consumerRecord.topic(), documentId);
            return;
        }

        String tablePath = optionalTablePath.get();
        Iterator<KeyValue<FieldPath, ChangeNode>> iterator = changeDataRecord.iterator();
        if (!iterator.hasNext()) {
            log.warn("Insert Change Data Record received with no change nodes. Ignoring ...");
            return;
        }

        Map.Entry<FieldPath, ChangeNode> changeNodeEntry = iterator.next();
        ChangeNode changeNode = changeNodeEntry.getValue();
        if (changeNode == null) {
            log.warn("Insert Change Data Record received with 'null' change node. Ignoring ...");
            return;
        }

        Value changeNodeValue = changeNode.getValue();
        if (changeNodeValue == null) {
            log.warn("Insert Change Data Record received with 'null' change node value. Ignoring ...");
            return;
        }

        String jsonString = changeNodeValue.asJsonString();

        searchService.saveIndexForTable(tablePath, documentId, parseJsonString(jsonString));
    }

    public void handleUpdate(ConsumerRecord<byte[], ChangeDataRecord> consumerRecord) {

        ChangeDataRecord changeDataRecord = consumerRecord.value();
        String documentId = changeDataRecord.getId().getString();
        log.info("Updated document with id = '{}'", documentId);

        Optional<String> optionalTablePath = config.tablePathForChangelog(consumerRecord.topic());
        if (!optionalTablePath.isPresent()) {
            log.warn("Can not get table path for changelog: '{}'. Ignoring update change data record for document: {}",
                    consumerRecord.topic(), documentId);
            return;
        }

        String tablePath = optionalTablePath.get();
        ObjectNode changes = mapper.createObjectNode();
        for (Map.Entry<FieldPath, ChangeNode> changeNodeEntry : changeDataRecord) {

            ChangeNode changeNode = changeNodeEntry.getValue();
            String jsonString = changeNode.getValue().asJsonString();
            String fieldPathAsString = changeNodeEntry.getKey().asPathString();
            changes.set(fieldPathAsString, parseJsonString(jsonString));
        }

        searchService.saveIndexForTable(tablePath, documentId, changes);
    }

    public void handleDelete(ConsumerRecord<byte[], ChangeDataRecord> consumerRecord) {

        ChangeDataRecord changeDataRecord = consumerRecord.value();
        String deletedDocumentId = changeDataRecord.getId().getString();
        log.info("Deleted document with id = '{}'", deletedDocumentId);

        Optional<String> optionalTablePath = config.tablePathForChangelog(consumerRecord.topic());
        if (!optionalTablePath.isPresent()) {
            log.warn("Can not get table path for changelog: '{}'. Ignoring delete change data record for document: {}",
                    consumerRecord.topic(), deletedDocumentId);
            return;
        }

        String tablePath = optionalTablePath.get();
        searchService.deleteIndexForTable(tablePath, deletedDocumentId);
    }

    private JsonNode parseJsonString(String jsonString) {

        JsonNode node = null;
        try {
            node = mapper.readValue(jsonString, JsonNode.class);
        } catch (IOException e) {
            log.warn("Can not parse JSON string '{}' as instance of Jackson JsonNode", jsonString);
        }

        return (node != null) ? node : mapper.createObjectNode();
    }

}
