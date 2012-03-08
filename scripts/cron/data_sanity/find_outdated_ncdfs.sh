#!/bin/bash
#
# This script finds all experiments with ncdfs containing properties or property values not in Atlas DB; for each such
# experiment it sets its ncdf update and analytics calculation status in $ATLAS_URL instance as incomplete - so that
# the nightly data update updates the outdated ncdfs accordingly
# @author: rpetry
# @date:   23 Feb 2012

if [ $# -lt 6 ]; then
        echo "Usage: $0 NCDF_DIR ATLAS_URL ADMIN_USER ADMIN_PWD CURAPI_USERNAME CURAPI_PASSWORD"
        exit;
fi

NCDF_DIR=$1
ATLAS_URL=$2
ADMIN_USER=$3
ADMIN_PWD=$4
CURAPI_USERNAME=$5
CURAPI_PASSWORD=$6

process_file="/tmp/find_outdated_ncdfs."`eval date +%Y%m%d`

rm -rf ${process_file}.properties
rm -rf ${process_file}.values
rm -rf ${process_file}.propertiesNotInDB
rm -rf ${process_file}.propertyValuesNotInDB
rm -rf ${process_file}.experimentsToSetAsIncomplete

curl -s -X GET -u ${CURAPI_USERNAME}:${CURAPI_PASSWORD} -v "${ATLAS_URL}/api/curators/v1/properties.json" | sed 's|},{|\
|g' | sed 's/"apiPropertyNameList"://g' | awk -F":" '{print $2}' | sed 's|}]}||g' | sed 's|"||g' | sort -f | uniq > ${process_file}.properties
db_props=`cat ${process_file}.properties`

rm -rf ${process_file}.values
for property in $(cat "${process_file}.properties"); do
	curl -s -X GET -u ${CURAPI_USERNAME}:${CURAPI_PASSWORD} -v "${ATLAS_URL}/api/curators/v1/properties/${property}.json" | sed 's|},{|\
|g' | sed 's/"apiPropertyValueList"://g' | awk -F"value" '{print $2}' | sed 's|}]}||g' | sed 's|"||g' | sed 's|^:||' | sort -f | uniq >> ${process_file}.values
done
db_vals=`cat ${process_file}.values | sort -f | uniq`

IFS="
"
for ncdf in $(find $NCDF_DIR -name *_data.nc); do
   ncdf_props=`ncdump -v EF $ncdf | grep '  "' | sed 's|["\\]||g' | sed 's|^[ ]*||g' | sed 's|;$||g' | sed 's|[ ]*$||g' | sed 's|,$||g' | sort -f | uniq | sed '/^$/d'`
   ncdf_vals=`ncdump -v EFV $ncdf | grep '  "' | sed 's|["\\]||g' | sed 's|^[ ]*||g' | sed 's|;$||g' | sed 's|[ ]*$||g' | sed 's|,$||g' | sort -f | uniq | sed '/^$/d'`

   for ncdf_prop in $(echo "$ncdf_props"); do
          ncdf_prop_esc="$(echo "$ncdf_prop" | sed 's/[^-A-Za-z0-9_]/\\&/g')" # backslash special characters
          if [[ "$db_props" =~ "$ncdf_prop_esc" ]]; then # if $ncdf_prop in DB, all well and good
		      : # ignoring $ncdf_prop
	      else
	         echo "$ncdf : $ncdf_prop : .$ncdf_prop_esc."
		     echo $ncdf" : "$ncdf_prop >> ${process_file}.propertiesNotInDB
		     echo `echo $ncdf | awk -F "/" '{print $10}'` >> ${process_file}.experimentsToSetAsIncomplete
	      fi
   done

   for ncdf_val in $(echo "$ncdf_vals"); do
          ncdf_val_esc="$(echo "$ncdf_val" | sed 's/[^-A-Za-z0-9_'\''<>]/\\&/g')" # backslash special characters (note exclusion of single quote and <>)
          if [[ "$db_vals" =~ "$ncdf_val_esc" ]]; then # if $ncdf_val in DB, all well and good
		      : # ignoring $ncdf_val
	      else
		     echo $ncdf" : "$ncdf_val >> ${process_file}.propertyValuesNotInDB
		     echo `echo $ncdf | awk -F "/" '{print $10}'` >> ${process_file}.experimentsToSetAsIncomplete
	      fi
   done
done

curl -c ${process_file}.session-cookie -X GET -H "Accept: application/json" "${ATLAS_URL}/admin?op=login&userName=${ADMIN_USER}&password=${ADMIN_PWD}"
if [ -e "${process_file}.experimentsToSetAsIncomplete" ]; then
   for exp in $(cat "${process_file}.experimentsToSetAsIncomplete" | sort | uniq ); do
        curl -X GET -b ${process_file}.session-cookie -H "Accept: application/json" "${ATLAS_URL}/admin?accession=${exp}&op=setincomplete"
   done
   curl -X GET -b ${process_file}.session-cookie -H "Accept: application/json" "${ATLAS_URL}/admin?op=logout"
fi
unset IFS

rm -rf ${process_file}.properties
rm -rf ${process_file}.values
rm -rf ${process_file}.session-cookie

