OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/GeneProperty.dat'
truncate into table a2_GeneProperty
fields terminated by "\t"
TRAILING NULLCOLS
(GENEPROPERTYID,NAME,AE2TABLENAME,IDENTIFIERPRIORITY)

