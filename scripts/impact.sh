#!/bin/bash
# Usage: ./impact.sh | awk ' { a[substr($2, 2) " " $3] ++} END {for (i in a) print i "\t" a[i]} '
find . -name *.j* | while read FILENAME
do
       git annotate --pretty=short -w $FILENAME
done