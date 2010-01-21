package uk.ac.ebi.gxa.netcdf.generator.helper;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Oct-2009
 */
public class DesignElementSlicer extends CallableSlicer<Void> {
    // required initial resources
    private final ArrayDesign arrayDesign;
    private final DataSlice dataSlice;

    public DesignElementSlicer(ExecutorService service, ArrayDesign arrayDesign, DataSlice dataSlice) {
        super(service);
        this.arrayDesign = arrayDesign;
        this.dataSlice = dataSlice;
    }

    public Void call() throws Exception {
        getLog().debug("Fetching design element data for " + arrayDesign.getAccession());
        // and store
        dataSlice.storeDesignElements(arrayDesign.getDesignElements());

        // genes for this experiment were prefetched -
        // compare to design elements and store, correctly indexed
        // fetch design elements specific to this array design
        getLog().debug("Indexing gene data by design element ID for " + arrayDesign.getAccession());
        fetchGenesTask.get();
        getLog().debug("Gene data for " + arrayDesign.getAccession() + " acquired");

        Set<Integer> deKeys = dataSlice.getDesignElements().keySet();
        for (Gene gene : fetchGenesTask.get()) {
            // whether or not we can map this gene
            boolean mapped = false;

            for (int designElementID : gene.getDesignElementIDs()) {
                // check this gene maps to a stored design element
                if (deKeys.contains(designElementID)) {
                    dataSlice.storeGene(designElementID, gene);

                    // remove from the unmapped list if necessary
                    synchronized (unmappedGenes) {
                        if (unmappedGenes.contains(gene)) {
                            unmappedGenes.remove(gene);
                        }
                    }

                    mapped = true;
                }
            }

            if (!mapped) {
                // exclude this gene - no design element resolvable,
                // or maybe it's just from a different array design
                synchronized (unmappedGenes) {
                    unmappedGenes.add(gene);
                }
            }
        }

        // expression analyses for this experiment were prefetched -
        // compare to design elements and store, correctly indexed
        getLog().debug("Indexing analytics data by design element ID for " + arrayDesign.getAccession());
        fetchAnalyticsTask.get();
        getLog().debug("Analytics data for " + arrayDesign.getAccession() + " acquired");
        for (ExpressionAnalysis analysis : fetchAnalyticsTask.get()) {
            if (deKeys.contains(analysis.getDesignElementID())) {
                dataSlice.storeExpressionAnalysis(
                        analysis.getDesignElementID(), analysis);

                // remove from the unmapped list if necessary
                synchronized (unmappedAnalytics) {
                    if (unmappedAnalytics.contains(analysis)) {
                        unmappedAnalytics.remove(analysis);
                    }
                }
            }
            else {
                // exclude this gene - design element not resolvable,
                // or maybe it's just from a different array design
                synchronized (unmappedAnalytics) {
                    unmappedAnalytics.add(analysis);
                }
            }
        }

        getLog().debug("Design Element/Gene/Analytics data for " + arrayDesign.getAccession() + " stored");
        return null;
    }
}
