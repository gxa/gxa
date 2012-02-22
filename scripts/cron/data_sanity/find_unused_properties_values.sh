#!/bin/bash
# @author: rpetry
# @date:   15 Feb 2012

if [ $# -lt 1 ]; then
        echo "Usage: $0 ATLAS_URL"
        exit;
fi

ATLAS_URL=$1

process_file="/tmp/find_unused_properties_values."`eval date +%Y%m%d`
report=$process_file".txt"
rm -rf $report
rm -rf ${process_file}.properties
rm -rf ${process_file}.property_values

curl -s -X GET -v "${ATLAS_URL}/api/curators/v1/properties/unused.json" | sed 's|},{|\
|g' | sed 's/"apiPropertyNameList"://g' | awk -F":" '{print $2}' | sed 's|["}]||g' | sed 's|]||g' | sort -f >> ${process_file}.properties

curl -s -X GET -v "${ATLAS_URL}/api/curators/v1/properties/values/unused.json" | sed 's|},{|\
|g' | sed 's/"apiPropertyValueList"://g' |  awk -F"name" '{print $2}' | awk -F",\"value" '{print $1$2}' | sed 's|^":"||' | sed 's|[}"]||g' | sed 's|]||g' | sort -f >> ${process_file}.property_values


if [ -e "${process_file}.properties" ]; then
    echo "The following properties are not used in any assays/samples: "
    cat ${process_file}.properties
    echo -e "\n"
    echo -e "\n"
fi

if [ -e "${process_file}.property_values" ]; then
    echo "The following property values are not used in any assays/samples: "
    cat "${process_file}.property_values"
fi

rm -rf ${process_file}.properties
rm -rf ${process_file}.property_values
