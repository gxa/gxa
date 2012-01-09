#!/bin/bash
# Releasing Atlas2 NetCDF files

if [ $# -ne 2 ]; then
	echo "Usage: $0 ATLAS_CONNECTION ATLAS_NCDF_PATH"
	exit;
fi

BASEDIR=$(dirname $0)
ATLAS_CONNECTION=$1
ATLAS_NCDF_PATH=$2

echo "Packing the NetCDFs"

# ncdfs-to-export.sql prints the list of (public) experiment data files in the following format:
# experiment_accession
# e.g.
# E-GEOD-5579
# E-GEOD-5348
# E-GEOD-7149
# E-GEOD-7177
cd $ATLAS_NCDF_PATH
sqlplus -S $ATLAS_CONNECTION @$BASEDIR/ncdfs-to-export.sql | \
  awk '{ split($1, a, "-"); print "ncdf/" a[2] "/" (a[3] < 100 ? "" : int(a[3]/100)) "00/" $1 "/" }'  | \
  find . -xdev -depth -print | cpio -pdm /nfs/public/ro/fg/atlas/data/ncdf
