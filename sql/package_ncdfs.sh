#!/bin/bash
# Installing Atlas2 database

if [ $# -ne 2 ]; then
	echo "Usage: $0 ATLAS_NCDF_PATH ATLAS_RELEASE"
	exit;
fi

ATLAS_NCDF_PATH=$1
ATLAS_RELEASE=$2

echo "Packing the NetCDFs"
ln -sf $ATLAS_NCDF_PATH ./ncdf
find ncdf/ -name '*.nc' | xargs tar rvf $ATLAS_RELEASE-ncdf.tar
rm ncdf
