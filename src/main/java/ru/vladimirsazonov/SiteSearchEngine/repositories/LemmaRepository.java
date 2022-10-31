package ru.vladimirsazonov.SiteSearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.vladimirsazonov.SiteSearchEngine.model.Lemma;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query("SELECT lemma FROM SearchIndex WHERE page_id = ?1")
    Optional<List<Lemma>> findByPageId(int pageId);

    @Query("FROM Lemma WHERE site_id = ?1")
    Optional<List<Lemma>> findBySiteId(int id);

    @Query("FROM Lemma WHERE site_id = ?1 and lemma in ?2")
    Optional<List<Lemma>> findBySiteIdAndLemmas(int siteId, Collection<String> lemmas);

    @Query("SELECT count(*) FROM Lemma WHERE site_id = ?1")
    int getCountBySiteId(int siteId);

    @Modifying
    @Query("DELETE Lemma WHERE site_id = ?1")
    void deleteBySiteId(int siteId);

    @Query("SELECT sum(frequency) FROM Lemma where lemma = ?1")
    Optional<Integer> findSumFrequencyByLemma(String lemma);

    @Query("SELECT sum(frequency) FROM Lemma where lemma = ?1 and site.url = ?2")
    Optional<Integer> findSumFrequencyByLemmaAndSite(String lemma, String site);
}
