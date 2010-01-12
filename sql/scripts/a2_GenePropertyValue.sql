begin
--CAS
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 1, VALUE from AE2__GENE_CAS__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--CHEBI
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 2, VALUE from AE2__GENE_CHEBI__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--CHEMFORMULA
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 3, VALUE from AE2__GENE_CHEMFORMULA__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--DBXREF
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 4, VALUE from AE2__GENE_DBXREF__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--DESC
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 5, VALUE from AE2__GENE_DESC__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--DISEASE
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 6, VALUE from AE2__GENE_DISEASE__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--EMBL
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 7, VALUE from AE2__GENE_EMBL__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--ENSFAMILY
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 8, VALUE from AE2__GENE_ENSFAMILY__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--ENSGENE
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 9, VALUE from AE2__GENE_ENSGENE__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--ENSPROTEIN
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 10, VALUE from AE2__GENE_ENSPROTEIN__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--ENSTRANSCRIPT
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 11, VALUE from AE2__GENE_ENSTRANSCRIPT__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--GOTERM
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 12, VALUE from AE2__GENE_GOTERM__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--GO
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 13, VALUE from AE2__GENE_GO__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--HMDB
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 14, VALUE from AE2__GENE_HMDB__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--IDS
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 15, VALUE from AE2__GENE_IDS__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--IMAGE
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 16, VALUE from AE2__GENE_IMAGE__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--INTERPROTERM
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 17, VALUE from AE2__GENE_INTERPROTERM__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--INTERPRO
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 18, VALUE from AE2__GENE_INTERPRO__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--KEYWORD
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 19, VALUE from AE2__GENE_KEYWORD__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--LOCUSLINK
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 20, VALUE from AE2__GENE_LOCUSLINK__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--OMIM
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 21, VALUE from AE2__GENE_OMIM__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--ORF
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 22, VALUE from AE2__GENE_ORF__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--ORTHOLOG
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 23, VALUE from AE2__GENE_ORTHOLOG__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--PATHWAY
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 24, VALUE from AE2__GENE_PATHWAY__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--PROTEINNAME
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 25, VALUE from AE2__GENE_PROTEINNAME__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--REFSEQ
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 26, VALUE from AE2__GENE_REFSEQ__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--SYNONYM
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 27, VALUE from AE2__GENE_SYNONYM__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--UNIGENE
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 28, VALUE from AE2__GENE_UNIGENE__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--UNIPROTMETENZ
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 29, VALUE from AE2__GENE_UNIPROTMETENZ__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
--UNIPROT
insert into a2_GenePropertyValue(GenePropertyValueID,GeneID,GenePropertyID,Value) select a2_GenePropertyValue_seq.nextval, GENE_ID_KEY, 30, VALUE from AE2__GENE_UNIPROT__DM@dwprd where GENE_ID_KEY > 0 and length(VALUE) > 0;
end;

COMMIT WORK;




