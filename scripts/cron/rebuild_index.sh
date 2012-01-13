#!/bin/bash
# @author: rpetry
# @date:   14 Oct 2011

# This script serves to re-build index for all experiments that need it on server ATLAS_URL:ATLAS_PORT/ATLAS_ROOT. using admin credentials: ADMIN_USERNAME/ADMIN_PASSWORD
# It assumes that calculate analytics step has been run before and was successful. If it finds any experiments with incomplete analytics it emails an error report to ERROR_NOTIFICATION_EMAILADDRESS and quits

if [ $# -eq 0 ]; then
        echo "Usage: $0 ADMIN_USERNAME ADMIN_PASSWORD ATLAS_URL ATLAS_PORT ATLAS_ROOT ERROR_NOTIFICATION_EMAILADDRESS FORCE_REBUILD"
        echo "e.g. $0 admin <pwd> lime 14032 gxa-load atlas-developers@ebi.ac.uk"
        exit;
fi

ADMIN_USERNAME=$1
ADMIN_PASSWORD=$2
ATLAS_URL=$3
ATLAS_PORT=$4
ATLAS_ROOT=$5
ERROR_NOTIFICATION_EMAILADDRESS=$6
# if == "force" the rebuild index on wwwdev irrespective of if any experiments need re-indexing - in preparation for the release
FORCE_REBUILD=$7

process_data="${ATLAS_URL}.${ATLAS_PORT}.`eval date +%Y%m%d`"
process_file="/tmp/process_experiments.$process_data"
authentication_cookie=$process_file.$$

# login to Atlas admin
curl -X GET -c $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=login&userName=${ADMIN_USERNAME}&password=${ADMIN_PASSWORD}&indent" >> /dev/null

# cut commands extract e.g. 0 from {"numTotal":0,
num_incomplete_analytics=`curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=searchexp&pendingOnly=INCOMPLETE_ANALYTICS" | cut -d'{' -f2 | cut -d':' -f2 | cut -d',' -f1`

if [ $num_incomplete_analytics != "0" ]; then
    # If the number of experiments with incomplete analytics is not 0, email an error to ERROR_NOTIFICATION_EMAILADDRESS and quit
    curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=searchexp&pendingOnly=INCOMPLETE_ANALYTICS&indent" 2>&1 >> $process_file.log
    mailx -s "Processing experiments on ${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT} failed due to incomplete analytics (see message body)" ${ERROR_NOTIFICATION_EMAILADDRESS} < $process_file.log
else
    num_incomplete_index=`curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=searchexp&pendingOnly=INCOMPLETE_INDEX" | cut -d'{' -f2 | cut -d':' -f2 | cut -d',' -f1`
    if [ ${FORCE_REBUILD} == "force" -o $num_incomplete_index != "0" ]; then
        echo "Number of experiments with incomplete index = $num_incomplete_index - re-building the index..." >> $process_file.log
        curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=schedule&runMode=RESTART&type=index&autoDepends=false&accession=&indent" 2>&1 >> $process_file.log
    else
        echo "No experiments needed indexing - not re-building the index" >> $process_file.log
    fi
fi

# logout from Atlas admin
curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=logout&indent" >> /dev/null

rm -rf $authentication_cookie

