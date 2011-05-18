package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;

public interface StatisticsBuilder {
    void addStatistics(Integer attributeIndex, Integer experimentIndex, ConciseSet bioEntityIds);

    void addBioEntitiesForEfAttribute(Integer attributeIndex, ConciseSet bioEntityIds);

    void addBioEntitiesForEfvAttribute(Integer attributeIndex, ConciseSet bioEntityIds);

    void setScoresAcrossAllEfos(Multiset<Integer> scores);

    void addPvalueTstatRank(Integer attributeIndex, Float pValue, Short tStatRank, Integer experimentIndex, Integer bioEntityId);

    Statistics getStatistics();
}
