OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/GeneGPV.dat'
truncate into table a2_GeneGPV
fields terminated by "\t" 
TRAILING NULLCOLS
(GENEGPVID,GENEID,GENEPROPERTYVALUEID)

