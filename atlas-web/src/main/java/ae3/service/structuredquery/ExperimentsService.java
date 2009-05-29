    package ae3.service.structuredquery;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import uk.ac.ebi.ae3.indexbuilder.Experiment;
import uk.ac.ebi.ae3.indexbuilder.Efo;

/**
 * Experiments listing service class
 */
public class ExperimentsService {
    static final private Logger log = LoggerFactory.getLogger(ExperimentsService.class);

    private static ExperimentList mapExperiments(Iterable<Experiment> exps, Comparator<ExperimentRow> comparator) {
        final ExperimentList results = new ExperimentList(comparator);
        for(Experiment exp : exps){
            AtlasExperiment experiment = AtlasDao.getExperimentByIdDw(exp.getId());
            if (experiment != null) {
                results.add(new ExperimentRow(
                        experiment.getDwExpId(),
                        experiment.getAerExpName(),
                        experiment.getAerExpAccession(),
                        experiment.getAerExpDescription(),
                        exp.getPvalue(),
                        exp.getExpression(),
                        exp.getEf(),
                        exp.getEfv()));
            }
        }
        return results;
    }

    public static <T extends Comparable<T>> ExperimentList getExperiments(AtlasGene gene, final EfvTree<T> efvTree, final EfoTree<T> efoTree, Comparator<ExperimentRow> order){
        Iterable<String> efviter = new Iterable<String>() {
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    private Iterator<EfvTree.EfEfv<T>> treeit = efvTree.getNameSortedList().iterator();
                    public boolean hasNext() { return treeit.hasNext(); }
                    public String next() { return treeit.next().getEfEfvId(); }
                    public void remove() { }
                };
            }
        };
        Iterable<String> efoiter = new Iterable<String>() {
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    private Iterator<String> explit = efoTree.getExplicitEfos().iterator();
                    private Iterator<String> childit;
                    public boolean hasNext() {
                        return explit.hasNext() || (childit != null && childit.hasNext());
                    }
                    public String next() {
                        if(childit != null) {
                            String r = childit.next();
                            if(!childit.hasNext() && explit.hasNext())
                                childit = Efo.getEfo().getTermAndAllChildrenIds(explit.next()).iterator();
                            return r;
                        } else {
                            childit = Efo.getEfo().getTermAndAllChildrenIds(explit.next()).iterator();
                            return next();
                        }
                    }
                    public void remove() { }
                };
            }
        };
        return mapExperiments(gene.getExpermientsTable().findByEfEfvEfoSet(efviter, efoiter), order);
    }

    public static ExperimentList getExperiments(AtlasGene gene, String ef, String efv, Comparator<ExperimentRow> order){
        return mapExperiments(gene.getExpermientsTable().findByEfEfv(ef, efv), order);
    }

    public static ExperimentList getExperiments(AtlasGene gene, Collection<String> efo, Comparator<ExperimentRow> order){
        return mapExperiments(gene.getExpermientsTable().findByEfoSet(efo), order);
    }

    public static ExperimentList getExperiments(AtlasGene gene, String efo, Comparator<ExperimentRow> order){
        return mapExperiments(gene.getExpermientsTable().findByEfoSet(Efo.getEfo().getTermAndAllChildrenIds(efo)), order);
    }

    public void shutdown() {
    }
}