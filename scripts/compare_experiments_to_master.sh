#!/bin/bash
# @author: rpetry
# @date:   21 Jan 2012

# This script compares experiments in local Atlas instance to that in the master Atlas instance

if [ $# -lt 2 ]; then
        echo "Usage: $0 MASTER_ATLAS_URL LOCAL_ATLAS_URL (NOTIFICATION_EMAILADDRESS)"
        echo "e.g. $0 http://www.ebi.ac.uk/gxa http://localhost:8080/gxa <notifcation_email_address>"
        exit 1
fi

MASTER_ATLAS_URL=$1
LOCAL_ATLAS_URL=$2
NOTIFICATION_EMAILADDRESS=$3

MASTER_ATLAS_VERSION=`curl -s -X GET "${MASTER_ATLAS_URL}/webping" | awk -F', ' '{print $2}'`
LOCAL_ATLAS_VERSION=`curl -s -X GET "${LOCAL_ATLAS_URL}/webping" | awk -F', ' '{print $2}'`

process_file="/tmp/compare_experiments_to_master."`eval date +%Y%m%d`
log=$process_file".log"

echo "Comparing software versions in ${MASTER_ATLAS_URL} and ${LOCAL_ATLAS_URL} ..." > $log
# Proceed only if local and master versions agree
if [ "$MASTER_ATLAS_VERSION" != "$LOCAL_ATLAS_VERSION" ]; then
    echo "Cannot compare experiments as master Atlas version: '$MASTER_ATLAS_VERSION' differs from local Atlas version: $LOCAL_ATLAS_VERSION" > $log
    if [ ! -z $NOTIFICATION_EMAILADDRESS ]; then
        mailx -s "Experiments comparison between (master) ${MASTER_ATLAS_URL} and (local) ${LOCAL_ATLAS_URL} failed: "`date +'%d-%m-%Y'` ${NOTIFICATION_EMAILADDRESS} < $log
    fi
    exit 1
fi

# Retrieve master Atlas digests
curl -s -X GET "${MASTER_ATLAS_URL}/api?experiment=listAll&experimentInfoOnly&indent" | grep '"digest"' | awk -F" : " '{print $2}' | sed 's/[",]//g' > $process_file.master.digests
curl -s -X GET "${MASTER_ATLAS_URL}/api?experiment=listAll&experimentInfoOnly&indent" | grep '"accession"' | grep 'E-' | awk -F" : " '{print $2}' | sed 's/[",]//g' > $process_file.master.experiments
paste ${process_file}.master.experiments ${process_file}.master.digests > ${process_file}.master

# Retrieve local Atlas digests
curl -s -X GET "${LOCAL_ATLAS_URL}/api?experiment=listAll&experimentInfoOnly&indent" | grep '"digest"' | awk -F" : " '{print $2}' | sed 's/[",]//g' > $process_file.local.digests
curl -s -X GET "${LOCAL_ATLAS_URL}/api?experiment=listAll&experimentInfoOnly&indent" | grep '"accession"' | grep 'E-' | awk -F" : " '{print $2}' | sed 's/[",]//g' > $process_file.local.experiments
paste ${process_file}.local.experiments ${process_file}.local.digests > ${process_file}.local

# Report experiments in local but not in master
echo "Experiments in ${LOCAL_ATLAS_URL} and not in ${MASTER_ATLAS_URL}: " >> $log
diff -y ${process_file}.master.experiments ${process_file}.local.experiments | grep '>' | awk -F'>' '{print $2}' | awk -F"\t" '{print $2}' | sort >> $log

# Report experiments in master but not in local
echo "Experiments in ${MASTER_ATLAS_URL} and not in ${LOCAL_ATLAS_URL}: " >> $log
diff -y ${process_file}.master.experiments ${process_file}.local.experiments | grep '<' | awk -F'<' '{print $1}' | awk -F"\t" '{print $1}' | sort >> $log

echo "Experiments that differ between ${MASTER_ATLAS_URL} and ${LOCAL_ATLAS_URL}: " >> $log
exps_in_master_and_local=`comm -1 -2 ${process_file}.master.experiments ${process_file}.local.experiments`
# Report experiments that differ between master and local
for exp in $(echo $exps_in_master_and_local); do
    in_master=`grep -P "${exp}\t" ${process_file}.master`
    in_local=`grep -P "${exp}\t" ${process_file}.local`
    if [ "$in_master" != "$in_local" ]; then
        echo "$exp" >> $log
    fi
done

# Remove auxiliary files
rm -rf $process_file.master.digests
rm -rf $process_file.master.experiments
rm -rf $process_file.local.digests
rm -rf $process_file.local.experiments
rm -rf $process_file.master
rm -rf $process_file.local

if [ ! -z $NOTIFICATION_EMAILADDRESS ]; then
     mailx -s "Experiments comparison between (master) ${MASTER_ATLAS_URL} and (local) ${LOCAL_ATLAS_URL}"`date +'%d-%m-%Y'` ${NOTIFICATION_EMAILADDRESS} < $log
fi

exit 0