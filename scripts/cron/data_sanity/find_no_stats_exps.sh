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
            echo "$stats_file"
         fi
   fi
done


