#!/bin/bash
# @author: rpetry
# @date:   11 Nov 2011

# This script allows to find the errors in atlas.log.<YYYY-MM-DD> along with their usage context in access.log<YYYY-MM-DD>
# This script is to be run on each of Atlas production tomcat machines

if [ $# -lt 1 ]; then
        echo "Usage: $0 HOSTNAME"
        exit;
fi

HOSTNAME=$1
WEBLOG_DIR=/nfs/public/rw/webadmin/tomcat/bases/fg/tc-fg-gxa/logs/${HOSTNAME}

today="`eval date +%Y-%m-%d`"
process_file="/tmp/find_web_errors.$today"
atlaslog_file="${WEBLOG_DIR}/atlas.log"
accesslog_file="${WEBLOG_DIR}/access_$today.log"

rm -rf $process_file.log

# Sort error types by frequency
grep "ERROR" $atlaslog_file | egrep -v "Exception|Failed to borrow an RServices object|No results were found|There are no records for experiment|Invalid factor|mydasGxaServlet|No existing genes specified by query" | awk -F'-' '{print $6}' | sort | uniq -c | sort -rg > $process_file.error_breakdown
echo "The following Atlas error frequencies/types occurred on ${HOSTNAME} on "`eval date +%Y-%m-%d`": "
echo "-----------------------------------------------------------------------------------------------"
cat $process_file.error_breakdown
echo ""
echo "The following Atlas exceptions frequencies/types occurred on ${HOSTNAME} on "`eval date +%Y-%m-%d`": "
echo "-----------------------------------------------------------------------------------------------"
grep "Exception" $atlaslog_file | egrep -v "ERROR|WARN|UnimplementedFeatureException|Caused by|No existing genes specified by query" | sort | uniq -c | sort -rg

# for each error in $process_file.error_ids find time of query from $atlaslog_file
cat $process_file.error_breakdown | awk '{print $2,$3,$4,$5,$6,$7}' > $process_file.error_ids

echo ""
      echo "----------------------------- ACCESS LOG CORRELATIONS FOR ERRORS ONLY: -------------------------"
while read error_id; do
      echo ""
      echo "------------------------------------------------------------------------------------------------"
      echo "-- IPADDR, TIME, REQUEST, HTTP_RETURN_CODE, REFERER, USER_AGENT for '$error_id' : "
      grep "$error_id" $atlaslog_file | awk '{print $2}' | awk -F, '{print $1}' | xargs -I % grep % $accesslog_file | xargs -I % echo % | awk '{print $1, $4, $7, $9, $11, $12}' | egrep -v ' 200 ' | egrep -v ' 302 ' | egrep -v 'SAMSUNG-SGH-E250' >> $process_file.log
done < $process_file.error_ids
echo ""
rm -rf $process_file.error_breakdown
rm -rf $process_file.error_ids