OPTIONS(ROWS=1000000,DIRECT=TRUE)
load data
infile 'Data/GenePropertyValue.dat'
truncate into table a2_GenePropertyValue
fields terminated by "\t" 
TRAILING NULLCOLS
(GENEPROPERTYVALUEID,GENEPROPERTYID,VALUE)

