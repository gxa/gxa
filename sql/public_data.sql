/*
 A very special SQL file - actually, a configuration for the exporter
 the exporter is in the where.clause.awk script,
 which is called as follows:

 awk -v table=$TABLE_NAME -f where_clause.awk Schema/public_data.sql

 NB: We have slightly weird syntax for comments here: a line containing /*
 will be completely ignored; so will the line containing */
 /*
 That means while one-line comments are completely possible,
  they are not allowed in the end of the string
 */

SELECT * FROM A2_EXPERIMENT          e
  WHERE e.releasedate IS NOT NULL;

SELECT * FROM A2_ASSAY               a
  WHERE EXISTS (SELECT * FROM A2_EXPERIMENT e
    WHERE e.experimentid = a.experimentid AND e.releasedate IS NOT NULL);

SELECT * FROM A2_ASSAYPV             apv WHERE EXISTS (SELECT * FROM A2_ASSAY a JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE a.ASSAYID = apv.assayid AND e.releasedate IS NOT NULL);

SELECT * FROM A2_ASSAYPVONTOLOGY     apvo WHERE EXISTS (SELECT * FROM A2_ASSAYPV apv JOIN A2_ASSAY a ON a.assayid = apv.assayid JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE apvo.assaypvid = apv.assaypvid AND e.releasedate IS NOT NULL);

SELECT * FROM A2_ASSAYSAMPLE         asmpl WHERE EXISTS (SELECT * FROM A2_ASSAY a JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE a.ASSAYID = asmpl.assayid AND e.releasedate IS NOT NULL);

SELECT * FROM A2_EXPRESSIONANALYTICS ea
  WHERE EXISTS (SELECT * FROM A2_EXPERIMENT e
    WHERE e.experimentid = ea.experimentid AND e.releasedate IS NOT NULL);

SELECT * FROM A2_SAMPLE              s WHERE EXISTS (SELECT * FROM A2_ASSAYSAMPLE asmpl JOIN A2_ASSAY a ON a.assayid = asmpl.assayid JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE asmpl.sampleid = s.sampleid AND e.releasedate IS NOT NULL);

SELECT * FROM A2_SAMPLEPV            spv WHERE EXISTS (SELECT * FROM A2_SAMPLE s JOIN A2_ASSAYSAMPLE asmpl ON s.SAMPLEID = asmpl.SAMPLEID JOIN A2_ASSAY a ON a.assayid = asmpl.assayid JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE asmpl.sampleid = s.sampleid AND e.releasedate IS NOT NULL);

SELECT * FROM A2_SAMPLEPVONTOLOGY    spvo WHERE EXISTS (SELECT * FROM A2_SAMPLEPV spv JOIN A2_SAMPLE s on s.sampleid = spv.sampleid JOIN A2_ASSAYSAMPLE asmpl ON s.SAMPLEID = asmpl.SAMPLEID JOIN A2_ASSAY a ON a.assayid = asmpl.assayid JOIN A2_EXPERIMENT e
  ON e.experimentid = a.experimentid
  WHERE spvo.samplepvid = spv.samplepvid AND e.releasedate IS NOT NULL);
