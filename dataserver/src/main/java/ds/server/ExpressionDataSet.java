package ds.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.io.Serializable;
import ds.R.RUtilities;


/**
 * 
 * Object Describing the Expression Result data for one Study for a specific
 * 
 * @author hugo
 *
 */

public class ExpressionDataSet implements Serializable {
	

	private String[] factors = null;  // experimental factors for which the expression matrix were taken
	private HashMap <String, Set<String>> factorValues = null; // factor valeurs for each factor
	private static final long serialVersionUID = 1L;
	private String filename; // file from which the data was read
	private String assayId; // Assay DB id
	private String studyId; // Study DB id
	private Hashtable <String, Integer> deMap; // Map from DE DB id -> Matrix row id
	private Hashtable <String, Integer> sampleMap; // Map from BS DB id -> Matrix col id
	private HashMap<String, TreeMap<String, ArrayList[]>> databyEF;

	private int[] ar_BS = null; // array of col names (DB BS ids)
	private String[] ar_DE = null; // array of row names (DB DE ids)
	private String[] ar_EF = null; 
	private String[][] ar_EFV = null;
	private Vector<ExpressionDataSet> joinTo = null;
	private double[][] expressionMatrix = null;
	private HashMap<String, double[][]> sortedExpressionMatrix = null;
	private HashMap <String, int[]> sortOrder = null;
	
	private Hashtable<String, String> deAnn = null;
	private Hashtable<String, String> bsAnn = null;
	
	public ExpressionDataSet(){
		
		sortedExpressionMatrix = new HashMap<String, double[][]>();
		factorValues = new HashMap <String, Set<String>>();
		sortOrder = new HashMap <String, int[]> ();
		databyEF = new HashMap<String, TreeMap<String, ArrayList[]>>();
		
	}
	
	/**
	 * 
	 * Create a Map corresponding to the DeId -> rowId relationship
	 * 
	 * @return the Hashtable representing the Map
	 */
	
	private Hashtable<String, Integer> obtaindeMap() {

		    Hashtable<String, Integer> sampleMap = new Hashtable<String, Integer>();
			
			for (int a = 0 ; a < this.ar_DE.length; a++){
				sampleMap.put(ar_DE[a], a);
			}

			return sampleMap;
		

	}
	
	/**
	 * 
	 * 
	 */
	public void updatedeMap() {

		this.deMap = new Hashtable <String, Integer>();
		
		for (int a = 0 ; a < this.ar_DE.length; a++){
			this.deMap.put(ar_DE[a], a);
		}
		
	}
	
	
	

		
	/**
	 * 
	 * Return an ExpressionDataSet containing only the deids
	 * 
	 * @param deids
	 * @return
	 */
	
	public ExpressionDataSet getSliceByDE(Vector<String> deids){
		
		return getSliceByDE((String[])deids.toArray(new String [deids.size()]));
		
	}
	
	/**
	 * 
	 * Return an ExpressionDataSet containing only the deids
	 * 
	 * @param deids
	 * @return
	 */
	
	public ExpressionDataSet getSliceByDE(String deids){
		
		String[] ar_deids = { deids };
		
		return getSliceByDE(ar_deids);
		
	}
	
	/**
	 * 
	 * Return an ExpressionDataSet containing only the deids
	 * 
	 * @param deids
	 * @return
	 */
	
	public ExpressionDataSet getSliceByDE(String[] deids){
		
		double[][] temp_new_d_arDBC = new double[deids.length][this.getAr_BS().length];
		String [] temp_new_i_arDE = new String[deids.length];
		ExpressionDataSet ers = new ExpressionDataSet();
		
		ers.setAssayId(this.getAssayId());
		ers.setStudyId(this.getStudyId());
		ers.setAr_BS(this.getAr_BS());
		int matchIt = 0;
		
		for (int a = 0 ;  a< deids.length; a++){
			
			if (this.deMap.containsKey(deids[a])){
				int dePos = this.deMap.get(deids[a]);
				temp_new_i_arDE[matchIt] = this.ar_DE[dePos];
				matchIt++;
			}
			
		}
		
		double[][] new_d_arDBC = new double[matchIt][this.getAr_BS().length];
		String [] new_i_arDE = new String[matchIt];
		
		System.arraycopy(temp_new_d_arDBC,0,new_d_arDBC,0,matchIt);
		System.arraycopy(temp_new_i_arDE,0,new_i_arDE,0,matchIt);

		// Updating NetCDFs attributes necessary
		ers.setAr_DE(new_i_arDE);
		ers.setAr_BS(this.getAr_BS());
		ers.updatedeMap();
		ers.setAr_EF(this.getAr_EF());
		ers.setAr_EFV(this.getAr_EFV());
		ers.setFilename(this.getFilename());
		
		return ers;
		
	}
	
