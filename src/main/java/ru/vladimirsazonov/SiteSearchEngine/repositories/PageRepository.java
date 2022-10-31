package ru.vladimirsazonov.SiteSearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.vladimirsazonov.SiteSearchEngine.model.Page;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query("FROM Page WHERE site_id = ?1 and path = ?2")
    Optional<Page> findBySiteIdAndPath(int siteId, String path);

    @Query("FROM Page WHERE site_id = ?1")
    Optional<List<Page>> findBySiteId(int siteId);

    @Query("SELECT s.page FROM SearchIndex s WHERE s.lemma.lemma = ?1")
    Optional<List<Page>> findByLemma(String lemma);

    @Query("SELECT count(*) FROM Page WHERE site_id = ?1")
    int getCountBySiteId(int siteId);

    @Modifying
    @Query("DELETE Page WHERE site_id = ?1")
    void deleteBySiteId(int siteId);

    @Query("SELECT s.page FROM SearchIndex s WHERE s.lemma.lemma = ?1 and s.lemma.site.url = ?2")
    Optional<List<Page>> findByLemmaAndSiteUrl(String lemma, String site);
}
