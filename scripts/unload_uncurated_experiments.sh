#!/bin/bash
# @author: rpetry
# @date:   3 Nov 2011

# This script serves to identify and then unload all experiments that don't contain experimental factors (i.e. are uncurated). To be run on a server where NCDF_DIR is nfs-mounted.

if [ $# -eq 0 ]; then
        echo "Usage: $0 NCDF_DIR ADMIN_USERNAME ADMIN_PASSWORD ATLAS_HOST ATLAS_PORT ATLAS_ROOT ERROR_NOTIFICATION_EMAILADDRESS"
        echo "e.g. $0 <ncdf_dir> admin <pwd> lime 14032 gxa-load atlas-developers@ebi.ac.uk"
        exit;
fi

process_data="$4.$5.`eval date +%Y%m%d`"
process_file="/tmp/unload_curated_experiments.$process_data"
authentication_cookie=$process_file.$$

rm -rf $process_file.log
rm -rf /tmp/experiments_ef_status.txt.$process_data
rm -rf /tmp/uncurated_experiments.txt.$process_data

# login to Atlas admin
curl -X GET -c $authentication_cookie -H "Accept: application/json" "http://$4:$5/$6/admin?op=login&userName=$2&password=$3&indent" >> /dev/null

for ncdf in $(find $1 -name *_data.nc); do echo "$ncdf "`ncdump -h $ncdf | grep 'EF ='`; done >> /tmp/experiments_ef_status.txt.$process_data
egrep -v 'EF =' /tmp/experiments_ef_status.txt.$process_data | awk -F/ '{print $10}' | uniq > /tmp/uncurated_experiments.txt.$process_data
for accession in `cat /tmp/uncurated_experiments.txt.$process_data`; do
    echo "curl -X GET -b $authentication_cookie -H \"Accept: application/json\" \"http://$4:$5/$6/admin?op=schedule&runMode=RESTART&type=unloadexperiment&autoDepends=false&accession=$accession&indent\""  >> $process_file.log
    curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$4:$5/$6/admin?op=schedule&runMode=RESTART&type=unloadexperiment&autoDepends=false&accession=$accession&indent" 2>&1 >> $process_file.log
done

# logout from Atlas admin
curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$4:$5/$6/admin?op=logout&indent" >> /dev/null

mailx -s `eval date +%H:%M:%S`" The following experiments were scheduled for unloading from Atlas: " $7 < /tmp/uncurated_experiments.txt.$process_data

rm -rf /tmp/experiments_ef_status.txt.$process_data
rm -rf /tmp/uncurated_experiments.txt.$process_data
rm -rf $authentication_cookie
