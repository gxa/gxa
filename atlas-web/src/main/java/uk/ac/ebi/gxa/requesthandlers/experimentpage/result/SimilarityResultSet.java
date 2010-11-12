/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.requesthandlers.experimentpage.result;

import uk.ac.ebi.rcloud.server.RType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Represents a Similarity Result
 * <p/>
 * fixme - this should really be moved to atlas-web, but the tests are using it
 *
 * @author hugo
 */

public class SimilarityResultSet {

    private Vector<SimilarityResult> results; // Similarity results, a vector where each row is a similarity result
    private String[] colNames = null;  // Description of the different columns making the vector
    private String methodUsed; // Similarity method used
    private String sourceNetCDF; // NetCDF file from which the analysis come
    private String targetDesignElementId; // Id of the target gene
    private String targetArrayDesignId; // Id of the target ArrayDesign
    private String targetExperimentId;
    private String netCDFsPath;
    private HashMap<String, Double> scores = new HashMap<String, Double>();

    public SimilarityResultSet() {
        results = new Vector<SimilarityResult>();
    }

    public SimilarityResultSet(String eid, String deid, String adid, String netCDFsPath) {
        this.targetExperimentId = eid;
        this.targetArrayDesignId = adid;
        this.targetDesignElementId = deid;
        this.netCDFsPath = netCDFsPath;
        if (!isEmpty(eid) && !isEmpty(deid) && !isEmpty(adid)) {
            sourceNetCDF = netCDFsPath + "/" + targetExperimentId + "_" + targetArrayDesignId + ".nc";
        }
        results = new Vector<SimilarityResult>();
    }

    /**
     * Get a specific similarity result
     *
     * @param it the position number to be returned
     * @return SimilarityResult as the given position
     */
    public SimilarityResult getResult(int it) {
        return results.size() >= it ? results.get(it) : null;
    }

    /**
     * Get size of the SimilarityResultSet
     *
     * @return
     */
    public int size() {
        return results.size();
    }

    /**
     * Retrieve the ids of the most similar genes
     *
     * @return an array of string containg the ids
     */
    public String[] getDesignElementIds() {

        String[] deIds = new String[results.size()];

        for (int a = 0; a < results.size(); a++) {
            deIds[a] = results.get(a).getDesignElementId();
        }

        return deIds;

    }

    /**
     * Retrieve the scores of the most similar genes
     *
     * @return an array of string containg the scores
     */
    public double[] retrieveScores1() {

        double[] scores = new double[results.size()];

        for (int a = 0; a < results.size(); a++) {
            scores[a] = results.get(a).getScore_row1();
        }

        return scores;

    }

    /**
     * Retrieve the scores (row 2) of the most similar genes
     *
     * @return an array of string containg the scores
     */
    public double[] retrieveScores2() {

        double[] scores = new double[results.size()];

        for (int a = 0; a < results.size(); a++) {

            scores[a] = results.get(a).getScore_row2();

        }

        return scores;

    }

    /**
     * Retrieve the scores (row 3) of the most similar genes
     *
     * @return an array of string containg the scores
     */
    public double[] retrieveScores3() {

        double[] scores = new double[results.size()];

        for (int a = 0; a < results.size(); a++) {

            scores[a] = results.get(a).getScore_row3();

        }

        return scores;

    }

    /**
     * Add a SimilarityResult to the SimilarityResultSet. It will be added at the end of the vector.
     *
     * @param sr SimilarityResult to be added
     */
    public void addResult(SimilarityResult sr, double score) {
        results.add(sr);
        scores.put(sr.getGeneId(), score);
    }

    public void addResult(SimilarityResult sr) {
        results.add(sr);
    }

    /**
     * Load an RMatrix into an actual SimilarityResult Java Object
     *
     * @param ro
     */
    public void loadResult(RMatrix ro) {

        RChar dimnames = (RChar) ro.getDimnames().getValue()[0];
        int dim1 = ro.getDim()[0];
        int dim2 = ro.getDim()[1];

        double[] tempResultMatrix;

        RNumeric resultMatrix_R = (RNumeric) ro.getValue();

        tempResultMatrix = resultMatrix_R.getValue();

        double[][] resultMatrix = new double[dim1][dim2];

        int it = 0;
        for (int d2 = 0; d2 < dim2; d2++) {

            for (int d1 = 0; d1 < dim1; d1++) {
                resultMatrix[d1][d2] = tempResultMatrix[it];
                it++;
            }

        }

        for (int d1 = 0; d1 < dim1; d1++) {
            SimilarityResult sr = new SimilarityResult();

            sr.setDesignElementId(dimnames.getValue()[d1]);
            String targetDeId = Double.toString(resultMatrix[d1][0]);
            sr.setTargetDesignElementId(targetDeId);
            sr.setScore_row1(resultMatrix[d1][1]);
            if (dim2 > 2) {
                sr.setScore_row2(resultMatrix[d1][2]);
            }
            if (dim2 > 3) {
                sr.setScore_row3(resultMatrix[d1][3]);
            }

            this.addResult(sr);

        }


    }


