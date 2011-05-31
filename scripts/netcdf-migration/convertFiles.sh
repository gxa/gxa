#!/bin/sh

processDirectory() {
  for file in *; do
    if [ -d $file ]; then
      pushd $file > /dev/null
      processDirectory
      popd > /dev/null
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
      if [ "$ncfile" == "$new_name.nc" ]; then 
        echo Skipping $ncfile
      else
        echo "$ncfile => $new_name.nc"
        variables=`ncdump -h $ncfile | psed -e "1,/variables:/d" -e "/^$/,/}/d" -e "s/.\\+[a-z]\\+ //" -e "s/(.\\+//" -e "/^AS$/d" -e "/^BS$/d" -e "/^DE$/d"`
        variables=`echo $variables | psed "s/ /,/g"`
        ncdump -v $variables $ncfile | psed -e "/double AS(AS)/d" -e "/double BS(BS)/d" -e "/double DE(DE)/d" > $new_name.data
        if ncgen -o $new_name.nc $new_name.data; then
          rm $new_name.data $ncfile
        fi
      fi
    fi
  done
}

processDirectory

