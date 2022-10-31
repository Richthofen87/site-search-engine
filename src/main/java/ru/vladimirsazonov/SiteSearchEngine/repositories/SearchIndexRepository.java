package ru.vladimirsazonov.SiteSearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.vladimirsazonov.SiteSearchEngine.model.SearchIndex;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {

    @Query("SELECT SUM(s.grade) FROM SearchIndex s WHERE s.page.id = ?1 and s.lemma.lemma in ?2")
    Optional<Float> getGradeSumByPageIdAndLemmas(int pageId, List<String> lemmas);

    @Modifying
    @Query("DELETE SearchIndex WHERE page_id in (SELECT id FROM Page WHERE site_id = ?1)")
    void deleteBySiteId(int id);

    @Modifying
    @Query("DELETE SearchIndex WHERE page_id = ?1")
    void deleteByPageId(int pageId);
}
