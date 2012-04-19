#!/bin/bash
# @author: rpetry
# @date:   11 Nov 2011

# This script measures web response times on a given hostname for selected Atlas queries

if [ $# -lt 1 ]; then
        echo "Usage: $0 HOSTNAME"
        exit;
fi

HOSTNAME=$1
ATLAS_ROOT_URL="http://${HOSTNAME}:8080/gxa"

echo "Web response times (secs) for ${ATLAS_ROOT_URL}:"
echo "-----------------------------------------------------------------------------------------------"
echo "query                                http_code    time_connect    time_starttransfer    time_total"
# Large gene-only query
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/qrs?gprop_0=&gnot_0=&gval_0=kinase" | awk -F":" '{print "qrs?gval_0=kinase&view=hm:           "$1"           "$2"           "$3"                "$4}'
# Large Condition-only query
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/qrs?fact_0=&fexp_0=UP_DOWN&fmex_0=&fval_0=brain&view=hm" | awk -F":" '{print "qrs?fval_0=brain&view=hm:            "$1"           "$2"          "$3"                 "$4}'
# Large list-view query
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/qrs?gprop_0=ensfamily_description&gnot_0=&gval_0=kinase&view=list" | awk -F":" '{print "qrs?gval_0=kinase&view=list:         "$1"           "$2"           "$3"                "$4}'
# E-MTAB-62 experiment page
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/experiment/E-MTAB-62" | awk -F":" '{print "experiment/E-MTAB-62:                "$1"           "$2"           "$3"                "$4}'
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/experimentDesign/E-MTAB-62" | awk -F":" '{print "experimentDesign/E-MTAB-62:          "$1"           "$2"           "$3"                "$4}'
# Complex API queries
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/api/v1?geneIs=cell+cycle" | awk -F":" '{print "api/v1?geneIs=cell+cycle:            "$1"           "$2"           "$3"                "$4}'
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/api/v1?geneIs=kinase" | awk -F":" '{print "api/v1?geneIs=kinase:                "$1"           "$2"           "$3"                "$4}'
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/api/v1?upDownIn=cancer+cell+line" | awk -F":" '{print "api/v1?upDownIn=cancer+cell+line:    "$1"           "$2"           "$3"                 "$4}'
curl -o /dev/null -X GET -s -w %{http_code}:%{time_connect}:%{time_starttransfer}:%{time_total} "${ATLAS_ROOT_URL}/api/v1?upDownIn=organism+part" | awk -F":" '{print "api/v1?upDownIn=organism+part:       "$1"           "$2"           "$3"                 "$4}'
echo ""
echo ""
