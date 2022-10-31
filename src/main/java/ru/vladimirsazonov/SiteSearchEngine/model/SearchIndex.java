package ru.vladimirsazonov.SiteSearchEngine.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class SearchIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SeqGenIndex")
    @SequenceGenerator(name = "SeqGenIndex", sequenceName = "seq_index")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    private final Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id")
    private final Lemma lemma;

    @Column(nullable = false)
    private final float grade;
}
