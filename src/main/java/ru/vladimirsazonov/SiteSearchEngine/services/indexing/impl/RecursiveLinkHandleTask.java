package ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import ru.vladimirsazonov.SiteSearchEngine.model.Selector;
import ru.vladimirsazonov.SiteSearchEngine.repositories.SelectorRepository;
import ru.vladimirsazonov.SiteSearchEngine.services.morphology.MorphologyService;

import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Data
@Slf4j
public class RecursiveLinkHandleTask extends RecursiveTask<List<LinkHandleTaskResult>> {
    private static List<Selector> selectors;

    private final int siteId;
    private final String siteUrl;
    private final String parentUrl;
    private final MorphologyService morphologyService;
    private final SelectorRepository selectorRepository;
    private final String userAgentName;
    private final String referer;
    private int statusCode;
    private Document document;
    private Exception ex;
    private boolean isSinglePageMode;

    private void initSelectors() {
        selectors = selectorRepository.findAll();
    }

    private String getPath() {
        String path = siteUrl.substring(parentUrl.length());
        if (path.isEmpty()) path = "/";
        return path;
    }

    private void getConnection() {
        Connection connection;
        try {
            Thread.sleep(new Random().nextInt(100) + 50);
            connection = Jsoup.connect(siteUrl).timeout(1500)
                    .userAgent(userAgentName).referrer(referer);
            Connection.Response response = connection.execute();
            statusCode = response.statusCode();
            document = response.parse();
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            else if (e instanceof HttpStatusException) {
                statusCode = ((HttpStatusException) e).getStatusCode();
            }
            ex = e;
            log.warn("failed to obtain %s: %s -> %s".formatted(siteUrl, e.getClass().getName(), e.getMessage()));
        }
    }

    @Override
    public List<LinkHandleTaskResult> compute() {
        getConnection();
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
        if (selectors == null) initSelectors();
        Map<String, Float> lemmasAndRanksMap = new HashMap<>();
        selectors.forEach(s -> {
            float weight = s.getWeight();
            morphologyService.getLemmasAndFrequenciesMap
                            (document.select(s.getSelector()).text())
                    .forEach((lemma, frequency) ->
                            lemmasAndRanksMap.merge(lemma, frequency * weight, Float::sum));
        });
        linkHandleTaskResult.setLemmasAndRanksMap(lemmasAndRanksMap);
        Set<String> childLinks;
        if (isSinglePageMode || (childLinks = getChildLinks()).isEmpty()) return taskResultList;
        List<LinkHandleTaskResult> subTasksResultList = getTaskResultList(childLinks);
        subTasksResultList.addAll(taskResultList);
        return subTasksResultList;
    }

    private List<LinkHandleTaskResult> getTaskResultList(Set<String> childLinks) {
        return invokeAll(childLinks.stream()
                .map(link -> new RecursiveLinkHandleTask(siteId, link, parentUrl, morphologyService, selectorRepository, userAgentName, referer))
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
                .filter(e -> e.matches(siteUrl.endsWith("/") ?
                        siteUrl + ".+" : siteUrl + "/.+")
                        && !e.contains("#")
                        && !e.contains("=")
                        && !e.contains("?")
                        && !e.contains(" ")
                        && !e.contains("extlink"))
                .collect(Collectors.toSet());
    }
}
