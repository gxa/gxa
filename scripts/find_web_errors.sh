#!/bin/bash
# @author: rpetry
# @date:   11 Nov 2011

# This script allows to find the errors in atlas.log.<YYYY-MM-DD> along with their usage context in access.log<YYYY-MM-DD>
# This script is to be run on each of Atlas production tomcat machines

if [ $# -lt 2 ]; then
        echo "Usage: $0 WEBLOG_DIR NOTIFICATION_EMAILADDRESS"
        exit;
fi

today="`eval date +%Y-%m-%d`"
process_file="/tmp/find_web_errors.$today"
atlaslog_file="$1/atlas.log"
accesslog_file="$1/access_$today.log"

rm -rf $process_file.log

# Sort error types by frequency
grep "ERROR" $atlaslog_file | egrep -v "Exception|Failed to borrow an RServices object|No results were found|There are no records for experiment|Invalid factor|mydasGxaServlet|No existing genes specified by query" | awk -F'-' '{print $6}' | sort | uniq -c | sort -rg > $process_file.error_breakdown
echo "The following Atlas error frequencies/types occurred on "`hostname`" on "`eval date +%Y-%m-%d`": " >> $process_file.log
echo "-----------------------------------------------------------------------------------------------" >> $process_file.log
cat $process_file.error_breakdown >> $process_file.log
echo "" >> $process_file.log
echo "The following Atlas exceptions frequencies/types occurred on "`hostname`" on "`eval date +%Y-%m-%d`": " >> $process_file.log
echo "-----------------------------------------------------------------------------------------------" >> $process_file.log
grep "Exception" $atlaslog_file | egrep -v "ERROR|WARN|UnimplementedFeatureException|Caused by|No existing genes specified by query" | sort | uniq -c | sort -rg >> $process_file.log

# for each error in $process_file.error_ids find time of query from $atlaslog_file
cat $process_file.error_breakdown | awk '{print $2,$3,$4,$5,$6,$7}' > $process_file.error_ids

echo "" >> $process_file.log
      echo "----------------------------- ACCESS LOG CORRELATIONS FOR ERRORS ONLY: -------------------------" >> $process_file.log
while read error_id; do
      echo "" >> $process_file.log
      echo "------------------------------------------------------------------------------------------------" >> $process_file.log
      echo "-- IPADDR, TIME, REQUEST, HTTP_RETURN_CODE, REFERER for '$error_id' : " >> $process_file.log
      grep "$error_id" $atlaslog_file | awk '{print $2}' | awk -F, '{print $1}' | xargs -I % grep % $accesslog_file | xargs -I % echo % | awk '{print $1, $4, $7, $9, $11}' | egrep -v ' 200 ' >> $process_file.log
done < $process_file.error_ids

mailx -s "[gxa/cron] "`eval date +%Y-%m-%d`": Atlas web error report for: "`hostname`  $2 < $process_file.log
rm -rf $process_file.error_breakdown
rm -rf $process_file.error_ids