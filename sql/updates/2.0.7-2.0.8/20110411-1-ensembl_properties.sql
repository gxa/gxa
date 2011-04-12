alter table A2_ORGANISM add ensname VARCHAR2(255);

UPDATE A2_ORGANISM SET ensname='athaliana_eg_gene' WHERE name = 'arabidopsis thaliana';
UPDATE A2_ORGANISM SET ensname='btaurus_gene_ensembl' WHERE name = 'bos taurus';
UPDATE A2_ORGANISM SET ensname='celegans_gene_ensembl' WHERE name = 'caenorhabditis elegans';
UPDATE A2_ORGANISM SET ensname='cintestinalis_gene_ensembl' WHERE name = 'ciona intestinalis';
UPDATE A2_ORGANISM SET ensname='drerio_gene_ensembl' WHERE name = 'danio rerio';
UPDATE A2_ORGANISM SET ensname='dmelanogaster_gene_ensembl' WHERE name = 'drosophila melanogaster';
UPDATE A2_ORGANISM SET ensname='ggallus_gene_ensembl' WHERE name = 'gallus gallus';
UPDATE A2_ORGANISM SET ensname='hsapiens_gene_ensembl' WHERE name = 'homo sapiens';
UPDATE A2_ORGANISM SET ensname='mmusculus_gene_ensembl' WHERE name = 'mus musculus';
UPDATE A2_ORGANISM SET ensname='rnorvegicus_gene_ensembl' WHERE name = 'rattus norvegicus';
UPDATE A2_ORGANISM SET ensname='scerevisiae_gene_ensembl' WHERE name = 'saccharomyces cerevisiae';
UPDATE A2_ORGANISM SET ensname='spombe_eg_gene' WHERE name = 'schizosaccharomyces pombe';
UPDATE A2_ORGANISM SET ensname='xtropicalis_gene_ensembl' WHERE name = 'xenopus laevis';
/

alter table A2_BIOENTITYPROPERTY add ensname VARCHAR2(255);

UPDATE A2_BIOENTITYPROPERTY SET ensname='ensembl_gene_id' WHERE name = 'ensgene';
UPDATE A2_BIOENTITYPROPERTY SET ensname='ensembl_peptide_id' WHERE name = 'ensprotein';
UPDATE A2_BIOENTITYPROPERTY SET ensname='description' WHERE name = 'description';
UPDATE A2_BIOENTITYPROPERTY SET ensname='external_gene_id' WHERE name = 'symbol';
UPDATE A2_BIOENTITYPROPERTY SET ensname='name_1006, go_cellular_component__dm_name_1006, go_molecular_function__dm_name_1006' WHERE name = 'goterm';
UPDATE A2_BIOENTITYPROPERTY SET ensname='go_biological_process_id, go_cellular_component_id, go_molecular_function_id' WHERE name = 'go';
UPDATE A2_BIOENTITYPROPERTY SET ensname='interpro' WHERE name = 'interpro';
UPDATE A2_BIOENTITYPROPERTY SET ensname='interpro_short_description' WHERE name = 'interproterm';
UPDATE A2_BIOENTITYPROPERTY SET ensname='hgnc_symbol' WHERE name = 'hgnc_symbol';
UPDATE A2_BIOENTITYPROPERTY SET ensname='uniprot_sptrembl, uniprot_swissprot_accession' WHERE name = 'uniprot';
UPDATE A2_BIOENTITYPROPERTY SET ensname='unigene' WHERE name = 'unigene';
UPDATE A2_BIOENTITYPROPERTY SET ensname='refseq_dna, refseq_peptide' WHERE name = 'refseq';
UPDATE A2_BIOENTITYPROPERTY SET ensname='embl' WHERE name = 'embl';
UPDATE A2_BIOENTITYPROPERTY SET ensname='mim_morbid_description' WHERE name = 'disease';
UPDATE A2_BIOENTITYPROPERTY SET ensname='ensfamily' WHERE name = 'family';
UPDATE A2_BIOENTITYPROPERTY SET ensname='family_description' WHERE name = 'ensfamily_description';
UPDATE A2_BIOENTITYPROPERTY SET ensname='ox_sgd_transcript__dm_dbprimary_acc_1074' WHERE name = 'sgd_transcript';
UPDATE A2_BIOENTITYPROPERTY SET ensname='ox_sgd_gene__dm_dbprimary_acc_1074' WHERE name = 'sgd_gene';
UPDATE A2_BIOENTITYPROPERTY SET ensname='human_ensembl_gene, cow_ensembl_gene, ciona_intestinalis_ensembl_gene ' WHERE name = 'ortholog';
/
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';





