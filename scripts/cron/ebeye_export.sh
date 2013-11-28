#!/bin/sh

# @author: rpetry
# @date:   04 Nov 2010
# This script tries to obtain www-test.ebi.ac.uk/gxa/download/ebeye_export.xml via curl. If the dump file is being generated 
# from scratch, an EBI timeout will cause a retrieval failure and a non-zip formatted error file will be returned. If this 
# is the case, the scripts reports an error and re-tries in 10 mins (up to 4 additional times) - at which time the dump 
# file will hopefully have been generated and is just sitting in a temp directory ready to be served by Atlas.
# This script is run as cron by tomcat user on machine dukkha. 

if [ $# -eq 0 ]; then
        echo "Usage: $0 INTERNAL_ATLAS_PROD_INSTANCE_URL INTERNAL_ATLAS_PROD_INSTANCE_PORT ERROR_NOTIFICATION_EMAILADDRESS"
        echo "e.g. $0 <atlas_url> <atlas_port> <notifcation_email_address>"
        exit;
fi

for attempt_num in 1 2 3 4 5 
do
  curl http://$1:$2/gxa/download/ebeye_export.xml -o /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip.tmp
  format=$(file /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip.tmp | awk '{ print $2}')
  s=$(ls -lah /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip.tmp | awk '{ print $5}')
  echo "attempt $attempt_num : Downloaded ebeye_export.xml.zip size: $s and format: $format"
  # Send an error email and re-try after 1 hr if ebeye_export.xml.zip.tmp generation is not completed yet (file cmd doesn't recognize it as being in zip format)
  if [ $format != 'Zip' ] ; then
    mailx -s"attempt $attempt_num : `whoami`@`hostname` cron error : $0 downloaded ebeye_export.xml.zip in incorrect format: $format - re-trying after 1 hr" $3 < /dev/null
    sleep 3600 # sleep 1h
  else 
    mv /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip.tmp /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip
    success_msg="attempt $attempt_num: successfully saved /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip file"
    echo "$success_msg"
    if [ $attempt_num -gt 1 ] ; then # there had been a failure that's now been fixed - email $success_msg
	mailx -s"$success_msg" $3 < /dev/null
    fi
    break # break out of the loop - we're done
  fi
done
