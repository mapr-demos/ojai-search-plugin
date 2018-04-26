# Dockerization

OJAI Search Plugin repository contains [Dockerfile](../Dockerfile) which allows you to build and use Docker image for 
running OJAI Search Service. Image contains Elastic Search and OJAI Search Plugin Service itself.

## Build OJAI Search Plugin Docker image

Run the following commands in order to build Docker image:
```
$ cd ojai-search-plugin

$ mvn clean package

$ docker build -t ojai-search-plugin .

```

## Run container

Use command below to run OJAI Search Plugin container:
```
docker run -it \
    -e MAPR_CONTAINER_USER=mapr \
    -e MAPR_CONTAINER_GROUP=mapr \
    -e MAPR_CONTAINER_UID=5000 \
    -e MAPR_CONTAINER_GID=5000 \
    -e MAPR_CLDB_HOSTS=cldbhostname \
    -e MAPR_CLUSTER='my.cluster.com' \
    --net=host \
    ojai-search-plugin

```

In this case service will use default configuration file from [app's resources](../search-plugin/src/main/resources/config.yml).
You can pass configuration file to docker container using Docker volume:
```
docker run -it \
    -e MAPR_CONTAINER_USER=mapr \
    -e MAPR_CONTAINER_GROUP=mapr \
    -e MAPR_CONTAINER_UID=5000 \
    -e MAPR_CONTAINER_GID=5000 \
    -e MAPR_CLDB_HOSTS=cldbhostname \
    -e MAPR_CLUSTER='my.cluster.com' \
    --net=host \
    -v /absolute/local/path/to/config.yml:/usr/share/mapr-apps/ojai-search-plugin/config.yml \
    ojai-search-plugin

```
 
By default, OJAI Search Service will try to use configuration file, located at `/usr/share/mapr-apps/ojai-search-plugin/` 
directory. You can override it using `CONFIG_FILE_PATH` environment variable:

```
docker run -it \
    -e MAPR_CONTAINER_USER=mapr \
    -e MAPR_CONTAINER_GROUP=mapr \
    -e MAPR_CONTAINER_UID=5000 \
    -e MAPR_CONTAINER_GID=5000 \
    -e MAPR_CLDB_HOSTS=cldbhostname \
    -e MAPR_CLUSTER='my.cluster.com' \
    --net=host \
    -v /absolute/local/path/to/config.yml:/usr/config.yml \
    -e CONFIG_FILE_PATH=/usr/config.yml \
    ojai-search-plugin
```
