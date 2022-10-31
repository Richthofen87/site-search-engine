package ru.vladimirsazonov.SiteSearchEngine.services.searchEngine.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.vladimirsazonov.SiteSearchEngine.repositories.SelectorRepository;
import ru.vladimirsazonov.SiteSearchEngine.services.morphology.MorphologyService;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public class RecursiveLinkHandleTask extends RecursiveTask<List<LinkHandleTaskResult>> {

    private final MorphologyService morphologyService;
    private final SelectorRepository selectorRepository;
    private final String userAgentName;
    private final String siteUrl;
    private final String baseUrl;
    private final int siteId;
    private int statusCode;
    private Document document;
    private IOException ex;
    private boolean singlePageFlag;

    public RecursiveLinkHandleTask(String userAgentName, int siteId, String siteUrl, String baseUrl,
                                   MorphologyService morphologyService, SelectorRepository repository) {
        this.userAgentName = userAgentName;
        this.morphologyService = morphologyService;
        this.selectorRepository = repository;
        this.siteId = siteId;
        this.siteUrl = siteUrl;
        this.baseUrl = baseUrl;
        Connection connection = getConnection(this.siteUrl, userAgentName);
        try {
            statusCode = Objects.requireNonNull(connection).execute().statusCode();
            document = Objects.requireNonNull(connection).get();
        } catch (NullPointerException | IOException e) {
            if (e instanceof HttpStatusException) {
                ex = (HttpStatusException) e;
                statusCode = ((HttpStatusException) e).getStatusCode();
            }
            else if (e instanceof SSLHandshakeException) ex = (SSLHandshakeException) e;
            log.warn(e.getMessage());
        }
    }

    private String getPath() {
        String path = siteUrl.substring(baseUrl.length());
        if (path.isEmpty()) path = "/";
        return path;
    }

    private Connection getConnection(String siteUrl, String userAgentName) {
        try {
            Thread.sleep(new Random().nextInt(100) + 100);
            return Jsoup.connect(siteUrl)
                    .userAgent(userAgentName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public List<LinkHandleTaskResult> compute() {
        String pagePath = getPath();
        LinkHandleTaskResult linkHandleTaskResult = new LinkHandleTaskResult(pagePath, statusCode, siteId,
                document != null ? document.html() : "");
        List<LinkHandleTaskResult> taskResultList = new ArrayList<>() {{
            add(linkHandleTaskResult);
        }};
        if (ex != null) linkHandleTaskResult.setException(ex);
        if (document == null) {
            linkHandleTaskResult.setLemmasAndRanksMap(Map.of());
            return taskResultList;
        }
        Map<String, Float> lemmasAndRanksMap = new HashMap<>();
        selectorRepository.findAll()
                .forEach(s -> {
                    float weight = s.getWeight();
                    morphologyService.getLemmasAndFrequenciesMap
                                    (document.select(s.getSelector()).text())
                            .forEach((lemma, frequency) ->
                                    lemmasAndRanksMap.merge(lemma, frequency * weight, Float::sum));
                });
        linkHandleTaskResult.setLemmasAndRanksMap(lemmasAndRanksMap);
        Set<String> childLinks;
        if (singlePageFlag || (childLinks = getChildLinks()).isEmpty()) return taskResultList;
        List<LinkHandleTaskResult> subTasksResultList = getTaskResultList(childLinks);
        subTasksResultList.addAll(taskResultList);
        return subTasksResultList;
    }

    private List<LinkHandleTaskResult> getTaskResultList(Set<String> childLinks) {
        return invokeAll(childLinks.stream()
                .map(link -> new RecursiveLinkHandleTask(userAgentName,
                        siteId, link, baseUrl, morphologyService, selectorRepository))
                .toList())
                .stream()
                .flatMap(task -> task.join().stream())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Set<String> getChildLinks() {
        Elements strings = document.select("a[href]");
        return strings.stream()
                .map(e -> {
                    String link = e.absUrl("href");
                    int index = link.indexOf("/", siteUrl.length() + 1);
                    if (index != -1) link = link.substring(0, index);
                    return link;
                })
                .filter(e -> e.matches(siteUrl + "/.+")
                        && !e.contains("#")
                        && !e.contains("=")
                        && !e.contains("?")
                        && !e.contains(" ")
                        && !e.contains("extlink"))
                .collect(Collectors.toSet());
    }
}
