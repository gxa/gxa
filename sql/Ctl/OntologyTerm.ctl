OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/OntologyTerm.dat'
truncate into table a2_OntologyTerm
fields terminated by '\t'  
TRAILING NULLCOLS
(ONTOLOGYTERMID,ONTOLOGYID,TERM,ACCESSION,DESCRIPTION)


