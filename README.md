# OJAI Search Plugin

## Design

![](docs/images/ojai-search-plugin-design.png?raw=true "Search plugin design")


OJAI Search Plugin consists of the following two components: Search Plugin Service and Search Client.
### Search Plugin Service

Kafka Consumer listens Change Data Records and updates indices at ElasticSearch. To make the installation easier it can 
be distributed as Docker image for example.

### Search Client

Must be included as dependency to Java application. Can be represented as single Java class. Usage example:
```

OjaiSearchClient clinet = new OjaiSearchClient("ojai:mapr:", "elastichostname:9300");
List<Document> documents = clinet.search("/apps/albums", "Immortal");

    ...

client.close();
```

Client makes search request to ElasticSearch, receives search results, that contain document's identifier and table path.
After that, client queries MapR-DB JSON Table via OJAI Driver to get actual JSON document. It can be published as Maven artifact or can be included to `mapr-ojai-driver` artifact.

Also, system which uses OJAI Search Plugin must have the following components:

* ElasticSearch

It can be separate ElasticSearch cluster or it can be an instance running on one of MapR Cluster's nodes.

* MapR-DB JSON Tables

MapR-DB JSON Tables store actual JSON document, which will be searched via the Plugin.

* Changelog

MapR cluster must be properly [configured](https://maprdocs.mapr.com/home/MapR-DB/DB-ChangeData/getting-started-cdc.html).
Tables must have the changelog relationship with the destination stream topic.
