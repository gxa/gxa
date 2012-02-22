#!/bin/bash
# @author: rpetry
# @date:   16 Jan 2012

if [ $# -lt 1 ]; then
        echo "Usage: $0 NCDF_DIR "
        exit;
fi

NCDF_DIR=$1

for stats_file in $(find $NCDF_DIR -name *_statistics.nc); do
   tstat=`ncdump -v PVAL $stats_file | egrep -v PVAL | egrep -v TSTAT`
   pval=`ncdump -v TSTAT $stats_file | egrep -v PVAL | egrep -v TSTAT`
   if [ "$pval" == "$tstat" ]; then

        # Now check if experiment has not enough expressions for each ef-efv to make a differential call
        data_file=`echo $stats_file | sed 's|_statistics|_data|'`
        exp_ad=`echo $stats_file | awk -F"/" '{print $11}' | awk -F"_" '{print "exp: "$1" ad: "$2}'`

        # Now check how many DE's are being analysed in an experiment - it looks
        # (c.f. http://comments.gmane.org/gmane.science.biology.informatics.conductor/30292) as if limma's lmFit function
        # throws the following error if only one DE is being tested:
        # Error in fit$effects[(fit$rank + 1):narrays, , drop = FALSE] :
        # incorrect number of dimensions

        num_des=`ncdump -h $data_file | grep 'DE = ' | awk -F" = " '{print $2}' | sed 's|[ ;]$||g'`
        if [ $num_des -eq 1 ]; then
            echo "$exp_ad: limma's lmFit function fails when num of DE's = $num_des"
            echo -e "\n"
        else

           # EFs
           num_efs=`ncdump -h $data_file | grep 'EF = ' | awk -F" = " '{print $2}' | sed 's|[ ;]$||g'`
           efs=`ncdump -v EF $data_file | grep '"' | tail -$num_efs | sed 's|[",]||g' | sed 's|[ ;]$||g'`

           # Assays
           num_assays=`ncdump -h $data_file | grep 'AS = ' | awk -F" = " '{print $2}' | sed 's|[ ;]$||g'`

           # EFVs
           num_efvs=$[ $num_efs * $num_assays ]
           efvs=`ncdump -v EFV $data_file | grep '"' | tail -$num_efvs | sed 's|[";,]||g' | sed 's|[ ;]$||g'`

           echo "No statistics in: $exp_ad: "
           efvs_remainder=$efvs
           for ef in $(echo $efs); do
               if [ $num_efvs -ne 0 ]; then

                  efvs_aux=`echo $efvs_remainder`
                  efvs_for_ef=`echo "$efvs_remainder" | head -$num_assays | sort | uniq -c`
                  # If this displays either
                  # a. just one efv per factor, or
                  # b. more than one efvs per factor but each of efvs with a count of 1 (i.e. a given ef-efv
                  #    was measured in only one assay)
                  # then no statistics will have been calculated for experimental factor $ef
                  # If all factors in this experiment/array design qualify for either a. and b. then
                  # no statistics will have been calculated for this experiment/array design
                  echo "  counts/efvs for ef = $ef:"
                  echo "$efvs_for_ef"
                  num_efvs=$[ $num_efvs - $num_assays ]
                  efvs_remainder=`echo "$efvs_remainder" | tail -$num_efvs`
               fi
           done
           echo -e "\n"
        fi
   fi
done


