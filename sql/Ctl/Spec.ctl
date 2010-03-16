OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/Spec.dat'
truncate into table a2_Spec
fields terminated by "\t" 
TRAILING NULLCOLS
(SPECID,NAME)