	/**
	 * 
	 * Will slice the first ten Design Elements and return the new ExpressionDataSet. Useful for benchmarking.
	 * 
	 * @param nbrGenes
	 * @return
	 */
	public ExpressionDataSet testBenchmark(int nbrGenes){
		
		//double[][] new_d_arDBC = new double[nbrGenes][this.getAr_BS().length];
		String [] new_i_arDE = new String[nbrGenes];
		ExpressionDataSet ers = new ExpressionDataSet();
		
		ers.setAssayId(this.getAssayId());
		ers.setStudyId(this.getStudyId());
		
		for (int a = 0 ;  a< nbrGenes; a++){
				new_i_arDE[a] = this.ar_DE[a];
		}
		
		ers.setAr_DE(new_i_arDE);
		ers.setAr_BS(this.getAr_BS());
		ers.updatedeMap();
		ers.setAr_EF(this.getAr_EF());
		ers.setAr_EFV(this.getAr_EFV());
		ers.setFilename(this.getFilename());
		
		return ers;
		
	}
	

	/* Setters and Getters */

	/**
	 * 
	 * Returns list of sample ids
	 * 
	 */
	public int[] getAr_BS() {
		return ar_BS;
	}

	public void setAr_BS(int[] ar_BS) {
		this.ar_BS = ar_BS;
	}

	/**
	 * 
	 * 
	 * @return list of Design Element ids as a string array
	 */
	public String[] getAr_DE() {
		return ar_DE;
	}

	public void setAr_DE(String[] ar_DE) {
		this.ar_DE = ar_DE;
	}

	public String[] getAr_EF() {
		return ar_EF;
	}

	public void setAr_EF(String[] ar_EF) {
		this.ar_EF = ar_EF;
	}

	public String[][] getAr_EFV() {
		return ar_EFV;
	}

	public void setAr_EFV(String[][] ar_EFV) {
		this.ar_EFV = ar_EFV;
	}

	public String getAssayId() {
		return assayId;
	}

	public void setAssayId(String assayId) {
		this.assayId = assayId;
	}

	public String getStudyId() {
		return studyId;
	}

	public void setStudyId(String studyId) {
		this.studyId = studyId;
	}

	public Hashtable<String, Integer> getDeMap() {
		return deMap;
	}

	public void setDeMap(Hashtable<String, Integer> deMap) {
		this.deMap = deMap;
	}

	public Hashtable<String, Integer> getSampleMap() {
		return sampleMap;
	}

