CREATE OR REPLACE VIEW "VWSAMPLEASSAY" ("ASSAYID", "SAMPLEID", "ASSAYACCESSION") AS select asa.ASSAYID, asa.SAMPLEID, a.Accession AssayAccession
from a2_assaysample asa
join a2_assay a on a.AssayID = asa.AssayID;
