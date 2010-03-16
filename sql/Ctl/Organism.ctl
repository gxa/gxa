OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/Organism.dat'
truncate into table a2_Organism
fields terminated by "\t" 
TRAILING NULLCOLS
(ORGANISMID,NAME)

