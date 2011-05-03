package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;

import java.util.Collection;

public interface StatisticsBuilder {
    void addStatistics(Integer attributeIndex, Integer experimentIndex, Collection<Integer> bioEntityIds);

    void addBioEntitiesForEfAttribute(Integer attributeIndex, Collection<Integer> bioEntityIds);

    void addBioEntitiesForEfvAttribute(Integer attributeIndex, Collection<Integer> bioEntityIds);

    void setScoresAcrossAllEfos(Multiset<Integer> scores);

    void addPvalueTstatRank(Integer attributeIndex, Float pValue, Short tStatRank, Integer experimentIndex, Integer bioEntityId);

    Statistics getStatistics();
}
