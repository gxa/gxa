#!/bin/bash
# @author: rpetry
# @date:   15 Feb 2012

if [ $# -lt 3 ]; then
        echo "Usage: $0 ATLAS_URL CURAPI_USERNAME CURAPI_PASSWORD"
        exit;
fi

ATLAS_URL=$1
CURAPI_USERNAME=$2
CURAPI_PASSWORD=$3

# Remove properties not found in any assay/sample
curl -s -X DELETE -u ${CURAPI_USERNAME}:${CURAPI_PASSWORD} -v "${ATLAS_URL}/api/curators/v1/properties/unused.json"

# Remove property values not found in any assay/sample
curl -s -X DELETE -u ${CURAPI_USERNAME}:${CURAPI_PASSWORD} -v "${ATLAS_URL}/api/curators/v1/properties/values/unused.json"