    public boolean loadResult(RDataFrame rdf) {
        boolean success = true;

        try {
            RList d = rdf.getData();
            String[] names = d.getNames();
            RObject[] values = d.getValue();
            RArray gnIds = (RArray) values[0];
            RArray deIds = (RArray) values[1];
            RNumeric scores = (RNumeric) values[2];
            for (int i = 0; i < rdf.getRowNames().length; i++) {
                SimilarityResult sr = new SimilarityResult();

                // Please do NOT change the type into RNumeric: ids MUST be integer, and if the problem is that they're
                // returned as floats, you're doing it wrong somewhere else.
                sr.setGeneId(String.format("%d", ((RInteger) gnIds.getValue()).getValue()[i]));
                sr.setDesignElementId(String.format("%d", ((RInteger) deIds.getValue()).getValue()[i]));
                sr.setScore_row1(scores.getValue()[i]);
                addResult(sr, scores.getValue()[i]);
            }
        }
        catch (Exception e) {
            success = false;
            e.printStackTrace();
        }
        return success;
    }


    public HashMap<String, Double> getScores() {
        return scores;
    }

    /**
     * Class representing a single similarity relation
     *
     * @author hugo
     */
    public class SimilarityResult {

        private String geneId; // gene on which the similarity is run
        private String designElementId; // Id of the original gene
        private String targetDesignElementId; // Id of the target gene
        private String geneName;

        private Hashtable<String, Double> scores = new Hashtable<String, Double>();


        public String getGeneId() {
            return geneId;
        }

        public void setGeneId(String geneId) {
            this.geneId = geneId;
        }

        public String getDesignElementId() {
            return designElementId;
        }

        public void setDesignElementId(String designElementId) {
            this.designElementId = designElementId;
        }

        public String getTargetDesignElementId() {
            return targetDesignElementId;
        }

        public void setTargetDesignElementId(String targetDesignElementId) {
            this.targetDesignElementId = targetDesignElementId;
        }

        public boolean hasScore_row1() {
            return scores.containsKey("score1");
        }

        public double getScore_row1() {

            return scores.get("score1").doubleValue();

        }

        public void setScore_row1(double scr1) {

            scores.put("score1", scr1);

        }

        public boolean hasScore_row2() {
            return scores.containsKey("score2");
        }

        public double getScore_row2() {

            return scores.get("score2").doubleValue();

        }

        public void setScore_row2(double scr2) {

            scores.put("score2", scr2);

        }

        public boolean hasScore_row3() {
            return scores.containsKey("score3");
        }

        public double getScore_row3() {

            return scores.get("score3").doubleValue();

        }

        public void setScore_row3(double scr3) {

            scores.put("score3", scr3);

        }

        public String getGeneName() {
            return geneName;
        }

        public void setGeneName(String geneName) {
            this.geneName = geneName;
        }


    }

    public String[] getColNames() {
        return colNames;
    }

    /**
     * Retrieve column name
     *
     * @param it
     * @return
     */
    public String getColNames(int it) {
        return colNames.length >= it ? colNames[it] : null;
    }

    /**
     * Set column names
     *
     * @param colNames
     */
    public void setColNames(String[] colNames) {
        this.colNames = colNames;
    }

    public String getMethodUsed() {
        return methodUsed;
    }

    public void setMethodUsed(String methodUsed) {
        this.methodUsed = methodUsed;
    }

    public String getSourceNetCDF() {
        return sourceNetCDF;
    }

    public void setSourceNetCDF(String sourceNetCDf) {
        this.sourceNetCDF = sourceNetCDf;
    }

    public String getTargetDesignElementId() {
        return targetDesignElementId;
    }

    public void setTargetDesignElementId(String targetDesignElementId) {
        this.targetDesignElementId = targetDesignElementId;
    }

    public ArrayList<String> getSimGeneIDs() {
        ArrayList<String> geneIds = new ArrayList<String>();
        for (SimilarityResult sim : results)
            if (!"0".equals(sim.getGeneId())) {
                geneIds.add(sim.getGeneId());
            }
        return geneIds;
    }

    public Vector<SimilarityResult> getResults() {
        return results;
    }

    public void setResults(Vector<SimilarityResult> results) {
        this.results = results;
    }

    private static boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }
}
