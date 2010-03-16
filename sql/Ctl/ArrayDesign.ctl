OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/ArrayDesign.dat'
truncate into table a2_ArrayDesign
fields terminated by "\t" 
TRAILING NULLCOLS
(ARRAYDESIGNID,ACCESSION,TYPE,NAME,PROVIDER)

