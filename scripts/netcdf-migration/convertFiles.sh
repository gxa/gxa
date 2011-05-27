#!/bin/sh

processDirectory() {
  for file in *; do
    if [ -d $file ]; then
      pushd $file
      processDirectory
      popd
    fi
  done
  processNCDFs
}

processNCDFs() {
  for ncfile in *.nc; do
    if [ -f $ncfile ]; then
      accession=`ncdump -h $ncfile | grep experiment_accession | psed 's/.\+"\(.\+\)".\+/\1/'`
      ad_accession=`ncdump -h $ncfile | grep ADaccession | psed 's/.\+"\(.\+\)".\+/\1/'`
      underscore=_
      new_name=$accession$underscore$ad_accession
      echo "$ncfile => $new_name.nc"
      ncdump -v BS2AS,DEacc,GN,ASacc,BSacc,EFSC,SC,SCV,EF,EFV,uVAL,uVALnum,PVAL,TSTAT,ORDER_ANY,ORDER_UP_DOWN,ORDER_UP,ORDER_DOWN,ORDER_NON_D_E,BDC $ncfile | sed "/double AS(AS)/d" | sed "/double BS(BS)/d" | sed "/double DE(DE)/d" > $new_name.data
      if ncgen -o $new_name.nc $new_name.data; then
        rm $new_name.data $ncfile
      fi
    fi
  done
}

processDirectory
