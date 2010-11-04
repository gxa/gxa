#!/bin/sh

# @author: rpetry
# @date:   04 Nov 2010
# This script tries to obtain www.ebi.ac.uk/gxa/download/ebeye_export.xml via curl. If the dump file is being generated 
# from scratch, an EBI timeout will cause a retrieval failure and a non-zip formatted error file will be returned. If this 
# is the case, the scripts reports an error and re-tries in 10 mins (up to 4 additional times) - at which time the dump 
# file will hopefully have been generated and is just sitting in a temp directory ready to be served by Atlas.
# This script is run as cron by tomcat user on machine dukkha. 

for attempt_num in 1 2 3 4 5 
do
  curl www.ebi.ac.uk/gxa/download/ebeye_export.xml -o /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip.tmp
  format=$(file /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip.tmp | awk '{ print $2}')
  s=$(ls -lah /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip.tmp | awk '{ print $5}')
  # echo "attempt $attempt_num : Downloaded ebeye_export.xml.zip size: $s and format: $format"
  # Send an error email and re-try after 10 mins if ebeye_export.xml.zip.tmp size is less than 1 MB
  if [ $format != 'Zip' ] ; then
    mailx -s"attempt $attempt_num : `whoami`@`hostname` cron error : $0 downloaded ebeye_export.xml.zip in incorrect format: $format - re-trying after 10 mins" rpetry@ebi.ac.uk < /dev/null
    sleep 600 # sleep 10 mins
  else 
    mv /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip.tmp /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip
    success_msg="attempt $attempt_num: successfully saved /nas/microarray/home/cronjobs/public/xml/gxa/ebeye_export.xml.zip file"
    # echo "$success_msg"
    if [ $attempt_num -gt 1 ] ; then # there had been a failure that's now been fixed - email $success_msg
	mailx -s"$success_msg" rpetry@ebi.ac.uk < /dev/null
    fi
    break # break out of the loop - we're done
  fi
done
