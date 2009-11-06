package ae3.service;

import ae3.model.ListResultRow;
import ae3.model.ListResultRowExperiment;
import ae3.service.structuredquery.AtlasStructuredQuery;
import ae3.service.structuredquery.AtlasStructuredQueryResult;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.service.structuredquery.ViewType;
import ae3.util.AtlasProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Represents a download event for exporting atlas list results to files
 * @author iemam
 *
 */
public class Download implements Runnable {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private int id;
	private final AtlasStructuredQuery query;

    private File outputFile;

    private long totalResults = 0;
    private long resultsRetrieved = 0;
    private static final int FRAME_SIZE = 50;

    public Download(int id, AtlasStructuredQuery query) throws IOException {
		this.query = query;
        this.id = id;

        this.outputFile =  File.createTempFile("listdl", ".zip", new File(System.getProperty("java.io.tmpdir")));
        this.outputFile.deleteOnExit();
	}

	public String getQuery() {
		return query.toString();
	}

	public double getProgress() {
        if(0 == getTotalResults()) return 0;
        if(getResultsRetrieved() == getTotalResults()) return 100;

        return Math.floor(100 * getResultsRetrieved() / getTotalResults());
	}

	public void run() {
		if(query != null) {
            try {
                ZipOutputStream zout =
                        new ZipOutputStream(new FileOutputStream(getOutputFile()));


                final AtlasStructuredQueryService sqs = AtlasSearchService
                        .instance()
                        .getStructQueryService();

                boolean first = true;

                query.setExpsPerGene(Integer.MAX_VALUE);
                query.setViewType(ViewType.LIST);
                while(first || getTotalResults() > getResultsRetrieved()) {
                    query.setStart((int) getResultsRetrieved());
                    query.setRowsPerPage(first ? FRAME_SIZE : (int) Math.min(FRAME_SIZE, getTotalResults() - getResultsRetrieved()));
                    AtlasStructuredQueryResult atlasResult = sqs.doStructuredAtlasQuery(query);
                    if(first) {
                        setTotalResults(atlasResult.getTotal());

                        log.info("Downloading query {}, expect total {} results", query.toString(), getTotalResults());
                        zout.putNextEntry(new ZipEntry("listdl.tab"));
                        outputHeader(zout);
                        first = false;
                    }

                    outputResults(atlasResult, zout);
                    incrementResultsRetrieved(atlasResult.getSize());
                }
                zout.closeEntry();
                zout.close();
            } catch (IOException e) {
                log.error("Error executing download for query {}, error {}", query, e.getMessage());
            }
		}
	}

    /**
     * Implement equality on query; prevents identical queries (within session) from being downloaded multiple times.
     */
    public boolean equals(Download d) {
        return d.getQuery().equals(this.getQuery());
    }

    /**
     * {@see {@link #equals}}
     */
    public int hashCode() {
        return getQuery().hashCode();
    }
	
	private void outputHeader(OutputStream out) throws IOException {
		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss");
        StringBuilder strBuf = new StringBuilder();

		strBuf.append("# Atlas data version: ").append(AtlasProperties.getProperty("atlas.data.release")).append("\n");
		strBuf.append("# Query: ").append(query.toString()).append("\n");
		strBuf.append("# Timestamp: ").append( formatter.format(today)).append("\n");
		
		strBuf.append("Gene name").append("\t").append("Gene identifier").append("\t").append("Organism").append("\t");
		strBuf.append("Experimental factor").append("\t").append("Factor value").append("\t");
		strBuf.append("Experiment accession").append("\t").append("Expression").append("\t").append("P-value").append("\n");

        out.write(strBuf.toString().getBytes("UTF-8"));
	}
	
	
	private void outputResults(AtlasStructuredQueryResult result, OutputStream out ) throws IOException {
        StringBuilder strBuf = new StringBuilder();
    	for (ListResultRow row : result.getListResults()) {
            String geneName = row.getGene_name();
            String geneIdentifier = row.getGene().getGeneIdentifier();
            String geneSpecies = row.getGene_species();
            String ef = row.getEf();
            String efv = row.getFv();

        	for(ListResultRowExperiment expRow: row.getExp_list()) {
        		strBuf.append(geneName);
	        	strBuf.append("\t");
	        	strBuf.append(geneIdentifier);
	        	strBuf.append("\t");
	        	strBuf.append(geneSpecies);
	        	strBuf.append("\t");
	        	strBuf.append(ef);
	        	strBuf.append("\t");
	        	strBuf.append(efv);
	        	strBuf.append("\t");
	        	strBuf.append(expRow.getExperimentAccession());
	        	strBuf.append("\t");
	        	strBuf.append(expRow.getUpdn().toString());
	        	strBuf.append("\t");
	        	strBuf.append(expRow.getPvalue());
	        	strBuf.append("\n");
        	}

            out.write(strBuf.toString().getBytes("UTF-8"));
            strBuf.setLength(0);
        }
    }

    private void incrementResultsRetrieved(long size) {
        resultsRetrieved += size;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(long total) {
        this.totalResults = total;
    }

    public int getId() {
        return id;
    }

    public long getResultsRetrieved() {
        return resultsRetrieved;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void finalize() {
        if(getOutputFile().exists())
            getOutputFile().delete();
    }

    public void setId(int id) {
        this.id = id;
    }
}
