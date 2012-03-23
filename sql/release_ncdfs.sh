#!/bin/bash
# Releasing Atlas2 NetCDF files

if [ $# -ne 3 ]; then
	echo "Usage: $0 ATLAS_CONNECTION ATLAS_NCDF_PATH ATLAS_EXPORTED_NCDF_PATH"
	exit;
fi

BASEDIR=`pwd`
ATLAS_CONNECTION=$1
ATLAS_NCDF_PATH=$2
ATLAS_EXPORTED_NCDF_PATH=$3

echo "Packing the NetCDFs"

# ncdfs-to-recall.sql prints the list of (public) experiment data files in the following format:
# experiment_accession
# e.g.
# E-GEOD-5579
# E-GEOD-5348
# E-GEOD-7149
# E-GEOD-7177
# Copy to $ATLAS_EXPORTED_NCDF_PATH all files from $ATLAS_NCDF_PATH that are newer in source dir compared to target dir
cd $ATLAS_NCDF_PATH
find . -xdev -depth -print | cpio -pdm $ATLAS_EXPORTED_NCDF_PATH
# Now delete private expriment's files from $ATLAS_EXPORTED_NCDF_PATH
cd $ATLAS_EXPORTED_NCDF_PATH/..
sqlplus -S $ATLAS_CONNECTION @$BASEDIR/ncdfs-to-recall.sql | \
  awk '{ split($1, a, "-"); print "ncdf/" a[2] "/" (a[3] < 100 ? "" : int(a[3]/100)) "00/" $1 "/" }'  | xargs -I % rm -rf %
