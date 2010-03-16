OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/Assay.dat'
truncate into table a2_Assay
fields terminated by "\t" 
TRAILING NULLCOLS
(ASSAYID,ACCESSION,EXPERIMENTID,ARRAYDESIGNID)

