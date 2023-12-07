package ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.vladimirsazonov.SiteSearchEngine.model.*;
import ru.vladimirsazonov.SiteSearchEngine.repositories.LemmaRepository;
import ru.vladimirsazonov.SiteSearchEngine.repositories.PageRepository;
import ru.vladimirsazonov.SiteSearchEngine.repositories.SearchIndexRepository;
import ru.vladimirsazonov.SiteSearchEngine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@Data
public class DAO {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final SearchIndexRepository searchIndexRepository;
    private String baseUrlForSinglePage;

    public Site findSiteByName(String name) {
        return siteRepository.findByName(name).orElse(null);
    }

    public Site findSiteByUrl(String url) {
        return siteRepository.findByUrl(url).orElse(null);
    }

    public Site findSiteById(int id) {
        return siteRepository.findById(id).orElse(null);
    }

    public long getPagesTotalCount() {
        return pageRepository.count();
    }

    public int getPagesCountBySiteId(int id) {
        return pageRepository.getCountBySiteId(id);
    }

    public long getLemmasTotalCount() {
        return lemmaRepository.count();
    }

    public int getLemmasCountBySiteId(int id) {
        return lemmaRepository.getCountBySiteId(id);
    }

    @Transactional
    public void setSiteStatusTimeBySiteId(int id) {
        siteRepository.setStatusTimeBySiteId(id);
    }

    @Transactional
    public void setAllSitesStatusFailed() {
        siteRepository.setAllSitesStatusFailed();
    }

    @Transactional
    public void setSiteErrorMessage(int id, String errorMessage) {
        siteRepository.setErrorMessageBySiteId(errorMessage, id);
    }

    @Transactional
    public void setSiteStatus(Status status, int id) {
        siteRepository.setStatusBySiteId(status, id);
    }

    public Page findPageBySiteIdAndPath(Site site, String path) {
        return pageRepository.findBySiteIdAndPath(site.getId(), path).orElse(null);
    }

    public List<Lemma> findLemmasBySiteIdAndLemmas(int id, Set<String> lemmas) {
        return lemmaRepository.findBySiteIdAndLemmas(id, lemmas).orElse(List.of());
    }

    public List<Lemma> findLemmasBySiteId(int id) {
        return lemmaRepository.findBySiteId(id).orElse(List.of());
    }

    public void saveLemma(Lemma lemma) {
        lemmaRepository.save(lemma);
    }

    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    public List<Lemma> findLemmasByPageId(int id) {
        return lemmaRepository.findByPageId(id).orElse(List.of());
    }

    @Transactional
    public void deleteIndexByPageId(int id) {
        searchIndexRepository.deleteByPageId(id);
    }

    @Transactional
    public void deleteLemma(Lemma lemma) {
        lemmaRepository.delete(lemma);
    }

    @Transactional
    public void deleteIndexesBySiteId(int id) {
        searchIndexRepository.deleteBySiteId(id);
    }

    @Transactional
    public void deleteLemmasBySiteId(int id) {
        lemmaRepository.deleteBySiteId(id);
    }

    @Transactional
    public void deletePagesBySiteId(int id) {
        pageRepository.deleteBySiteId(id);
    }

    @Transactional
    public void savePages(List<Page> pages) {
        pageRepository.saveAll(pages);
    }

    @Transactional
    public void saveLemmas(Set<Lemma> lemmas) {
        lemmaRepository.saveAll(lemmas);
    }

    @Transactional
    public void saveIndexes(List<SearchIndex> indexes) {
        searchIndexRepository.saveAll(indexes);
    }

    @Transactional
    public List<Site> saveSites(List<Site> sites) {
        return siteRepository.saveAll(sites);
    }

    public List<Page> findPagesBySiteId(int siteId) {
        return pageRepository.findBySiteId(siteId).orElse(List.of());
    }

    @Transactional
    public Site updateSiteData(String url, String name, boolean isSinglePage) {
        Site site = findSiteByName(name);
        if (site == null)
            site = saveSite(new Site(Status.INDEXING, LocalDateTime.now(), url, name));
        else if (isSinglePage) deletePageAndIndexes(site, url);
        else {
            int siteId = site.getId();
            deleteIndexesBySiteId(siteId);
            deletePagesBySiteId(siteId);
            deleteLemmasBySiteId(siteId);
        }
        setSiteStatusIndexing(site);
        return site;
    }

    @Transactional
    private void setSiteStatusIndexing(Site site) {
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(null);
        saveSite(site);
    }

    @Transactional
    public void deletePageAndIndexes(Site site, String url) {
        String path = url.substring(site.getUrl().length());
        if (path.isBlank()) path = "/";
        Page page = findPageBySiteIdAndPath(site, path);
        if (page == null) return;
        int pageId = page.getId();
        List<Lemma> lemmas = findLemmasByPageId(pageId);
        deleteIndexByPageId(pageId);
        deletePageById(pageId);
        lemmas.forEach(lemma -> {
            int frequency = lemma.getFrequency();
            if (--frequency == 0) deleteLemma(lemma);
            else {
                lemma.setFrequency(frequency);
                saveLemma(lemma);
            }
        });
    }

    @Transactional
    private void deletePageById(int id) {
        pageRepository.deleteById(id);
    }

    public Integer getTotalFrequencyForLemma(String lemma) {
        return lemmaRepository.findSumFrequencyByLemma(lemma).orElse(0);
    }

    public Integer getFrequencyForLemmaBySite(String lemma, String site) {
        return lemmaRepository.findSumFrequencyByLemmaAndSite(lemma, site).orElse(0);
    }

    public List<Page> findPagesByLemma(String lemma) {
        return pageRepository.findByLemma(lemma).orElse(List.of());
    }

    public Float getGradeSumByPageIdAndLemmas(int pageId, List<String> lemmas) {
        return searchIndexRepository.getGradeSumByPageIdAndLemmas(pageId, lemmas).orElse(0f);
    }

    public List<Site> findAllSites() {
        return siteRepository.findAll();
    }

    public List<Page> findPagesByLemmaAndSiteUrl(String lemma, String site) {
        return pageRepository.findByLemmaAndSiteUrl(lemma, site).orElse(List.of());
    }
}
