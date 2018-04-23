# Getting started

## Contents

* [Overview](#overview)
* [Setting up your environment](#setting-up-your-environment)
* [Configure and run OJAI Search Service](#configure-and-run-ojai-search-service)
* [Using OJAI Search Client](#using-ojai-search-client)
* [Dependencies](#dependencies)
* [Search Client Instantiating](#search-client-instantiating)
* [Supported Queries](#supported-queries)

## Overview

OJAI Search Plugin designated to allow users to perform full text search on documents stored at MapR-DB JSON Tables. 
It depends on ElasticSearch and consists of two components: Search Service and Search Client.
Search Service designated to listen Change Data Records of MapR-DB JSON Table and index these changes via ElasticSearch.
Search Client provides flexible API to search documents and retrieve them from JSON Table. Both Search Service and
Search Client represented as Java classes and contained in the single [search-plugin artifact](../search-plugin/pom.xml).

## Setting up your environment

* MapR Client

Application, which uses OJAJ Search Client must be run on the machine with MapR Client installed. It can be one of the 
MapR Cluster nodes or separate machine.
TODO

* Change Data Capture

[Change Data Capture](https://maprdocs.mapr.com/60/MapR-DB/DB-ChangeData/setting-up-CDC.html) must be configured. 
MapR-DB JSON Table with Changelog must exist. You can use 
[create-table-changelog.sh](../search-plugin/bin/create-table-changelog.sh) script to create a table with changelog:
```
Usage: create-table-changelog.sh [-p|--path] [-s|--stream] [-t|--topic] [-f|--force] [-h|--help]
Options:
    --path      Specifies path of table, which will be created.
    --stream     Specifies path of changelog stream, which will be created.
    --topic      Specifies changelog topic name, which will be created.
    --force      Forces table-changlog recreation if one of them or they both exists.
    --help       Prints usage information.
```

* ElasticSearch

ElasticSearch instance must be running and accessible from machine, on which OJAJ Search Service and Serch Client will 
be running. Refer [Installation](https://www.elastic.co/guide/en/elasticsearch/reference/current/_installation.html) 
page of ElasticSearch documentation.

## Configure and run OJAI Search Service

* Search Service Configuration

[config.yml](../search-plugin/src/main/resources/config.yml) configuration file declares
MapR-DB JSON tables(and corresponding changelogs) on which searching will be performed. You can specify path to 
`config.yml` as argument while running OJAI Search Service instance, otherwise `config.yml` from application's 
`resources` will be used.

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

* Build the project

To build the project with tests, run the commands:
```
$ cd ojai-search-plugin/
$ mvn clean install
```

Project contains the set of integration tests, which require access to properly configured MapR Cluster, existing table 
and changelog. Change test configuration file [config.yml](../search-plugin/src/test/resources/config.yml) to match 
your environment.

Also, you can build the project without tests by executing the following command:
```
$ mvn clean install -DskipTests
```

* Run the service

From project root directory run:
```
$ java -jar search-plugin/target/ojai-search-plugin.jar 
```

Also, your are free to specify path to service configuration file:
```
$ java -jar search-plugin/target/ojai-search-plugin.jar ~/config.yml
```

## Using OJAI Search Client

### Dependencies

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

### Search Client Instantiating

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

### Supported Queries

* Match query

Used for full text search on some field.

```
    // Full-text search on 'indexed_field' field using Match query
    DocumentStream found = searchClient.search("/apps/test_table", new Match("indexed_field", "entry"));
```

* MatchPhrase query

In the same way that the match query is the go-to query for standard full-text search, the match_phrase query is the 
one you should reach for when you want to find words that are near each other.
 
```
    // Full-text search on 'indexed_field' field using MatchPhrase query
    DocumentStream found = searchClient.search("/apps/test_table", new MatchPhrase("indexed_field", "Some text, which contains"));

```

* MatchPhrasePrefix query

The match_phrase_prefix is the same as match_phrase, except that it allows for prefix matches on the last term in 
the text. In addition, it also accepts a `max_expansions` parameter (default 50) that can control to how many suffixes 
the last term will be expanded. It is highly recommended to set it to an acceptable value to control the execution 
time of the query.

```
    // Full-text search on 'indexed_field' field using MatchPhrase query
    DocumentStream found = searchClient.search("/apps/test_table", new MatchPhrasePrefix("indexed_field", "Some t"));
```

* MultiMatch query

The multi_match query builds on the match query to allow multi-field queries. Fields can be specified with 
wildcards (eg: `*_name`).

```
    // Full-text search on 'indexed_field' field using MultiMatch query
    DocumentStream found = searchClient.search("/apps/test_table", new MultiMatch("entry", "indexed_field", "second_indexed_field"));
```

* QueryString query

A query that uses a query parser in order to parse its content. See 
[ElasticSearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html).

```
    // Full-text search on 'indexed_field' field using QueryString query
    DocumentStream found = searchClient.search("/apps/test_table", new QueryString("indexed_field", "(new york city) OR (search entry)"));
```
