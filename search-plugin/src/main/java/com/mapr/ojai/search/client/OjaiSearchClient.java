package com.mapr.ojai.search.client;

import com.mapr.ojai.search.client.query.OjaiSearchQuery;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.ojai.DocumentStream;
import org.ojai.store.Connection;
import org.ojai.store.DriverManager;
import org.ojai.store.QueryCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OjaiSearchClient implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(OjaiSearchClient.class);

    private final Connection connection;
    private final TransportClient client;

    public OjaiSearchClient(Connection connection, String elasticHost, int elasticPort) {

        if (connection == null) {
            throw new IllegalArgumentException("OJAI connection can not be null");
        }

        this.connection = connection;
        this.client = createElasticSearchClient(elasticHost, elasticPort);
    }

    public OjaiSearchClient(Connection connection, String elasticHostPort) {

        if (elasticHostPort == null || elasticHostPort.isEmpty()) {
            throw new IllegalArgumentException("Elastic Search host-port string can not be empty");
        }

        if (!elasticHostPort.contains(":")) {
            throw new IllegalArgumentException("Invalid Elastic Search host-port string. Must be in 'host:port' format");
        }

        String[] hostPortPair = elasticHostPort.split(":");
        String host = hostPortPair[0];

        try {
            int port = Integer.valueOf(hostPortPair[1]);
            this.connection = connection;
            this.client = createElasticSearchClient(host, port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Elastic Search host-port string. Can not parse '" +
                    hostPortPair[1] + "' as port number");
        }
    }

    public OjaiSearchClient(String connectionURL, String elasticHost, int elasticPort) {

        if (connectionURL == null || connectionURL.isEmpty()) {
            throw new IllegalArgumentException("Connection URL can not be empty");
        }

        this.connection = DriverManager.getConnection(connectionURL);
        this.client = createElasticSearchClient(elasticHost, elasticPort);
    }

    public OjaiSearchClient(String connectionURL, String elasticHostPort) {

        if (connectionURL == null || connectionURL.isEmpty()) {
            throw new IllegalArgumentException("Connection URL can not be empty");
        }

        String[] hostPortPair = elasticHostPort.split(":");
        String host = hostPortPair[0];

        try {
            int port = Integer.valueOf(hostPortPair[1]);
            this.connection = DriverManager.getConnection(connectionURL);
            this.client = createElasticSearchClient(host, port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Elastic Search host-port string. Can not parse '" +
                    hostPortPair[1] + "' as port number");
        }
    }

    private TransportClient createElasticSearchClient(String elasticHost, int elasticPort) {

        if (elasticHost == null || elasticHost.isEmpty()) {
            throw new IllegalArgumentException("Elastic Search host can not be empty");
        }

        if (elasticPort <= 0) {
            throw new IllegalArgumentException("Elastic Search port number must be grater than zero");
        }

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(elasticHost);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }

        return new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(inetAddress, elasticPort));
    }

    public DocumentStream search(String tablePath, OjaiSearchQuery query) {

        if (tablePath == null || tablePath.isEmpty()) {
            throw new IllegalArgumentException("Table path can not be empty");
        }

        if (query == null) {
            throw new IllegalArgumentException("OJAI search query can not be null");
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(query.query()).fetchSource(false);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(sourceBuilder);

        SearchResponse response;
        try {
            response = client.search(searchRequest).get();

        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Can not get ES search response", e);
        }

        List<String> foundDocsIds = Stream.of(response.getHits().getHits())
                .map(SearchHit::getId)
                .collect(Collectors.toList());

        QueryCondition condition = connection.newCondition().in("_id", foundDocsIds);

        return connection.getStore(tablePath).find(condition.build());
    }

    /**
     * Returns OJAI connection, used by this instance of {@link OjaiSearchClient}.
     *
     * @return OJAI connection, used by this instance of {@link OjaiSearchClient}.
     */
    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
