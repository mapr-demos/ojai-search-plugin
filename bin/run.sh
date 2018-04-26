#!/bin/bash

#######################################################################
# Globals definition
#######################################################################
ELASTIC_SEARCH_PORT=9300
WORK_DIR=/usr/share/mapr-apps/ojai-search-plugin
DEFAULT_CONFIG_FILE_PATH=/usr/share/mapr-apps/ojai-search-plugin/config.yml

# Check if 'CONFIG_FILE_PATH' environment variable set
if [ ! -z ${CONFIG_FILE_PATH+x} ]; then # CONFIG_FILE_PATH exists
    echo "Config file path: $CONFIG_FILE_PATH"
else
    CONFIG_FILE_PATH=${DEFAULT_CONFIG_FILE_PATH}
    echo "Config file path is not specified. Using default path: $DEFAULT_CONFIG_FILE_PATH"
fi

# Change permissions
sudo chown -R ${MAPR_CONTAINER_USER}:${MAPR_CONTAINER_GROUP} ${WORK_DIR}
sudo chown -R ${MAPR_CONTAINER_USER}:${MAPR_CONTAINER_GROUP} ${CONFIG_FILE_PATH}

# Start ElasticSearch
${WORK_DIR}/elasticsearch-5.6.1/bin/elasticsearch >> elastic.log &

# Wait for ElasticSearch to start
GREP_ES_PORT_PROC_NUM=$(ps -aux | grep ${ELASTIC_SEARCH_PORT} | wc -l)
while [ ${GREP_ES_PORT_PROC_NUM} -gt 0 ]
do
    sleep 1
    GREP_ES_PORT_PROC_NUM=$(ps -aux | grep ${ELASTIC_SEARCH_PORT} | wc -l)
done

# Start OJAI Search Plugin Service
if [ -f "$CONFIG_FILE_PATH" ]
then
    echo "Starting OJAI Search service with config file '$CONFIG_FILE_PATH'"
    java -jar ${WORK_DIR}/ojai-search-plugin.jar ${CONFIG_FILE_PATH} &
else
    echo "Config file '$CONFIG_FILE_PATH' does not exists. Using default config file from app resources"
    java -jar ${WORK_DIR}/ojai-search-plugin.jar &
fi

sleep infinity
