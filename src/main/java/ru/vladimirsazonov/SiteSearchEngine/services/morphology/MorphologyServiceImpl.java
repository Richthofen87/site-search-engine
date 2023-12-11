package ru.vladimirsazonov.SiteSearchEngine.services.morphology;

import com.github.demidko.aot.WordformMeaning;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MorphologyServiceImpl implements MorphologyService {

    private static LuceneMorphology luceneMorph;

    static {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Integer> getLemmasAndFrequenciesMap(String text) {
        List<String> lemmas = getLemmasList(text);
        if (lemmas.isEmpty()) return Map.of();
        Map<String, Integer> result = new HashMap<>();
        lemmas.forEach(lemma -> result.merge(lemma, 1, (oldV, newV) -> ++oldV));
        return result;
    }

    @Override
    public Set<String> getLemmasWordForms(String lemma) {
        try {
            return WordformMeaning.lookupForMeanings(lemma).stream()
                    .flatMap(wordForm -> wordForm.getTransformations().stream())
                    .map(WordformMeaning::toString)
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            return Set.of();
        }
    }

    @Override
    public Set<String> getLemmasSet(String text) {
        List<String> lemmas;
        if ((lemmas = getLemmasList(text)).isEmpty()) return Set.of();
        return new HashSet<>(lemmas);
    }

    private List<String> getLemmasList(String text) {
        if (text.isBlank()) return List.of();
        String[] words = text.split("[^А-ЯЁа-яё]+");
        return Arrays.stream(words)
                .filter(word -> !word.isBlank())
                .map(String::toLowerCase)
                .flatMap(word -> luceneMorph.getMorphInfo(word).stream())
                .filter(morphInfo -> !morphInfo.contains("МЕЖД") && !morphInfo.contains("ПРЕДЛ")
                        && !morphInfo.contains("СОЮЗ") && !morphInfo.contains("МС")
                        && !morphInfo.contains("ЧАСТ") && !morphInfo.contains("КР_ПРИЛ"))
                .map(morphInfo -> morphInfo.substring(0, morphInfo.indexOf("|")))
                .toList();
    }
}
