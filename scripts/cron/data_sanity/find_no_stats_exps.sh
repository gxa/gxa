#!/bin/bash
# @author: rpetry
# @date:   16 Jan 2012

if [ $# -lt 1 ]; then
        echo "Usage: $0 NCDF_DIR "
        exit;
fi

NCDF_DIR=$1

echo "The following experiments/designs have no statistics:"
for stats_file in $(find $NCDF_DIR -name *_statistics.nc); do
   tstat=`ncdump -v PVAL $stats_file | egrep -v PVAL | egrep -v TSTAT`
   pval=`ncdump -v TSTAT $stats_file | egrep -v PVAL | egrep -v TSTAT`
   if [ "$pval" == "$tstat" ]; then

         # Now check if experiment has (potentially) not enough efvs to make a differential call
         data_file=`echo $stats_file | sed 's|_statistics|_data|'`
         num_efs=`ncdump -v EF $data_file | grep 'EF = ' | awk -F" = " '{print $2}' | sed 's|[\s;]||g'`
         num_unique_efvs=`ncdump -v EFV $data_file | grep '  "' | sort | uniq | wc -l`
         if [ $num_unique_efvs -le $num_efs ]; then
            echo "$stats_file: efs = $num_efs; unique efvs = $num_unique_efvs"
         else
            # Now check how many DE's are being analysed in an experiment - it looks
            # (c.f. http://comments.gmane.org/gmane.science.biology.informatics.conductor/30292) as if limma's lmFit function
            # throws the following error if only one DE is being tested:
            # Error in fit$effects[(fit$rank + 1):narrays, , drop = FALSE] :
            # incorrect number of dimensions

            num_des=`ncdump -v EF $data_file | grep 'DE = ' | awk -F" = " '{print $2}' | sed 's|[\s;]||g'`
            if [ $num_des -eq 1 ]; then
                echo "$stats_file: num of DE's = $num_des"
            else
                echo "$stats_file"
            fi
         fi
   fi
done


