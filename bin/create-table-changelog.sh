#!/bin/bash

#######################################################################
# Functions definition
#######################################################################

function print_usage() {
    cat <<EOM
Usage: $(basename $0) [-p|--path] [-s|--stream] [-t|--topic] [-f|--force] [-h|--help]
Options:
    --path      Specifies path of table, which will be created.
    --stream     Specifies path of changelog stream, which will be created.
    --topic      Specifies changelog topic name, which will be created.
    --force      Forces table-changlog recreation if one of them or they both exists.
    --help       Prints usage information.
EOM
}

function create_table() {

    TABLE_NAME=$1
    RECREATE=$2

    EXISTS=0
    maprcli table info -path ${TABLE_NAME} > /dev/null
    EXISTS=$?

    if [ "$EXISTS" -eq 0 ] && [ "$RECREATE" -eq 0 ]; then
        echo "error: Table '$TABLE_NAME' already exists. Exiting. "
        echo "Please, specify '[-f|--force]' if you want to recreate it."
        cleanup
        exit 1
    fi

    maprcli table delete -path ${TABLE_NAME} > /dev/null
    maprcli table create -path ${TABLE_NAME} -tabletype json
    OUT=$?
    if [ $OUT -eq 0 ];then
        echo "Table '$TABLE_NAME' successfully created!"
    else
        echo "error: Errors occurred while creating '$USERNAME' table. Exiting."
        cleanup
        exit 1
    fi
}

function change_table_permissions() {
    local TABLE_NAME=$1
    maprcli table cf edit -path ${TABLE_NAME} -cfname default -readperm p -writeperm p -traverseperm  p
}

function create_changelog_stream() {

    local STREAM_PATH=$1
    local RECREATE=$2

    EXISTS=0
    maprcli stream info -path ${STREAM_PATH} > /dev/null
    EXISTS=$?

    if [ "$EXISTS" -eq 0 ] && [ "$RECREATE" -eq 0 ]; then
        echo "error: Stream '$STREAM_PATH' already exists. Exiting. "
        echo "Please, specify '[-f|--force]' if you want to recreate it."
        exit 1
    fi

    maprcli stream delete -path ${STREAM_PATH} > /dev/null
    maprcli stream create -path ${STREAM_PATH} -ischangelog true
    OUT=$?
    if [ $OUT -eq 0 ];then
        echo "Changelog stream '$STREAM_PATH' successfully created!"
    else
        echo "error: Errors occurred while creating '$STREAM_PATH' changelog stream. Exiting."
        exit 1
    fi
}

function change_stream_permissions() {
    local STREAM_PATH=$1
    maprcli stream edit -path ${STREAM_PATH} -produceperm p -consumeperm p -topicperm p -copyperm p -adminperm p
}

#######################################################################
# Parse options
#######################################################################

OPTS=`getopt -o hfp:s:t: --long help,force,path:,stream:,topic: -n 'create-table-changelog.sh' -- "$@"`
eval set -- "$OPTS"

TABLE_PATH=''
STREAM_PATH=''
TOPIC_NAME=''
FORCE_RECREATE=0
while true ; do
    case "$1" in
        -p|--path)
            case "$2" in
                "") shift 2 ;;
                *) TABLE_PATH=$2 ; shift 2 ;;
            esac ;;
        -s|--stream)
            case "$2" in
                "") shift 2 ;;
                *) STREAM_PATH=$2 ; shift 2 ;;
            esac ;;
        -t|--topic)
            case "$2" in
                "") shift 2 ;;
                *) TOPIC_NAME=$2 ; shift 2 ;;
            esac ;;
        -f|--force) FORCE_RECREATE=1 ; shift ;;
        -h|--help) print_usage ; exit 0 ;;
        --) shift ; break ;;

        *) break ;;
    esac
done

if [[ -z "${TABLE_PATH}" ]]; then
 echo 'error: Table path can not be empty'
 exit 1
fi

if [[ -z "${STREAM_PATH}" ]]; then
 echo 'error: Changelog stream path can not be empty'
 exit 1
fi

if [[ -z "${TOPIC_NAME}" ]]; then
 echo 'error: Changelog topic name can not be empty'
 exit 1
fi

create_table ${TABLE_PATH} ${FORCE_RECREATE}
change_table_permissions ${TABLE_PATH}

create_changelog_stream ${STREAM_PATH} ${FORCE_RECREATE}
change_stream_permissions ${STREAM_PATH}

maprcli table changelog add -path ${TABLE_PATH} -changelog ${STREAM_PATH}:${TOPIC_NAME}
OUT=$?
if [ $OUT -eq 0 ];then
    echo "Changelog '$STREAM_PATH:$TOPIC_NAME' successfully added to table '$TABLE_PATH'!"
else
    echo "error: Errors occurred while adding changelog '$STREAM_PATH:$TOPIC_NAME' to table '$TABLE_PATH'. Exiting."
    exit 1
fi

exit 0
