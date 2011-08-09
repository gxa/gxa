/*
 A very special SQL file - actually, a configuration for the exporter
 the exporter is in the where_clause.awk script,
 which is called as follows:

 awk -v table=$TABLE_NAME -f where_clause.awk public_data.sql

 NB: We have slightly weird syntax for comments here: a line containing / followed by *
 will be completely ignored; so will the line containing */
 /*
 That means while one-line comments are completely possible,
  they are not allowed in the end of the string
 */

SELECT * FROM A2_EXPERIMENT          e
  WHERE e.private = 0;

SELECT * FROM A2_ASSAY               a
  WHERE EXISTS (SELECT * FROM A2_EXPERIMENT e
    WHERE e.experimentid = a.experimentid AND e.private = 0);

SELECT * FROM A2_ASSAYPV             apv WHERE EXISTS (SELECT * FROM A2_ASSAY a JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE a.ASSAYID = apv.assayid AND e.private = 0);

SELECT * FROM A2_ASSAYPVONTOLOGY     apvo WHERE EXISTS (SELECT * FROM A2_ASSAYPV apv JOIN A2_ASSAY a ON a.assayid = apv.assayid JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE apvo.assaypvid = apv.assaypvid AND e.private = 0);

SELECT * FROM A2_ASSAYSAMPLE         asmpl WHERE EXISTS (SELECT * FROM A2_ASSAY a JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE a.ASSAYID = asmpl.assayid AND e.private = 0);

SELECT * FROM A2_SAMPLE              s WHERE EXISTS (SELECT * FROM A2_ASSAYSAMPLE asmpl JOIN A2_ASSAY a ON a.assayid = asmpl.assayid JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE asmpl.sampleid = s.sampleid AND e.private = 0);

SELECT * FROM A2_SAMPLEPV            spv WHERE EXISTS (
  SELECT * FROM A2_SAMPLE s
           JOIN A2_ASSAYSAMPLE asmpl ON s.SAMPLEID = asmpl.SAMPLEID
           JOIN A2_ASSAY a ON a.assayid = asmpl.assayid
           JOIN A2_EXPERIMENT e ON e.experimentid = a.experimentid
  WHERE spv.sampleid = s.sampleid AND e.private = 0);

SELECT * FROM A2_SAMPLEPVONTOLOGY    spvo WHERE EXISTS (SELECT * FROM A2_SAMPLEPV spv JOIN A2_SAMPLE s on s.sampleid = spv.sampleid JOIN A2_ASSAYSAMPLE asmpl ON s.SAMPLEID = asmpl.SAMPLEID JOIN A2_ASSAY a ON a.assayid = asmpl.assayid JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE spvo.samplepvid = spv.samplepvid AND e.private = 0);

SELECT * FROM A2_SCHEMACHANGES       sc
  WHERE sc.changeid IN (SELECT MAX(changeid) FROM A2_SCHEMACHANGES);
