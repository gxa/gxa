#!/bin/bash
# Releasing Atlas2 NetCDF files

if [ $# -ne 3 ]; then
	echo "Usage: $0 ATLAS_CONNECTION ATLAS_NCDF_PATH ATLAS_RELEASE (withbams)"
	echo "'withbams' option includes assays and annotations directories (excluded by default)"
	exit;
fi

ATLAS_CONNECTION=$1
ATLAS_NCDF_PATH=$2
ATLAS_RELEASE=$3
WITH_BAMS=$4

echo "Packing the NetCDFs"

# ncdfs-to-export.sql prints the list of (public) experiment data files in the following format:
# experiment_accession
# e.g.
# E-GEOD-5579
# E-GEOD-5348
# E-GEOD-7149
# E-GEOD-7177

if [ ! -z "$WITH_BAMS" ]; then
   sqlplus -S $ATLAS_CONNECTION @ncdfs-to-export.sql | \
     awk '{ split($1, a, "-"); print "ncdf/" a[2] "/" (a[3] < 100 ? "" : int(a[3]/100)) "00/" $1 "/" }'  | \
     xargs tar rv -C $ATLAS_NCDF_PATH/.. -f $ATLAS_RELEASE-ncdf.tar
else
   sqlplus -S $ATLAS_CONNECTION @ncdfs-to-export.sql | \
     awk '{ split($1, a, "-"); print "ncdf/" a[2] "/" (a[3] < 100 ? "" : int(a[3]/100)) "00/" $1 "/" }'  | \
     xargs tar rv -C $ATLAS_NCDF_PATH/.. --exclude 'assays' --exclude 'annotations' -f $ATLAS_RELEASE-ncdf.tar
fi