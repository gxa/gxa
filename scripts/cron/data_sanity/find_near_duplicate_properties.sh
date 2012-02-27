#!/bin/bash
# @author: rpetry
# @date:   27 Jan 2012

if [ $# -lt 1 ]; then
        echo "Usage: $0 ATLAS_URL"
        exit;
fi

ATLAS_URL=$1

process_file="/tmp/find_properties."`eval date +%Y%m%d`

curl -s -X GET -v "${ATLAS_URL}/api/curators/v1/properties.json" | sed 's|},{|\
|g' | sed 's/"apiPropertyNameList"://g' | awk -F":" '{print $2}' | sed 's|}]}||g' | sed 's|"||g' | sort -f | uniq > ${process_file}.properties

rm -rf ${process_file}.values
for property in $(cat "${process_file}.properties"); do
    if [ "$property" == "individual" ]; then
	: # Exclude values for property 'individual' - as requested by curators
    else
	curl -s -X GET -v "${ATLAS_URL}/api/curators/v1/properties/${property}.json" | sed 's|},{|\
|g' | sed 's/"apiPropertyValueList"://g' | awk -F"value" '{print $2}' | sed 's|}]}||g' | sed 's|"||g' | sed 's|^:||' | sort -f | uniq >> ${process_file}.values
    fi 
done

# Load near-duplicates properties exceptions (to be excluded from the report) 
if [ -e ./exceptions_properties.txt ]; then 
   exceptions_properties=`cat ./exceptions_properties.txt`
fi

# Find all properties with max levenstein distance of ${max_levenstein_distance}
IFS="
"
max_levenstein_distance=2
rm -rf ${process_file}.ld${max_levenstein_distance}.properties
for property in $(cat "${process_file}.properties"); do  
   if [ "${#property}" -gt "${max_levenstein_distance}" ]; then
       # Compare only strings of length greater than ${max_levenstein_distance} 
       canonical_previous=`echo ${previous/ /} | tr [A-Z] [a-z] | sed 's| |_|g'`
       canonical_current=`echo ${property/ /} | tr [A-Z] [a-z] | sed 's| |_|g'`
       if [ ! -z $canonical_previous ]; then
          ld=`./ldistance.py $canonical_current $canonical_previous`
          if [ "$ld" -le "$max_levenstein_distance" ]; then
	      similarity="$previous	$property"
	      similarity_esc="$(echo "$similarity" | sed 's/[^-A-Za-z0-9_]/\\&/g')" # backslash special characters
	      if [[ "$exceptions_properties" =~ "$similarity_esc" ]]; then # If similarity is already tagged as an allowable exception, exclude it from the report
		  : # ignoring $similarity
	      else
		  echo $similarity >> ${process_file}.ld${max_levenstein_distance}.properties
	      fi
          fi
       fi
   fi
   previous=$property
done
unset IFS

# Load near-duplicates property values exceptions (to be excluded from the report) 
if [ -e ./exceptions_values.txt ]; then 
   exceptions_values=`cat ./exceptions_values.txt`
fi

# Find all property values with max levenstein distance of ${max_levenstein_distance}
IFS="
"
rm -rf ${process_file}.ld${max_levenstein_distance}.values.nonuniq
rm -rf ${process_file}.ld${max_levenstein_distance}.values
previous=
for value in $(cat "${process_file}.values"); do
   if [ "${#value}" -gt "${max_levenstein_distance}" ]; then
       # Compare only strings of length greater than ${max_levenstein_distance}
       canonical_current=`echo ${value/ /} | tr [A-Z] [a-z] | sed 's| |_|g'`
       canonical_previous=`echo ${previous/ /} | tr [A-Z] [a-z] | sed 's| |_|g'`
       nonumbers_current=`echo ${value/ /} | sed 's|[0-9.]*||g'`
       nonumbers_previous=`echo ${previous/ /} | sed 's|[0-9.]*||g'`
       if [ ! -z $nonumbers_current ]; then
         if [ ! -z $nonumbers_previous ]; then 	   
           if [ $nonumbers_current != $nonumbers_previous ]; then # Don't report number-only differences
             ld=`./ldistance.py $canonical_current $canonical_previous`
             if [ "$ld" -le "$max_levenstein_distance" ]; then
	           if [ $previous != $value ]; then
	             similarity="$previous	$value"
	             similarity_esc="$(echo "$similarity" | sed 's/[^-A-Za-z0-9_'\''<>]/\\&/g')" # backslash special characters (note exclusion of single quote and <>)
	             if [[ "$exceptions_values" =~ "$similarity_esc" ]]; then # If similarity is already tagged as an allowable exception, exclude it from the report
		           : # ignoring $similarity
	             else
		           echo $similarity >> ${process_file}.ld${max_levenstein_distance}.values.nonuniq
	          fi
	        fi
             fi
           fi
	 fi
       fi
   fi
   previous=$value
done
unset IFS

cat ${process_file}.ld${max_levenstein_distance}.values.nonuniq | sort | uniq > ${process_file}.ld${max_levenstein_distance}.values

if [ -e ${process_file}.ld${max_levenstein_distance}.properties ]; then
    echo "Near duplicate property names: "
    cat ${process_file}.ld${max_levenstein_distance}.properties
    echo -e "\n"
fi

if [ -e ${process_file}.ld${max_levenstein_distance}.values ]; then
    echo "Near duplicate property values: "
    cat ${process_file}.ld${max_levenstein_distance}.values
    echo -e "\n"
fi

rm -rf ${process_file}.properties
rm -rf ${process_file}.values
rm -rf ${process_file}.ld${max_levenstein_distance}.properties
rm -rf ${process_file}.ld${max_levenstein_distance}.values
rm -rf ${process_file}.ld${max_levenstein_distance}.values.nonuniq
