package ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.vladimirsazonov.SiteSearchEngine.config.SitesList;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.ServerStateException;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.RunApplicationException;
import ru.vladimirsazonov.SiteSearchEngine.model.*;
import ru.vladimirsazonov.SiteSearchEngine.repositories.SelectorRepository;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.IndexingService;
import ru.vladimirsazonov.SiteSearchEngine.services.morphology.MorphologyService;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final int PARALLELISM_LEVEL = Runtime.getRuntime().availableProcessors();
    private final Map<Integer, ScheduledFuture<?>> scheduledFutureMap = new ConcurrentHashMap<>();
    private final SitesList sitesList;
    private final SelectorRepository selectorRepository;
    private final MorphologyService morphologyService;
    private final DAO dao;
    private ScheduledExecutorService scheduledThreadPoolExecutor;
    private ForkJoinPool forkJoinPool;
    private ExecutorService threadPoolExecutor;
    private boolean indexingRunningFlag;
    private String urlForSinglePage;
    private boolean isSinglePageMode;

    @Value("${user-agent-name}")
    private String userAgentName;

    @Value("${referer}")
    private String referer;

    @Override
    public boolean isIndexingRunning() {
        return indexingRunningFlag;
    }

    @Override
    public SearchEngineResponse startIndexing() {
        List<Site> sites;
        if ((sites = sitesList.getSites()).isEmpty())
            throw new RunApplicationException("В конфигурационом файле не указаны сайты");
        if (indexingRunningFlag) throw new ServerStateException("Индексация уже запущена");
        if (isSinglePageMode) {
            Optional<Site> siteOptional = sites
                    .stream()
                    .filter(site -> urlForSinglePage.startsWith(site.getUrl()))
                    .findAny();
            if (siteOptional.isEmpty())
                throw new RunApplicationException("Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле");

            Site site = siteOptional.get();
            String path = urlForSinglePage.substring(site.getUrl().length());
            if (path.isEmpty()) path = "/";
            if (dao.findPageBySiteIdAndPath(site, path) == null)
                throw new RunApplicationException("Указанная страница не найдена");
            sites = List.of(site);
        }
        indexingRunningFlag = true;
        scheduledThreadPoolExecutor = Executors.newScheduledThreadPool(PARALLELISM_LEVEL);
        threadPoolExecutor = Executors.newFixedThreadPool(PARALLELISM_LEVEL);
        forkJoinPool = new ForkJoinPool();
        List<Site> sitesToIndexing = sites;
        threadPoolExecutor.execute(() -> indexing(getTaskList(sitesToIndexing)));
        return new SearchEngineResponse();
    }

    @Override
    public SearchEngineResponse stopIndexing() {
        if (!indexingRunningFlag) throw new ServerStateException("Индексация ещё не запущена");
        scheduledThreadPoolExecutor.shutdownNow();
        threadPoolExecutor.shutdownNow();
        forkJoinPool.shutdownNow();
        dao.setAllSitesStatusFailed();
        return new SearchEngineResponse();
    }

    @Override
    public SearchEngineResponse indexPage(String pageUrl) {
        if (pageUrl.isBlank()) throw new RunApplicationException("В запросе передана пустая страница");
        isSinglePageMode = true;
        urlForSinglePage = pageUrl;
        return startIndexing();
    }

    private void indexing(List<RecursiveLinkHandleTask> taskList) {
        log.info("Start indexing...");
        CountDownLatch countDownLatch = new CountDownLatch(taskList.size());
        LocalTime start = LocalTime.now();
        ForkJoinTask.invokeAll(taskList)
                .forEach(task ->
                        threadPoolExecutor.execute(() -> {
                            Site site = dao.findSiteById(task.getSiteId());
                            List<LinkHandleTaskResult> taskResults = task.join();
                            if (checkFailedSiteAndSetErrorsDetails(site, taskResults)) {
                                dao.setSiteStatus(Status.FAILED, site.getId());
                                countDownLatch.countDown();
                                return;
                            }
                            dao.savePages(getSitePages(taskResults, site));
                            if (isSinglePageMode) {
                                indexingPage(taskResults.get(0), site);
                                isSinglePageMode = false;
                            } else {
                                dao.saveLemmas(getSiteLemmas(taskResults, site));
                                dao.saveIndexes(getSiteIndexes(taskResults, site));
                            }
                            dao.setSiteStatus(Status.INDEXED, site.getId());
                            cancelScheduledTask(site);
                            countDownLatch.countDown();
                        }));
        try {
            countDownLatch.await();
            log.info("Indexed at time: " + Duration.between(start, LocalTime.now()).toMillis() + " ms");
            scheduledThreadPoolExecutor.shutdownNow();
            indexingRunningFlag = false;
        } catch (InterruptedException e) {
            log.warn("Main thread '%s' is interrupted while indexing with cause: %s -> %s"
                    .formatted(Thread.currentThread().getName(), e.getCause().getClass().getName(),
                            e.getCause().getMessage()));
        }
    }

    private boolean checkFailedSiteAndSetErrorsDetails(Site site, List<LinkHandleTaskResult> taskResults) {
        int countFailes = 0;
        Exception lastEx;
        String errorMesage = "";
        for (LinkHandleTaskResult taskResult : taskResults) {
            if ((lastEx = taskResult.getException()) != null)
                errorMesage = "Total errors(s): %d. Last error: obtain path '%s' failed, exception type -> '%s', exception message -> %s; "
                        .formatted(++countFailes, taskResult.getPath().equals("/") ? site.getUrl() : taskResult.getPath(),
                                lastEx.getClass().getName(), lastEx.getMessage());
        }
        boolean result = countFailes == taskResults.size();
        dao.setSiteErrorMessage(site.getId(), result ? errorMesage : "null");
        return result;
    }

    private void indexingPage(LinkHandleTaskResult taskResult, Site site) {
        Page page = dao.findPageBySiteIdAndPath(site, taskResult.getPath());
        Map<Lemma, Float> pageLemmasAndRanksMap = getPageLemmasAndRanksMap(taskResult, site);
        dao.saveLemmas(pageLemmasAndRanksMap.keySet());
        dao.saveIndexes(getPageIndexes(pageLemmasAndRanksMap, page));
    }

    private List<RecursiveLinkHandleTask> getTaskList(List<Site> sites) {
        return sites
                .stream()
                .map(siteData -> getSingleTask(siteData).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private Optional<RecursiveLinkHandleTask> getSingleTask(Site site) {
        if (forkJoinPool.isShutdown()) return Optional.empty();
        String url = site.getUrl();
        site = dao.updateSiteData(url, site.getName(), isSinglePageMode);
        int siteId = site.getId();
        addScheduledTask(siteId);
        RecursiveLinkHandleTask task = new RecursiveLinkHandleTask(siteId, isSinglePageMode ? urlForSinglePage : url,
                url, morphologyService, selectorRepository, userAgentName, referer);
        if (isSinglePageMode) task.setSinglePageMode(true);
        return Optional.of(task);
    }

    private void addScheduledTask(int siteId) {
        if (scheduledThreadPoolExecutor.isShutdown()) return;
        scheduledFutureMap.put(siteId, scheduledThreadPoolExecutor.scheduleAtFixedRate
                (() -> dao.setSiteStatusTimeBySiteId(siteId), 0, 5, TimeUnit.SECONDS));
    }

    private void cancelScheduledTask(Site site) {
        scheduledFutureMap.get(site.getId()).cancel(true);
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
                .filter(taskResult -> taskResult.getStatusCode() == 200)
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
}
