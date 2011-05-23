#!/bin/bash
#
# Generates A2_Sample(organismid) update statement for all samples where 'species' column (the third one) is not empty
# (Sample.dat is the dump of A2_Sample table from release before 03/10/2011)
#
# If your data is the same as EBI Atlas production you probably do not need to use this script; the list of required
# update statements already added to the list of sql updates.
#

if [ $# -ne 1 ]
then
    echo "Error in $0 - Invalid Argument Count"
    echo "Syntax: $0 <Sample.dat> "
    exit
fi

awk -F'\t' '{ if ($3) { printf("update a2_sample set organismid=%s where sampleid=%s;\n", $3, $1) } }' $1 > fix_sample_organism.sql

