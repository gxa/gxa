alter table a2_software add  url VARCHAR2(255);
alter table a2_software add  querytemplate VARCHAR2(510);

INSERT INTO a2_software (name, version, url, querytemplate) VALUES ('fungal', '9', 'http://fungi.ensembl.org/biomart/martservice?', '<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE Query><Query  virtualSchemaName = "fungi_mart_9" formatter = "TSV" header = "0" uniqueRows = "0" count = "" datasetConfigVersion = "0.7" ><Dataset name = "$DATA_SET" interface = "default" ><Attribute name = "ensembl_gene_id"/><Attribute name ="ensembl_transcript_id"/><Attribute name="$PROP_NAME"/></Dataset></Query>');

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

-- alter table A2_BIOENTITYPROPERTY add ensname VARCHAR2(1000);
--
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='ensembl_gene_id' WHERE name = 'ensgene';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='ensembl_peptide_id' WHERE name = 'ensprotein';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='description' WHERE name = 'description';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='external_gene_id' WHERE name = 'symbol';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='name_1006, go_cellular_component__dm_name_1006, go_molecular_function__dm_name_1006' WHERE name = 'goterm';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='go_biological_process_id, go_cellular_component_id, go_molecular_function_id' WHERE name = 'go';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='interpro' WHERE name = 'interpro';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='interpro_short_description' WHERE name = 'interproterm';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='hgnc_symbol' WHERE name = 'hgnc_symbol';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='uniprot_sptrembl, uniprot_swissprot_accession' WHERE name = 'uniprot';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='unigene' WHERE name = 'unigene';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='refseq_dna, refseq_peptide' WHERE name = 'refseq';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='embl' WHERE name = 'embl';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='mim_morbid_description' WHERE name = 'disease';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='ensfamily' WHERE name = 'family';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='family_description' WHERE name = 'ensfamily_description';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='ox_sgd_transcript__dm_dbprimary_acc_1074' WHERE name = 'sgd_transcript';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='ox_sgd_gene__dm_dbprimary_acc_1074' WHERE name = 'sgd_gene';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='human_ensembl_gene, cow_ensembl_gene, ciona_intestinalis_ensembl_gene, zebrafish_ensembl_gene, drosophila_ensembl_gene, chicken_ensembl_gene, mouse_ensembl_gene, rat_ensembl_gene, yeast_ensembl_gene, xenopus_ensembl_gene' WHERE name = 'ortholog';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='rgd_symbol' WHERE name = 'rgd_symbol';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='rgd' WHERE name = 'rgd';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='entrezgene' WHERE name = 'entrezgene';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='family' WHERE name = 'ensfamily';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='mirbase_id' WHERE name = 'mirbase_id';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='mirbase_accession' WHERE name = 'mirbase_accession';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='mgi_symbol' WHERE name = 'mgi_symbol';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='mgi_id' WHERE name = 'mgi_id';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='mgi_description' WHERE name = 'mgi_description';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='flybase_gene_id' WHERE name = 'flybase_gene_id';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='flybasename_gene' WHERE name = 'flybasename_gene';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='flybase_transcript_id' WHERE name = 'flybase_transcript_id';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='flybasename_transcript' WHERE name = 'flybasename_transcript';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='tair_locus_model' WHERE name = 'tair_locus_model';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='tair_locus' WHERE name = 'tair_locus';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='cint_jgi_v2' WHERE name = 'cint_jgi_v2';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='cint_jgi_v1' WHERE name = 'cint_jgi_v1';
-- /
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';
-- UPDATE A2_BIOENTITYPROPERTY SET ensname='' WHERE name = '';




CREATE SEQUENCE  "A2_ENSPROPERTY_SEQ"
    MINVALUE 1 MAXVALUE 1.00000000000000E+27
    INCREMENT BY 1 START WITH 1 CACHE 20
    NOORDER  NOCYCLE;

create table A2_ENSPROPERTY (
  ENSPROPERTYID NUMBER(22,0) CONSTRAINT NN_ENSPROPERTY_ID NOT NULL
, BIOENTITYPROPERTYID NUMBER(22,0) CONSTRAINT NN_EP_BEPROPERTYID NOT NULL
, name VARCHAR2(255) CONSTRAINT NN_ENSPROPERTY_NAME NOT NULL
);

ALTER TABLE A2_ENSPROPERTY
  ADD CONSTRAINT PK_ENSPROPERTY
    PRIMARY KEY (ENSPROPERTYID)
    ENABLE;

ALTER TABLE A2_ENSPROPERTY
  ADD CONSTRAINT FK_ENSPROPERTY_PROPERTY
    FOREIGN KEY (BIOENTITYPROPERTYID)
    REFERENCES A2_BIOENTITYPROPERTY (BIOENTITYPROPERTYID)
    ON DELETE CASCADE
    ENABLE;

CREATE OR REPLACE TRIGGER A2_ENSPROPERTY_INSERT
before insert on A2_ENSPROPERTY
for each row
begin
if(:new.ENSPROPERTYID is null) then
select A2_ENSPROPERTY_SEQ.nextval into :new.ENSPROPERTYID from dual;
end if;
end;

INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (19, 'mirbase_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (35, 'flybasename_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (17, 'family');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (36, 'flybase_transcript_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (18, 'unigene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (33, 'cint_jgi_v1');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (15, 'embl');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (34, 'flybase_gene_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (16, 'family_description');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (39, 'tair_locus');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (13, 'refseq_dna');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (13, 'refseq_peptide');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (14, 'entrezgene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (37, 'flybasename_transcript');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (11, 'interpro_short_description');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (38, 'tair_locus_model');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (12, 'uniprot_sptrembl');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (12, 'uniprot_swissprot_accession');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (21, 'mim_morbid_description');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (20, 'mirbase_accession');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (41, 'ox_sgd_transcript__dm_dbprimary_acc_1074');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (40, 'ox_sgd_gene__dm_dbprimary_acc_1074');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (22, 'hgnc_symbol');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'human_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'cow_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'ciona_intestinalis_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'zebrafish_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'drosophila_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'chicken_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'mouse_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'rat_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'yeast_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (24, 'xenopus_ensembl_gene');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (25, 'mgi_symbol');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (26, 'mgi_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (27, 'mgi_description');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (28, 'rgd_symbol');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (29, 'rgd');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (3, 'ensembl_gene_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (10, 'interpro');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (7, 'name_1006');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (7, 'go_cellular_component__dm_name_1006');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (7, 'go_molecular_function__dm_name_1006');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (6, 'external_gene_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (32, 'cint_jgi_v2');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (5, 'description');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (4, 'ensembl_peptide_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (8, 'go_biological_process_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (8, 'go_cellular_component_id');
INSERT INTO a2_ensproperty (BIOENTITYPROPERTYID, name) VALUES (8, 'go_molecular_function_id');