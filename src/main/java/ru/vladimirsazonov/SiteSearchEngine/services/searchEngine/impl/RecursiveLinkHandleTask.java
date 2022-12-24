package ru.vladimirsazonov.SiteSearchEngine.services.searchEngine.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.vladimirsazonov.SiteSearchEngine.repositories.SelectorRepository;
import ru.vladimirsazonov.SiteSearchEngine.services.morphology.MorphologyService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Data
@Slf4j
public class RecursiveLinkHandleTask extends RecursiveTask<List<LinkHandleTaskResult>> {

    private final String userAgentName;
    private final int siteId;
    private final String siteUrl;
    private final String baseUrl;
    private final MorphologyService morphologyService;
    private final SelectorRepository selectorRepository;
    private int statusCode;
    private Document document;
    private IOException ex;
    private boolean singlePageFlag;

    private String getPath() {
        String path = siteUrl.substring(baseUrl.length());
        if (path.isEmpty()) path = "/";
        return path;
    }

    private void getConnection() {
        Connection connection;
        try {
            Thread.sleep(new Random().nextInt(100) + 100);
            connection = Jsoup.connect(siteUrl).timeout(3000)
                    .userAgent(userAgentName);
            statusCode = Objects.requireNonNull(connection).execute().statusCode();
            document = Objects.requireNonNull(connection).get();
        } catch (NullPointerException | IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            else if (e instanceof HttpStatusException) {
                ex = (HttpStatusException) e;
                statusCode = ((HttpStatusException) e).getStatusCode();
            } else if (!(e instanceof NullPointerException)) ex = (IOException) e;
            log.warn(e.getMessage());
        }
    }

    @Override
    public List<LinkHandleTaskResult> compute() {
        getConnection();
        if (statusCode == 0) return List.of();
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
