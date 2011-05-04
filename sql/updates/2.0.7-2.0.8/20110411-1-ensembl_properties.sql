alter table A2_ORGANISM add ensname VARCHAR2(255);

UPDATE A2_ORGANISM SET ensname='agambiae_eg_gene' WHERE name = 'anopheles gambiae';
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

alter table A2_BIOENTITYPROPERTY add ensname VARCHAR2(1000);

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
UPDATE A2_BIOENTITYPROPERTY SET ensname='human_ensembl_gene, cow_ensembl_gene, ciona_intestinalis_ensembl_gene, zebrafish_ensembl_gene, drosophila_ensembl_gene, chicken_ensembl_gene, mouse_ensembl_gene, rat_ensembl_gene, yeast_ensembl_gene, xenopus_ensembl_gene' WHERE name = 'ortholog';
UPDATE A2_BIOENTITYPROPERTY SET ensname='rgd_symbol' WHERE name = 'rgd_symbol';
UPDATE A2_BIOENTITYPROPERTY SET ensname='rgd' WHERE name = 'rgd';
UPDATE A2_BIOENTITYPROPERTY SET ensname='entrezgene' WHERE name = 'entrezgene';
UPDATE A2_BIOENTITYPROPERTY SET ensname='family' WHERE name = 'ensfamily';
UPDATE A2_BIOENTITYPROPERTY SET ensname='mirbase_id' WHERE name = 'mirbase_id';
UPDATE A2_BIOENTITYPROPERTY SET ensname='mirbase_accession' WHERE name = 'mirbase_accession';
UPDATE A2_BIOENTITYPROPERTY SET ensname='mgi_symbol' WHERE name = 'mgi_symbol';
UPDATE A2_BIOENTITYPROPERTY SET ensname='mgi_id' WHERE name = 'mgi_id';
UPDATE A2_BIOENTITYPROPERTY SET ensname='mgi_description' WHERE name = 'mgi_description';
UPDATE A2_BIOENTITYPROPERTY SET ensname='flybase_gene_id' WHERE name = 'flybase_gene_id';
UPDATE A2_BIOENTITYPROPERTY SET ensname='flybasename_gene' WHERE name = 'flybasename_gene';
UPDATE A2_BIOENTITYPROPERTY SET ensname='flybase_transcript_id' WHERE name = 'flybase_transcript_id';
UPDATE A2_BIOENTITYPROPERTY SET ensname='flybasename_transcript' WHERE name = 'flybasename_transcript';
UPDATE A2_BIOENTITYPROPERTY SET ensname='tair_locus_model' WHERE name = 'tair_locus_model';
UPDATE A2_BIOENTITYPROPERTY SET ensname='tair_locus' WHERE name = 'tair_locus';
UPDATE A2_BIOENTITYPROPERTY SET ensname='cint_jgi_v2' WHERE name = 'cint_jgi_v2';
UPDATE A2_BIOENTITYPROPERTY SET ensname='cint_jgi_v1' WHERE name = 'cint_jgi_v1';
/
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';





