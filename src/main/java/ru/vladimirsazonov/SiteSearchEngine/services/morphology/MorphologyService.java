package ru.vladimirsazonov.SiteSearchEngine.services.morphology;

import java.util.Map;
import java.util.Set;

public interface MorphologyService {
    Set<String> getLemmasSet(String text);
    Map<String, Integer> getLemmasAndFrequenciesMap(String text);
}
