OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/Ontology.dat'
truncate into table a2_Ontology
fields terminated by "\t"
TRAILING NULLCOLS
(ONTOLOGYID,NAME,SOURCE_URI,DESCRIPTION,VERSION)

