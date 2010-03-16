OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/Gene.dat'
truncate into table a2_Gene
fields terminated by "\t" 
TRAILING NULLCOLS
(GENEID,ORGANISMID,IDENTIFIER,NAME)    
