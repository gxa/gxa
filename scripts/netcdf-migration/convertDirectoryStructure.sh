#!/bin/sh
for ncfile in *.nc; do
  accession=`ncdump -h $ncfile | grep experiment_accession | sed 's/.\+"\(.\+\)".\+/\1/'`
  echo $ncfile : $accession;
  grp=`echo $accession | cut -d "-" -f 2`
  num=`echo $accession | cut -d "-" -f 3`
  if [ "$num" == "145a" -o "$num" == "145b" -o "$num" == "145c" ]; then
    num100=1400;
  else
    num100=$((num / 100 * 100))
    if [ "$num100" == "0" ]; then
      num100=00;
    fi
  fi
  dir=$grp/$num100/$accession;
  mkdir -p $dir
  mv $ncfile $dir
done
