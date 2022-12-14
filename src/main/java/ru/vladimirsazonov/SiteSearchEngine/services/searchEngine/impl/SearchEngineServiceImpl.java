package ru.vladimirsazonov.SiteSearchEngine.services.searchEngine.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import ru.vladimirsazonov.SiteSearchEngine.config.AppProps;
import ru.vladimirsazonov.SiteSearchEngine.dto.*;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.IndexPageException;
import ru.vladimirsazonov.SiteSearchEngine.model.*;
import ru.vladimirsazonov.SiteSearchEngine.services.morphology.MorphologyService;
import ru.vladimirsazonov.SiteSearchEngine.services.searchEngine.SearchEngineService;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.DisableSiteSearchException;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.EmptySearchQueryException;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.RunApplicationException;
import ru.vladimirsazonov.SiteSearchEngine.repositories.SelectorRepository;
import com.github.demidko.aot.WordformMeaning;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchEngineServiceImpl implements SearchEngineService {

    private final int PARALLELISM_LEVEL = Runtime.getRuntime().availableProcessors();
    private final Map<Integer, ScheduledFuture<?>> scheduledFutureMap = new ConcurrentHashMap<>();
    private final AppProps appProps;
    private final SelectorRepository selectorRepository;
    private final MorphologyService morphologyService;
    private final DAO dao;
    private List<AppProps.SiteData> siteDataList;
    private String userAgentName;
    private ScheduledExecutorService scheduledThreadPoolExecutor;
    private ForkJoinPool forkJoinPool;
    private ExecutorService threadPoolExecutor;
    private boolean startFlag;
    private String baseUrlForSinglePage;

    public void setSiteDataListAndUserAgentName() {
        siteDataList = appProps.getSites();
        userAgentName = appProps.getUserAgentName();
    }

    @Override
    public SearchEngineResponse startIndexing() {
        if (siteDataList.isEmpty()) throw new RunApplicationException("?? ?????????????????????????????? ?????????? ???? ?????????????? ??????????!");
        return startIndex(siteDataList, false);
    }

    @Override
    public SearchEngineResponse stopIndexing() {
        if (!startFlag) throw new RunApplicationException("???????????????????? ?????? ???? ????????????????!");
        scheduledThreadPoolExecutor.shutdownNow();
        threadPoolExecutor.shutdownNow();
        forkJoinPool.shutdownNow();
        dao.setAllSitesStatusFailed();
        startFlag = false;
        return new SearchEngineResponse();
    }

    @Override
    public SearchEngineResponse indexPage(String url) {
        Optional<AppProps.SiteData> siteDataOptional = siteDataList
                .stream()
                .filter(data -> url.startsWith(data.getUrl()))
                .findAny();
        if (siteDataOptional.isEmpty()) throw new IndexPageException();
        dao.setBaseUrlForSinglePage(baseUrlForSinglePage = siteDataOptional.get().getUrl());
        return startIndex(List.of(new AppProps.SiteData(siteDataOptional.get().getName(),
                url)), true);
    }

    @Override
    public SearchEngineResponse getStatistics() {
        if (siteDataList == null || userAgentName == null) setSiteDataListAndUserAgentName();
        List<SiteStatistics> detailed = new ArrayList<>();
        siteDataList.forEach(siteData -> {
            String name = siteData.getName();
            Site site = dao.findSiteByName(name);
            if (site == null) return;
            String url = siteData.getUrl();
            String status = site.getStatus().toString();
            String statusTime = site.getStatusTime().toString();
            String error = site.getLastError();
            int siteId = site.getId();
            int pages = dao.getPagesCountBySiteId(siteId);
            int lemmas = dao.getLemmasCountBySiteId(siteId);
            if (error != null)
                detailed.add(new ErrorSiteStatistics(url, name, status, statusTime, error,
                        pages, lemmas));
            else detailed.add(new SiteStatistics(url, name, status, statusTime,
                    pages, lemmas));
        });
        return new StatResponse(new StatResponse.Statistics(
                new StatResponse.Total(siteDataList.size(), dao.getPagesTotalCount(),
                        dao.getLemmasTotalCount(), startFlag), detailed));
    }

    @Override
    public SearchEngineResponse startSearch(String query, String siteUrl, int offset, int limit) {
        if (query == null || query.isBlank()) throw new EmptySearchQueryException();
        checkSiteUrlAndSiteStatus(siteUrl);
        int upperPagesCountBound = appProps.getResultPageMaxCount();
        List<String> lemmas = getLemmas(query, siteUrl, upperPagesCountBound);
        List<Page> pages = getSearchResultPages(lemmas, siteUrl);
        if (pages.isEmpty()) return new SearchResultResponse(0, new SearchResultResponse.SearchResult[0]);
        SearchResultResponse.SearchResult[] data = getDataArray(getPagesAndRelevanceMap(pages, lemmas), lemmas);
        return new SearchResultResponse(data.length, data);
    }

    private SearchEngineResponse startIndex(List<AppProps.SiteData> siteDataList, boolean singlePageFlag) {
        if (startFlag) throw new RunApplicationException("???????????????????? ?????? ????????????????!");
        startFlag = true;
        initExecutors();
        forkJoinPool.execute(() ->
                indexing(getTaskList(siteDataList, singlePageFlag), singlePageFlag));
        return new SearchEngineResponse();
    }

    private void initExecutors() {
        scheduledThreadPoolExecutor = Executors.newScheduledThreadPool(PARALLELISM_LEVEL);
        threadPoolExecutor = Executors.newFixedThreadPool(PARALLELISM_LEVEL);
        forkJoinPool = new ForkJoinPool();
    }

    private void indexing(List<RecursiveLinkHandleTask> taskList, boolean singlePageFlag) {
        log.info("Start indexing: " + LocalTime.now());
        CountDownLatch countDownLatch = new CountDownLatch(taskList.size());
        ForkJoinTask.invokeAll(taskList)
                .forEach(task -> {
                    Site site = dao.findSiteById(task.getSiteId());
                    List<LinkHandleTaskResult> taskResults = task.join();
                    if (checkFailedIndexingSite(site, taskResults, countDownLatch)) return;
                    if (singlePageFlag) indexingPage(taskResults.get(0), site, countDownLatch);
                    else threadPoolExecutor.execute(() -> {
                        dao.savePages(getSitePages(taskResults, site));
                        dao.saveLemmas(getSiteLemmas(taskResults, site));
                        dao.saveIndexes(getSiteIndexes(taskResults, site));
                        dao.setSitesStatusIndexed(site);
                        cancelScheduledTask(site, countDownLatch);
                    });
                });
        try {
            countDownLatch.await();
        } catch (InterruptedException ignore) {
        }
        log.info("Indexed at time: " + LocalTime.now());
        scheduledThreadPoolExecutor.shutdownNow();
        startFlag = false;
    }

    private boolean checkFailedIndexingSite(Site site, List<LinkHandleTaskResult> taskResults,
                                            CountDownLatch countDownLatch) {
        StringBuilder errorMessageBuilder = new StringBuilder();
        for (LinkHandleTaskResult taskResult : taskResults) {
            Exception exception;
            if ((exception = taskResult.getException()) == null) return false;
            errorMessageBuilder.append(exception.getMessage()).append("; ");
        }
        dao.setSiteStatusFailed(site.getId(), errorMessageBuilder.toString().trim());
        countDownLatch.countDown();
        return true;
    }

    private void indexingPage(LinkHandleTaskResult taskResult, Site site, CountDownLatch countDownLatch) {
        dao.savePages(getSitePages(Arrays.asList(taskResult), site));
        Page page = dao.findPageBySiteIdAndPath(site, taskResult.getPath());
        Map<Lemma, Float> pageLemmasAndRanksMap = getPageLemmasAndRanksMap(taskResult, site);
        dao.saveLemmas(pageLemmasAndRanksMap.keySet());
        dao.saveIndexes(getPageIndexes(pageLemmasAndRanksMap, page));
        dao.setSitesStatusIndexed(site);
        cancelScheduledTask(site, countDownLatch);
    }

    private List<RecursiveLinkHandleTask> getTaskList(List<AppProps.SiteData> siteDataList, boolean singlePageFlag) {
        return siteDataList
                .stream()
                .map(siteData -> getSingleTask(siteData, singlePageFlag).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private Optional<RecursiveLinkHandleTask> getSingleTask(AppProps.SiteData siteData, boolean singlePageFlag) {
        if (forkJoinPool.isShutdown()) return Optional.empty();
        String url = siteData.getUrl();
        String name = siteData.getName();
        Site site = dao.updateSiteData(url, name, singlePageFlag);
        int siteId = site.getId();
        addScheduledTask(siteId);
        RecursiveLinkHandleTask task = new RecursiveLinkHandleTask(userAgentName, siteId, url,
                singlePageFlag ? baseUrlForSinglePage : url,
                morphologyService, selectorRepository);
        if (singlePageFlag) task.setSinglePageFlag(true);
        return Optional.of(task);
    }

    private void addScheduledTask(int siteId) {
        if (scheduledThreadPoolExecutor.isShutdown()) return;
        scheduledFutureMap.put(siteId, scheduledThreadPoolExecutor.scheduleAtFixedRate
                (() -> dao.setSiteStatusTimeBySiteId(siteId), 0, 5, TimeUnit.SECONDS));
    }

    private void cancelScheduledTask(Site site, CountDownLatch countDownLatch) {
        scheduledFutureMap.get(site.getId()).cancel(true);
        countDownLatch.countDown();
    }

    private Map<Lemma, Float> getPageLemmasAndRanksMap(LinkHandleTaskResult taskResult, Site site) {
        Map<String, Float> lemmasAndRanksMap = taskResult.getLemmasAndRanksMap();
        List<Lemma> lemmas = dao.findLemmasBySiteIdAndLemmas(site.getId(), lemmasAndRanksMap.keySet());
        Map<Lemma, Float> pageLemmasAndRanksMap = new HashMap<>();
        lemmasAndRanksMap.forEach((lemma, rank) -> {
            Lemma pageLemma = lemmas.stream()
                    .filter(l -> l.getLemma().equals(lemma))
                    .findFirst().orElse(null);
            if (pageLemma != null) {
                int frequency = pageLemma.getFrequency();
                pageLemma.setFrequency(++frequency);
            } else pageLemma = new Lemma(lemma, 1, site);
            pageLemmasAndRanksMap.put(pageLemma, rank);
        });
        return pageLemmasAndRanksMap;
    }

    private List<SearchIndex> getPageIndexes(Map<Lemma, Float> pageLemmasAndRanksMap, Page page) {
        return pageLemmasAndRanksMap.entrySet()
                .stream()
                .map(entry -> new SearchIndex(page, entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<Page> getSitePages(List<LinkHandleTaskResult> taskResultList, Site site) {
        return taskResultList.stream()
                .map(taskResult -> new Page(taskResult.getPath(),
                        taskResult.getStatusCode(),
                        taskResult.getContent(), site))
                .toList();
    }

    private Set<Lemma> getSiteLemmas(List<LinkHandleTaskResult> taskResultList, Site site) {
        Map<String, Integer> lemmasAndFrequencies = new HashMap<>();
        taskResultList.forEach(taskResult ->
                taskResult.getLemmasAndRanksMap()
                        .keySet()
                        .forEach(lemma ->
                                lemmasAndFrequencies.merge(lemma, 1, (oldV, newV) -> ++oldV)));
        Set<Lemma> lemmas = new HashSet<>();
        lemmasAndFrequencies.forEach((lemma, frequency) ->
                lemmas.add(new Lemma(lemma, frequency, site)));
        return lemmas;
    }

    private List<SearchIndex> getSiteIndexes(List<LinkHandleTaskResult> taskResultList, Site site) {
        int siteId = site.getId();
        List<Lemma> siteLemmas = dao.findLemmasBySiteId(siteId);
        List<Page> sitePages = dao.findPagesBySiteId(siteId);
        List<SearchIndex> indexes = new ArrayList<>();
        taskResultList.forEach(taskResult -> {
            if (taskResult.getContent().isEmpty()) return;
            Page page = sitePages.stream()
                    .filter(p -> p.getPath().equals(taskResult.getPath()))
                    .findFirst()
                    .get();
            taskResult.getLemmasAndRanksMap().forEach((lemma, rank) -> {
                Lemma pageLemma = siteLemmas.stream()
                        .filter(l -> l.getLemma().equals(lemma))
                        .findFirst()
                        .get();
                indexes.add(new SearchIndex(page, pageLemma, rank));
            });
        });
        return indexes;
    }

    private void checkSiteUrlAndSiteStatus(String siteUrl) {
        List<Site> sites;
        if (siteUrl != null) {
            if (siteDataList.stream().noneMatch(siteData -> siteData.getUrl().equals(siteUrl)))
                throw new DisableSiteSearchException();
            Site site;
            if (!((site = dao.findSiteByUrl(siteUrl)) != null && site.getStatus().equals(Status.INDEXED)))
                throw new RunApplicationException("???????? ?????? ???? ??????????????????????????????!");
        } else if (!((sites = dao.findAllSites()) != null
                && sites.stream().anyMatch(s -> s.getStatus().equals(Status.INDEXED))))
            throw new RunApplicationException("?????????? ?????? ???? ????????????????????????????????!");
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
            Set<String> wordForms = getLemmasWordForms(lemma);
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

    private Set<String> getLemmasWordForms(String lemma) {
        try {
            return WordformMeaning.lookupForMeanings(lemma).stream()
                    .flatMap(wordForm -> wordForm.getTransformations().stream())
                    .map(WordformMeaning::toString)
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            return Set.of();
        }
    }
}



