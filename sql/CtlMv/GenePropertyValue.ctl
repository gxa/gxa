OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'DataMv/GenePropertyValue.dat'
truncate into table a2_GenePropertyValue
fields terminated by "\t" 
TRAILING NULLCOLS
(GENEPROPERTYVALUEID 
,GENEPROPERTYID
,VALUE
)

