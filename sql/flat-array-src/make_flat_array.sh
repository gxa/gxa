# a pretty fast exporter from Oracle to tab-delimited files (much faster than sqlplus)
#Êthanks to http://asktom.oracle.com/pls/asktom/f?p=100:11:0::::P11_QUESTION_ID:459020243348

# you will need $ORACLE_HOME/bin in the $PATH, with Pro*C and respective Oracle headers and
#Êclient libraries installed, as well as gcc.

# copy the resulting file (flat_array) to the same location as migrate.sh and it will be used
#Êinstead of sqlplus.

proc ./flat_array.pc
gcc -o flat_array ./flat_array.c -L /sw/arch/dbtools/oracle/product/11.1.0.6.2/client/lib -lclntsh
