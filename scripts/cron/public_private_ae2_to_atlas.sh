#!/bin/bash
# @author: rpetry
# @date:   18 Oct 2011

# This script serves to reflect in Atlas private/public experiment status switch on the AE2 side.

if [ $# -eq 0 ]; then
        echo "Usage: $0 ADMIN_USERNAME ADMIN_PASSWORD ATLAS_URL ATLAS_PORT ATLAS_ROOT ERROR_NOTIFICATION_EMAILADDRESS"
        echo "e.g. $0 admin <pwd> lime 14032 gxa-load <notifcation_email_address>"
        exit;
fi

process_data="$3.$4.`eval date +%Y%m%d`"
process_file="/tmp/privatepublic_ae2_to_atlas.$process_data"
authentication_cookie=$process_file.$$
all_atlas_experiments_file=$process_file.$$.all_exps
all_atlas_experiments_file=$process_file.$$.all_atlas_exps
all_ae2_experiments_file=$process_file.$$.all_ae2_exps

# Remove any previous $process_file
rm -rf $process_file

# login to Atlas admin
curl -X GET -c $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=login&userName=$1&password=$2&indent" >> /dev/null

# cut commands extract e.g. 0 from {"numTotal":0,
num_all_atlas_exps=`curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?pendingOnly=ALL&op=searchexp" | cut -d'{' -f2 | cut -d':' -f2 | cut -d',' -f1`
echo "Found $num_all_atlas_exps experiments in Atlas. Processing..."  >> $process_file.log

# Retrieve all Atlas experiments into $all_atlas_experiments_file (json)
curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?&p=0&n=$num_all_atlas_exps&search=&fromDate=&toDate=&pendingOnly=ALL&op=searchexp&indent" > $all_atlas_experiments_file
if [ ! -f $all_atlas_experiments_file ]; then
   err_msg="Updating private/public status of experiments on $3:$4/$5 was unsuccessful due failure to retrieve all Atlas experiments"
   echo $err_msg >> $process_file.log
   mailx -s "$err_msg" $6 < $process_file.log
fi

# Retrieve all AE2 experiments into $all_ae2_experiments_file (ssv)
curl -X GET "http://www.ebi.ac.uk/arrayexpress/admin/privacy" > $all_ae2_experiments_file
if [ ! -f $all_ae2_experiments_file ]; then
   err_msg="Updating private/public status of experiments on $3:$4/$5  was unsuccessful due failure to retrieve all AE2 experiments"
   echo $err_msg >> $process_file.log
   mailx -s "$err_msg" $6 < $process_file.log
fi


# Retrieve from $all_atlas_experiments_file private status for each Atlas experiment
while read line; do
   val=`echo $line | sed 's|[\", ]||g' | grep -P 'accession' | cut -d':' -f2`
   if [ ! -z $val ]; then
        exp_accession=$val
   fi

   val=`echo $line | sed 's|[\", ]||g' | grep -P 'private' | cut -d':' -f2`
   if [ ! -z $val ]; then
       atlas_private_flag=$val

       if [ ! -z $exp_accession ]; then
          # Now get the experiment's private/public status in AE2
          # E.g. line in $all_ae2_experiments_file: accession:E-MEXP-31 privacy:public releasedate:2004-03-01
          ae2_experiment=`grep "accession:$exp_accession " $all_ae2_experiments_file`
	      ae2_release_date=`echo $ae2_experiment | awk '{print $3}' | cut -d':' -f2`
          if [ ! -z $ae2_experiment ]; then
               ae2_public_status=`echo $ae2_experiment | grep -Po 'privacy:public'`
               ae2_private_status=`echo $ae2_experiment | grep -Po 'privacy:private'`
               if [ ! -z $ae2_public_status ]; then
                  if [ $atlas_private_flag == "true" ]; then
                      # Experiment public in AE2 and private in Atlas - make it public in Atlas if today's date >= ae2_release_date + 1 month
                      date_diff=$((`eval date +%s` - `date -d "$ae2_release_date 1 month" +%s`))
                      if [ $date_diff -gt 0 ]; then
                          # Experiment $exp_accession became public in AE2 more than a month ago, but it's still private in Atlas
                          echo "$exp_accession - AE2: public; AE2 reldate: $ae2_release_date; Atlas: private - status change in Atlas: private->public"  >> $process_file.log
                          curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?runMode=RESTART&accession=$exp_accession&type=makeexperimentpublic&autoDepends=false&op=schedule"
                      else
                          echo "$exp_accession - AE2: public; AE2 release_date: $ae2_release_date; Atlas: private - NO status change in Atlas: private->public"  >> $process_file.log
                      fi
                  fi
               elif [ ! -z $ae2_private_status ]; then
                   if [ $atlas_private_flag == "false" ]; then
                      # Experiment private in AE2 and public in Atlas - make it private in Atlas
                      echo "$exp_accession - AE2: private; Atlas: public - status change in Atlas: public->private" >> $process_file.log
                      curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?runMode=RESTART&accession=$exp_accession&type=makeexperimentprivate&autoDepends=false&op=schedule"
                   fi
               fi
          else
             err_msg="Updating private/public status of experiments on $3:$4/$5 unsuccessful: failed to find $exp_accession in AE2"
             echo $err_msg >> $process_file.log
             mailx -s "$err_msg" $6 < $process_file.log
             exit 1
          fi
       else
          err_msg="Updating private/public status of experiments on $3:$4/$5 failed due to incorrect format of Atlas API call output"
          echo $err_msg >> $process_file.log
          mailx -s "$err_msg" $6 < $process_file.log
          exit 1
       fi
   fi
done < $all_atlas_experiments_file

# logout from Atlas admin
curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=logout&indent" >> /dev/null
rm -rf $authentication_cookie

# Remove auxiliary file created by this script
rm -rf $all_atlas_experiments_file
rm -rf $all_ae2_experiments_file
