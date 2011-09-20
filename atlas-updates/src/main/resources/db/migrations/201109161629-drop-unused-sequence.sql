drop sequence a2_schemachanges_seq;

-- make sure sequences are intact
call atlasmgr.RebuildSequences();
