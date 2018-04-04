package com.mapr.ojai.search.config;

import java.util.Set;

public class TableConfig {

    private String path;
    private String changelog;
    private Set<String> indexedFields;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public Set<String> getIndexedFields() {
        return indexedFields;
    }

    public void setIndexedFields(Set<String> indexedFields) {
        this.indexedFields = indexedFields;
    }

    @Override
    public String toString() {
        return "TableConfig{" +
                "path='" + path + '\'' +
                ", changelog='" + changelog + '\'' +
                ", indexedFields=" + indexedFields +
                '}';
    }
}
