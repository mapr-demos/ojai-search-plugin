package com.mapr.ojai.search.service;

import com.mapr.ojai.search.config.SearchServiceConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.ojai.store.cdc.ChangeDataRecord;
import org.ojai.store.cdc.ChangeDataRecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);

    private static final long CONSUMER_POLL_TIMEOUT = 500L;
    private static final Properties consumerProperties = new Properties();

    static {
        consumerProperties.setProperty("group.id", "ojai-search-service");
        consumerProperties.setProperty("enable.auto.commit", "true");
        consumerProperties.setProperty("auto.offset.reset", "latest");
        consumerProperties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        consumerProperties.setProperty("value.deserializer", "com.mapr.db.cdc.ChangeDataRecordDeserializer");
    }

    /**
     * Consumer used to consume MapR-DB CDC events.
     */
    private static final KafkaConsumer<byte[], ChangeDataRecord> consumer = new KafkaConsumer<>(consumerProperties);

    public static void main(String[] args) {

        SearchServiceConfig config = (args.length > 0)
                ? SearchServiceConfig.fromFile(args[0])
                : SearchServiceConfig.fromResource("config.yml");

        consumer.subscribe(config.getChangelogs());
        ChangeDataRecordHandler cdcHandler = new ChangeDataRecordHandler(config);
        while (true) {

            ConsumerRecords<byte[], ChangeDataRecord> changeRecords = consumer.poll(CONSUMER_POLL_TIMEOUT);
            for (ConsumerRecord<byte[], ChangeDataRecord> consumerRecord : changeRecords) {

                // The ChangeDataRecord contains all the changes made to a document
                ChangeDataRecord changeDataRecord = consumerRecord.value();
                ChangeDataRecordType recordType = changeDataRecord.getType();
                switch (recordType) {
                    case RECORD_INSERT:
                        cdcHandler.handleInsert(consumerRecord);
                        break;
                    case RECORD_UPDATE:
                        cdcHandler.handleUpdate(consumerRecord);
                        break;
                    case RECORD_DELETE:
                        cdcHandler.handleDelete(consumerRecord);
                        break;
                    default:
                        log.warn("Get record of unknown type '{}'. Ignoring ...", recordType);
                }
            }
        }
    }

}
