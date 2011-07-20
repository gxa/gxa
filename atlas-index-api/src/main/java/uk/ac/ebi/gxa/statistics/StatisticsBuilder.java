package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;

import java.util.Collection;

public interface StatisticsBuilder {
    void addStatistics(EfvAttribute attribute, ExperimentInfo experiment, Collection<Integer> bioEntityIds);

    void addBioEntitiesForEfAttribute(EfvAttribute attribute, Collection<Integer> bioEntityIds);

    void addBioEntitiesForEfvAttribute(EfvAttribute attribute, Collection<Integer> bioEntityIds);

    void setScoresAcrossAllEfos(Multiset<Integer> scores);

    void addPvalueTstatRank(EfvAttribute attribute, PTRank ptRank, ExperimentInfo experiment, Integer bioEntityId);

    Statistics getStatistics();
}