	public void setSampleMap(Hashtable<String, Integer> sampleMap) {
		this.sampleMap = sampleMap;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/**
	 * 
	 * ExpressionDataSet from the same Experiment and ArrayDesingcan be added together
	 * 
	 * @param eds
	 * @return
	 */
	
	public ExpressionDataSet addExpressionDataSet(ExpressionDataSet eds){
		
		ExpressionDataSet new_eds = new ExpressionDataSet();
		boolean same_exp_adid = false;
		
		if (eds.getAssayId().equals(this.getAssayId()) && eds.getStudyId().equals(this.getStudyId())){
			same_exp_adid = true;
		}
		
		if (same_exp_adid){ // we only need to add the deid from one to the other
			
			String [] temp_new_i_arDE = new String[eds.getAr_DE().length+this.getAr_DE().length];
			System.arraycopy(this.getAr_DE(), 0, temp_new_i_arDE, 0, this.getAr_DE().length);
			
			new_eds.setAssayId(this.getAssayId());
			new_eds.setStudyId(this.getStudyId());
			new_eds.setAr_BS(this.getAr_BS());
			int matchIt = 0;
			
			for (int a = this.getAr_DE().length ;  a< temp_new_i_arDE.length; a++){
				
				temp_new_i_arDE[a] = eds.getAr_DE()[matchIt];
				matchIt++;

			}

			// Updating NetCDFs attributes necessary
			new_eds.setAr_DE(temp_new_i_arDE);
			new_eds.setAr_BS(this.getAr_BS());
			new_eds.setDeMap(new_eds.obtaindeMap());
			new_eds.setAr_EF(this.getAr_EF());
			new_eds.setAr_EFV(this.getAr_EFV());
			new_eds.setFilename(this.getFilename());

			
		}
		else { // add data at the end of the current ones
			
			return this;
		
		}
		
		return new_eds;
		
	}


	public Vector<ExpressionDataSet> getJoinTo() {
		return joinTo;
	}


	public void setJoinTo(Vector<ExpressionDataSet> joinTo) {
		this.joinTo = joinTo;
	}

	public Hashtable<String, String> getDeAnn() {
		return deAnn;
	}

	public void setDeAnn(Hashtable<String, String> deAnn) {
		this.deAnn = deAnn;
	}

	public Hashtable<String, String> getBsAnn() {
		return bsAnn;
	}

	public void setBsAnn(Hashtable<String, String> bsAnn) {
		this.bsAnn = bsAnn;
	}

	/**
	 * 
	 * Retrieves expression matrix for the object. If the matrix has not been loaded yet then it will be read from the NetCDf.
	 * 
	 * @return
	 */
	
	public double[][] getExpressionMatrix() {
		
		if (expressionMatrix == null){
			RUtilities ru = new RUtilities();
			
			try {
				this.expressionMatrix = ru.retrieveExpressionMatrix(this).getExpressionMatrix();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return expressionMatrix;
	}
	
	/**
	 * 
	 * Retrieves expression matrix order by a specific factor.
	 * 
	 * @param factor
	 * @return
	 */
	
	public double[][] getExpressionMatrixOrdered(String factor) {
		
		
			RUtilities ru = new RUtilities();
			
			if (!this.sortedExpressionMatrix.containsKey(factor)){
				try {
					this.sortedExpressionMatrix = ru.retrieveExpressionMatrix(this).getSortedExpressionMatrix();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		return this.sortedExpressionMatrix.get(factor);
	}

	public void setExpressionMatrix(double[][] expressionMatrix) {
		this.expressionMatrix = expressionMatrix;
	}
	
	public void setSortedExpressionMatrix(double[][] expressionMatrix, String factor) {
		this.sortedExpressionMatrix.put(factor, expressionMatrix);
	}
	
	public void setSortedExpressionMatrix(HashMap<String, double[][]> sem) {
		this.sortedExpressionMatrix = sem;
	}
	
	public HashMap<String, double[][]> getSortedExpressionMatrix() {
		return this.sortedExpressionMatrix;
	}

	public String[] getFactors() {

		return factors;
	}

	public void setFactors(Object[] factors) {
		String[] factorsStr = new String[factors.length];
		for(int i=0; i<factors.length; i++){
			factorsStr[i] = factors[i].toString();
		}
		this.factors = factorsStr;
	}

	public HashMap<String, Set<String>> getAllFactorValues() {
		return factorValues;
	}
	
	/**
	 * 
	 * Returns all factor values for the specified factor
	 * 
	 * @param factor
	 * @return
	 */
	
	public Set<String> getFactorValues(String factor) {	
		if (factors == null || factorValues.size() == 0)
			return null;
		if (!factorValues.containsKey(factor))
			return null;
		else
			return factorValues.get(factor);
	}

	public void setFactorValues(String factor, Set<String> factorValues) {
		this.factorValues.put(factor, factorValues);
	}
	
	public void setFactorValues(HashMap <String, Set<String>> efv) {
		this.factorValues = efv;
	}

	public HashMap<String, int[]> getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(HashMap<String, int[]> sortOrder) {
		this.sortOrder = sortOrder;
	}
	
	public int[] getSortOrder(String factor) {
		return sortOrder.get(factor);
	}

	public void setSortOrder(String factor, int[] order) {
		sortOrder.put(factor, order);
	}

	
	public void addEFdata(String factor,  TreeMap<String, ArrayList[]> fvMap){
		databyEF.put(factor, fvMap);
	}
	public ArrayList[] getDataByFV(String EF, String FV){
		ArrayList[] DEdata =null;
		DEdata = databyEF.get(EF).get(FV);
		return DEdata;
	}
	
	/*
	public void sortByFactor(String factor) {
		
		RUtilities ru = new RUtilities();
		
		try {
			this.expressionMatrix = ru.retrieveExpressionMatrix(this.filename, this.ar_DE, factor);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}*/
	
	
}
