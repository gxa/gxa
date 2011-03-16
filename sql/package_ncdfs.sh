#!/bin/bash
# Releasing Atlas2 NetCDF files

if [ $# -ne 3 ]; then
	echo "Usage: $0 ATLAS_CONNECTION ATLAS_NCDF_PATH ATLAS_RELEASE"
	exit;
fi

ATLAS_CONNECTION=$1
ATLAS_NCDF_PATH=$2
ATLAS_RELEASE=$3

echo "Packing the NetCDFs"
ln -sf $ATLAS_NCDF_PATH ./ncdf

# ncdfs-to-export.sql prints the list of (public) experiment data files in the following format:
# experiment_accession <tab> filename
# e.g.
# E-TABM-562	1036805178_170473054.nc
# E-MEXP-2580	1036805592_193480066.nc
# E-GEOD-12675	1036808747_410305887.nc
# E-GEOD-7218	1036806601_175818769.nc
sqlplus -S $ATLAS_CONNECTION @ncdfs-to-export.sql | \
  awk '{ split($1, a, "-"); print a[2] "/" (a[3] < 100 ? "" : int(a[3]/100)) "00/" $1 "/" $2 }'  | \
  xargs tar rvf $ATLAS_RELEASE-ncdf.tar

rm ncdf
