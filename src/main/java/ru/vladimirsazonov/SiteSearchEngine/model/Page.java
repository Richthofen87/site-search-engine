package ru.vladimirsazonov.SiteSearchEngine.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SeqGenPage")
    @SequenceGenerator(name = "SeqGenPage", sequenceName = "seq_page")
    private int id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private final String path;

    @Column(nullable = false)
    private final int code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private final String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private final Site site;

    @OneToMany(mappedBy = "page", fetch = FetchType.LAZY)
    private Collection<SearchIndex> searchIndexCollection;
}
