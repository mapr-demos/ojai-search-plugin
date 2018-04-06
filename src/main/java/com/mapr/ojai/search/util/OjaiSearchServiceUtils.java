package com.mapr.ojai.search.util;

public final class OjaiSearchServiceUtils {

    private OjaiSearchServiceUtils() {
    }

    /**
     * Replaces all invalid characters from table path with underscore.
     *
     * @param tablePath
     * @return
     */
    public static String tablePathToIndexName(String tablePath) {

        // , ", *, \, <, |, ,, >, /, ? - Elastic Search index name can not contain these chars
        String replaced = tablePath.replaceAll("[*,\"/\\\\<>|?]", "_");
        return (replaced.startsWith("_")) ? replaced.substring(1) : replaced;
    }

}
