package ae3.service;

import ae3.service.structuredquery.AtlasStructuredQuery;
import ae3.service.structuredquery.AtlasStructuredQueryResult;

/**
 * Represents a download event for exporting atlas list results to files
 * @author iemam
 *
 */
public class Download {
	private String file;
	private double progress=0;
	private AtlasStructuredQuery query;
	private long size=1;
	private StringBuilder strBuf;
	
	public Download(AtlasStructuredQuery query){
		this.query = query;
		strBuf = new StringBuilder();
	}

	public String getQuery() {
		return query.toString();
	}

	public void setQuery(AtlasStructuredQuery query) {
		this.query = query;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public double getProgress() {
		return Math.ceil((progress/size) * 100);
	}
	
	public void setProgress(int progress) {
		this.progress = progress;
	}
	
	public StringBuilder getFileContents(){
		return strBuf;
	}
	
	public void doDownload(){
		if(query != null){
			
			AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().getStructQueryService().doStructuredAtlasQuery(query);
			appendResults(atlasResult, strBuf);
			progress++;
			long total = atlasResult.getTotal();
			size = (long)Math.ceil(total/query.getRowsPerPage()+0.5);
			if(total > query.getRowsPerPage()){
				
				for(int i=1; i<size; i++){
					query.setStart(i*query.getRowsPerPage());

					AtlasStructuredQueryResult atlasResult2 = ArrayExpressSearchService.instance().getStructQueryService().doStructuredAtlasQuery(query);
					appendResults(atlasResult2, strBuf);
					progress++;
				}

			}

//			FileOutputStream fos = new FileOutputStream("/Volumes/Workspace/Projects/atlas-1.0/out.csv");
//			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
//			PrintWriter pw = new PrintWriter(osw);
//			pw.print(strBuf);
//			pw.close();
//			request.setAttribute("filename", "out.csv");

		}
	}
	
	private void appendResults(AtlasStructuredQueryResult result, StringBuilder strBuf ){
    	for (ListResultRow row: result.getListResults()){
        	for(ListResultRowExperiment expRow: row.getExp_list()){
        		strBuf.append(row.getGene_name());
	        	strBuf.append("\t");
	        	strBuf.append(row.getGene().getGeneIdentifier());
	        	strBuf.append("\t");
	        	strBuf.append(row.getGene_species());
	        	strBuf.append("\t");
	        	strBuf.append(row.getEf());
	        	strBuf.append("\t");
	        	strBuf.append(row.getFv());
	        	strBuf.append("\t");
	        	strBuf.append(expRow.getExperimentAccession());
	        	strBuf.append("\t");
	        	strBuf.append(expRow.getUpdn().toString());
	        	strBuf.append("\t");
	        	strBuf.append(expRow.getPvalue());
	        	strBuf.append("\n");
        	}
        }
    }
}
