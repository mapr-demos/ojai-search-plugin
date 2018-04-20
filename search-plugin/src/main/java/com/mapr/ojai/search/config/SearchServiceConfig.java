package com.mapr.ojai.search.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mapr.ojai.search.service.SearchIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SearchServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceConfig.class);

    @JsonProperty("elastic")
    private String elasticHostPort;
    private List<TableConfig> tables;
    private Map<String, String> tablePathChangelogCache = new HashMap<>();

    public String getElasticHostPort() {
        return elasticHostPort;
    }

    public void setElasticHostPort(String elasticHostPort) {
        this.elasticHostPort = elasticHostPort;
    }

    public String getElasticHost() {

        if (elasticHostPort == null || elasticHostPort.isEmpty()) {
            throw new IllegalStateException("Elastic host-port is empty");
        }

        if (!elasticHostPort.contains(":")) {
            throw new IllegalStateException("Elastic host-port is invalid: '" + elasticHostPort +
                    "'. Must be in 'host:port' format.");
        }

        return elasticHostPort.split(":")[0];
    }

    public int getElasticPort() {

        if (elasticHostPort == null || elasticHostPort.isEmpty()) {
            throw new IllegalStateException("Elastic host-port is empty");
        }

        if (!elasticHostPort.contains(":")) {
            throw new IllegalStateException("Elastic host-port is invalid: '" + elasticHostPort +
                    "'. Must be in 'host:port' format.");
        }

        try {
            return Integer.valueOf(elasticHostPort.split(":")[1]);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Elastic host-port is invalid: '" + elasticHostPort +
                    "'. Can not parse port as number.");
        }
    }

    public List<String> getChangelogs() {

        if (getTables() == null || getTables().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> validChangelogs = new ArrayList<>();
        for (TableConfig tableConfig : getTables()) {

            String changelog = tableConfig.getChangelog();
            if (changelog == null || changelog.isEmpty()) {
                log.warn("Table config for '" + tableConfig.getPath() + "' table has empty changelog. Ignoring it.");
                continue;
            }

            if (!changelog.startsWith("/") || !changelog.contains(":")) {
                log.warn("Table config for '" + tableConfig.getPath() + "' table has an invalid changelog: '" +
                        changelog + "'. Changelog must be in '/stream-name:topic-name' format. Ignoring it.");
                continue;
            }

            validChangelogs.add(changelog);
        }

        return validChangelogs;
    }

    public Optional<String> tablePathForChangelog(String changelog) {

        if (changelog == null || changelog.isEmpty()) {
            throw new IllegalArgumentException("Changelog can not be empty");
        }

        if (tablePathChangelogCache.containsKey(changelog)) {
            return Optional.of(tablePathChangelogCache.get(changelog));
        }

        if (getTables() == null || getTables().isEmpty()) {
            return Optional.empty();
        }

        Optional<String> optionalPath = getTables().stream()
                .filter(t -> changelog.equals(t.getChangelog()))
                .map(TableConfig::getPath)
                .findAny();

        // put to cache to prevent repeatable computations
        optionalPath.ifPresent(path -> tablePathChangelogCache.put(changelog, path));

        return optionalPath;
    }

    public List<TableConfig> getTables() {
        return tables;
    }

    public void setTables(List<TableConfig> tables) {
        this.tables = tables;
    }

    public static SearchServiceConfig fromResource(String resourceName) {

        if (resourceName == null || resourceName.isEmpty()) {
            throw new IllegalArgumentException("Config resource name can not be empty");
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(SearchIndexService.class.getClassLoader().getResource(resourceName), SearchServiceConfig.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not load config from resource: '" + resourceName + "'", e);
        }
    }

    public static SearchServiceConfig fromFile(String filePath) {

        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("Config file path can not be empty");
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(new File(filePath), SearchServiceConfig.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not load config from file: '" + filePath + "'", e);
        }
    }

    @Override
    public String toString() {
        return "SearchServiceConfig{" +
                "elasticHostPort='" + elasticHostPort + '\'' +
                ", tables=" + tables +
                '}';
    }
}
