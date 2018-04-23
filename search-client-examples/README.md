# OJAI Search Client Examples

This sample application provides an example of usage of [OJAI Search Plugin](../search-plugin).

## Prerequisites

OJAI Search Plugin depends on ElasticSearch and consists of two components: Search Service and Search Client.
Search Service designated to listen Change Data Records of MapR-DB JSON Table and index these changes via ElasticSearch.
Search Client provides flexible API to search documents and retrieve them from JSON Table. Both Search Service and
Search Client represented as Java classes and contained in the single [search-plugin artifact](../search-plugin/pom.xml).

Thus, before using OJAI Search Client ensure that:
* MapR Cluster and your environment are properly configured

Application, which uses OJAJ Search Client must be run on the machine with MapR Client installed. It can be one of the 
MapR Cluster nodes or separate machine.
[Change Data Capture](https://maprdocs.mapr.com/60/MapR-DB/DB-ChangeData/setting-up-CDC.html) must be configured. Also,
MapR-DB JSON Table with Changelog must exist. You can use 
[create-table-changelog.sh](../search-plugin/bin/create-table-changelog.sh) script to create a table with changelog.

* OJAI Search Service properly configured and running

Change [config.yml](../search-plugin/src/main/resources/config.yml) configuration file and specify
MapR-DB JSON tables(and corresponding changelogs) on which searching will be performed.

```
# Specifies ElasticSearch location
elastic: localhost:9300

# List of MapR-DB JSON Tables, on which searching will be performed
tables:
  -
    path: /apps/test_table # table path
    changelog: /apps/test_changelog:test # changelog path
    indexedFields: # list of documents fields, which are allowed to be indexed(on which searching will be performed)
      - first_name
```

* ElasticSearch is running

## Dependencies

Declare `search-plugin` dependency in order to use OJAI Search Client from your application to search MapR-DB JSON
documents.
```
<dependencies>
    <dependency>
        <groupId>com.mapr.ojai.search</groupId>
        <artifactId>search-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Search Client Instantiating

Search Client instance must be obtained in one of few ways:

* From existing `Connection` instance

```
    Connection connection = DriverManager.getConnection("ojai:mapr:");

        ...

    // Instantiate OJAI Search Client from existing Connection instance
    OjaiSearchClient searchClient = new OjaiSearchClient(connection, ELASTIC_SEARCH_HOST_PORT)

        ...

    client.close();
```

* From connection URL

```
    // Instantiate OJAI Search Client from existing Connection instance
    OjaiSearchClient searchClient = new OjaiSearchClient("ojai:mapr:", ELASTIC_SEARCH_HOST_PORT)

        ...

    client.close();
```
## Sample Queries

* Match

Used for full text search on some field.

```
    // Full-text search on 'indexed_field' field using Match query
    DocumentStream found = searchClient.search("/apps/test_table", new Match("indexed_field", "entry"));
```

* MatchPhrase

In the same way that the match query is the go-to query for standard full-text search, the match_phrase query is the 
one you should reach for when you want to find words that are near each other.
 
```
    // Full-text search on 'indexed_field' field using MatchPhrase query
    DocumentStream found = searchClient.search("/apps/test_table", new MatchPhrase("indexed_field", "Some text, which contains"));

```

* MatchPhrasePrefix

The match_phrase_prefix is the same as match_phrase, except that it allows for prefix matches on the last term in 
the text. In addition, it also accepts a `max_expansions` parameter (default 50) that can control to how many suffixes 
the last term will be expanded. It is highly recommended to set it to an acceptable value to control the execution 
time of the query.

```
    // Full-text search on 'indexed_field' field using MatchPhrase query
    DocumentStream found = searchClient.search("/apps/test_table", new MatchPhrasePrefix("indexed_field", "Some t"));
```

* MultiMatch

The multi_match query builds on the match query to allow multi-field queries. Fields can be specified with 
wildcards (eg: `*_name`).

```
    // Full-text search on 'indexed_field' field using MultiMatch query
    DocumentStream found = searchClient.search("/apps/test_table", new MultiMatch("entry", "indexed_field", "second_indexed_field"));
```

* QueryString

A query that uses a query parser in order to parse its content. See 
[ElasticSearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html).

```
    // Full-text search on 'indexed_field' field using QueryString query
    DocumentStream found = searchClient.search("/apps/test_table", new QueryString("indexed_field", "(new york city) OR (search entry)"));
```

## Running examples

* Check [Prerequisites](#prerequisites) section

* Build the project

```
$ cd ojai-search-plugin/
$ mvn clean install
```

* Run one of the examples

```
$ java -cp search-client-examples/target/ojai-search-client-examples.jar com.mapr.ojai.examples.MatchExample
```
