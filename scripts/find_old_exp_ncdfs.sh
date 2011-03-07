#!/bin/bash
# This script 
# Exaple usage: ./find_old_exp_ncdfs.sh /ebi/microarray/home/atlas-production/ATLASRAW2.ATLASDEV/ncdf 2>&1 > ~/old_exp_ncdfs.log &
#
if [ $# != 1 ]; then
  echo "Please provide full path to ncdf repository you wish to analyse"
  exit 1
fi 

ncdf_dir=$1
for exp_dir in $(find $ncdf_dir -name E-????-*)
do 
  if [ -d $exp_dir ]; then 
    prev_exp_id=""
    prev_ncdf=""
    for ncdf_path in $(ls $exp_dir/*.nc) 
       do  
          ncdf=`ls $ncdf_path | cut -d'/' -f11`
          exp_acc=`ls $ncdf_path | cut -d'/' -f10`
	  exp_id=`echo $ncdf | cut -d'_' -f1`

          if [ -z $prev_exp_id ]; then
	      prev_exp_id=$exp_id 
          fi

	  if [ "$exp_id" != "$prev_exp_id" ]; then
                 
             echo "Mismatch for $exp_acc: experiment ids different in $ncdf and $prev_ncdf"
	  fi
          prev_exp_id=$exp_id
          prev_ncdf=$ncdf
       done
  fi
done
exit 0