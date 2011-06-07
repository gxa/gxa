package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;

import java.util.Collection;

public interface StatisticsBuilder {
    void addStatistics(EfvAttribute attributeIndex, ExperimentInfo experimentIndex, Collection<Integer> bioEntityIds);

    void addBioEntitiesForEfAttribute(EfvAttribute attributeIndex, Collection<Integer> bioEntityIds);

    void addBioEntitiesForEfvAttribute(EfvAttribute attributeIndex, Collection<Integer> bioEntityIds);

    void setScoresAcrossAllEfos(Multiset<Integer> scores);

    void addPvalueTstatRank(EfvAttribute attributeIndex, PTRank ptRank, ExperimentInfo experimentIndex, Integer bioEntityId);

    Statistics getStatistics();
}
