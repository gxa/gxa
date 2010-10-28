#!/bin/bash
# Installing Atlas2 database

source install-routines.sh

if [ $# -ne 3 ]; then
	echo "Usage: $0 ATLAS_CONNECTION ATLAS_NCDF_PATH ATLAS_RELEASE"
	exit;
fi

ATLAS_CONNECTION=$1
ATLAS_NCDF_PATH=$2
ATLAS_RELEASE=$3

if [ -d $ATLAS_RELEASE ]; then
	echo "$ATLAS_RELEASE already exists! Choose another directory name.";
	exit;
fi

mkdir $ATLAS_RELEASE
pushd $ATLAS_RELEASE

echo "Dumping data..."
mkdir Data
for TABLE_NAME in $TABLE_NAMES
do
  echo "... $TABLE_NAME"
  sqlplus -S $ATLAS_CONNECTION @../create_ctl_for_table.sql $TABLE_NAME > Data/$TABLE_NAME.ctl
  sqlplus -S $ATLAS_CONNECTION @../create_sql_for_table.sql $TABLE_NAME > Data/$TABLE_NAME.sql

  ../flat_array userid=$ATLAS_CONNECTION sqlstmt="`cat Data/$TABLE_NAME.sql`" arraysize=100 > Data/$TABLE_NAME.dat
done

echo "Exporting schema scripts"
svn export svn://bar.ebi.ac.uk/branches/atlas-standalone/sql/Schema Schema


echo "Packing the release"
popd
cp drop_all.sql install-routines.sh install.sh INSTALL $ATLAS_RELEASE/
tar cvzf $ATLAS_RELEASE.tar.Z $ATLAS_RELEASE

echo "Packing the NetCDFs"
ln -sf $ATLAS_NCDF_PATH ./ncdf
find ncdf/ -name '*.nc' | xargs tar rvf $ATLAS_RELEASE-ncdf.tar
rm ncdf
