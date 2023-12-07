package ru.vladimirsazonov.SiteSearchEngine.services.search;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchResultResponse;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.RunApplicationException;
import ru.vladimirsazonov.SiteSearchEngine.model.Page;
import ru.vladimirsazonov.SiteSearchEngine.model.Site;
import ru.vladimirsazonov.SiteSearchEngine.model.Status;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl.DAO;
import ru.vladimirsazonov.SiteSearchEngine.services.morphology.MorphologyService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final MorphologyService morphologyService;
    private final DAO dao;

    @Override
    public SearchEngineResponse startSearch(String query, String siteUrl, int offset, int limit) {
        if (query == null || query.isBlank()) throw new RunApplicationException("Задан пустой поисковый запрос");
        if (siteUrl == null && dao.findAllSites().stream().noneMatch(site -> site.getStatus() == Status.INDEXED))
            throw new RunApplicationException("Сайты ещё не проиндексированы");
        else if (siteUrl != null) {
            Site site = dao.findSiteByUrl(siteUrl);
            if (site == null) throw new RunApplicationException("Данный сайт не указан в конфигурационном файле");
            if (site.getStatus() != Status.INDEXED)
                throw new RunApplicationException("Сайт ещё не проиндексирован");
        }
        List<String> lemmas = getLemmas(query, siteUrl, limit);
        List<Page> pages = getSearchResultPages(lemmas, siteUrl);
        if (pages.isEmpty()) return new SearchResultResponse(0, new SearchResultResponse.SearchResult[0]);
        SearchResultResponse.SearchResult[] data = getDataArray(getPagesAndRelevanceMap(pages, lemmas), lemmas);
        return new SearchResultResponse(data.length, data);
    }

    private SearchResultResponse.SearchResult[] getDataArray(Map<Page, Float> pagesAndRelevanceMap, List<String> lemmas) {
        return pagesAndRelevanceMap.entrySet().stream()
                .map(entry -> getSearchResult(entry.getKey(), entry.getValue(), lemmas))
                .sorted(Comparator.reverseOrder())
                .toArray(SearchResultResponse.SearchResult[]::new);
    }

    private SearchResultResponse.SearchResult getSearchResult(Page page, float relevance, List<String> lemmas) {
        Site site = page.getSite();
        Document document = Jsoup.parse(page.getContent());
        String snippet = getSnippet(document.text(), lemmas);
        return new SearchResultResponse.SearchResult(site.getUrl(), site.getName(), page.getPath(),
                document.title(), snippet, relevance);
    }

    private List<String> getLemmas(String query, String site, int upperPagesCountBound) {
        return morphologyService.getLemmasSet(query)
                .stream()
                .map(lemma -> Map.entry(lemma,
                        site == null ? dao.getTotalFrequencyForLemma(lemma)
                                : dao.getFrequencyForLemmaBySite(lemma, site)))
                .filter(entry -> entry.getValue() < upperPagesCountBound)
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<Page> getSearchResultPages(List<String> lemmas, String site) {
        if (lemmas.isEmpty()) return List.of();
        String lemma = lemmas.get(0);
        List<Page> pages = getPages(lemma, site);
        lemmas.stream()
                .skip(1L)
                .map(l -> getPages(l, site))
                .forEach(pageList -> pages.removeIf(page -> !pageList.contains(page)));
        return pages;
    }

    private List<Page> getPages(String lemma, String site) {
        return site == null ? dao.findPagesByLemma(lemma)
                : dao.findPagesByLemmaAndSiteUrl(lemma, site);
    }

    private Map<Page, Float> getPagesAndRelevanceMap(List<Page> pages, List<String> lemmas) {
        Map<Page, Float> pagesAndRelevanceMap = pages.stream()
                .collect(Collectors.toMap(page -> page,
                        page -> dao.getGradeSumByPageIdAndLemmas(page.getId(), lemmas)));
        float maxAbsRel = pagesAndRelevanceMap.values().stream()
                .max(Float::compareTo)
                .get();
        pagesAndRelevanceMap.forEach((page, rel) -> pagesAndRelevanceMap.replace(page, rel / maxAbsRel));
        return pagesAndRelevanceMap;
    }

    private String getSnippet(String text, List<String> lemmas) {
        StringBuilder builder = new StringBuilder();
        List<String> fragments = new ArrayList<>();
        Set<String> totalWordFormsForHandle = new HashSet<>();
        for (String lemma : lemmas) {
            Set<String> wordForms = morphologyService.getLemmasWordForms(lemma);
            totalWordFormsForHandle.addAll(wordForms);
            fragments.add(getTextFragment(text, wordForms));
        }
        fragments.sort((s1, s2) -> s2.length() - s1.length());
        for (int i = fragments.size() - 1; i > 0; i--)
            for (int j = i - 1; j >= 0; j--)
                if (fragments.get(j).contains(fragments.get(i))) fragments.remove(i--);
        fragments.forEach(fragment -> builder.append(handleFragment(fragment, totalWordFormsForHandle)).append("... "));
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    private String getTextFragment(String text, Set<String> lemmas) {
        for (String lemma : lemmas) {
            Matcher matcher = Pattern.compile("(?i)(?u)" + lemma).matcher(text);
            int index, start, end;
            int length = text.length();
            while (matcher.find()) {
                start = matcher.start();
                end = matcher.end();
                if (isAPartOfWord(start, end, text)) continue;
                if (start <= 20 || (index = text.lastIndexOf(" ", start - 20)) == -1) start = 0;
                else start = ++index;
                if (length < end + 20 || (index = text.indexOf(" ", end + 20)) == -1) end = length;
                else end = index;
                return text.substring(start, end);
            }
        }
        return "";
    }

    private boolean isAPartOfWord(int start, int end, String text) {
        return end < text.length() && Character.isAlphabetic(text.charAt(end)) ||
                start != 0 && Character.isAlphabetic(text.charAt(start - 1));
    }

    private String handleFragment(String fragment, Set<String> lemmas) {
        StringBuilder builder = new StringBuilder(fragment);
        char firstMatchedFragmentChar;
        for (String lemma : lemmas) {
            char firstLemmaChar = lemma.charAt(0);
            Matcher matcher = Pattern.compile("(?i)(?u)" + lemma).matcher(builder);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                if (isAPartOfWord(start, end, builder.toString())) continue;
                firstMatchedFragmentChar = builder.charAt(start);
                builder.replace(start, end, "<b>" + lemma + "</b>");
                if (firstMatchedFragmentChar != firstLemmaChar)
                    builder.replace(start + 3, start + 4, String.valueOf(firstMatchedFragmentChar));
            }
        }
        return builder.toString();
    }
}
