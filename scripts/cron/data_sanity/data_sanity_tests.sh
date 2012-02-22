#!/bin/bash
# @author: rpetry
# @date:   16 Jan 2012

if [ $# -lt 3 ]; then
        echo "Usage: $0 NCDF_DIR NOTIFICATION_EMAILADDRESS ATLAS_URL"
        exit;
fi

NCDF_DIR=$1
NOTIFICATION_EMAILADDRESS=$2
ATLAS_URL=$3

log="/tmp/data_sanity_tests."`eval date +%Y%m%d`".log"

rm -rf $log

./find_no_stats_exps.sh $NCDF_DIR >> $log

./find_unused_properties_values.sh $ATLAS_URL >> $log

./find_near_duplicate_properties.sh $ATLAS_URL >> $log

if [ -e "$log" ]; then
    mailx -s "[gxa/cron] Atlas Data Sanity Tests for: "`date +'%d-%m-%Y'` ${NOTIFICATION_EMAILADDRESS} < $log
fi