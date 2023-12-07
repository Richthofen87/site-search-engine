package ru.vladimirsazonov.SiteSearchEngine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;

@Entity
@Data
@NoArgsConstructor
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column
    private Status status;

    @Column
    private LocalDateTime statusTime;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    private Collection<Lemma> lemmas;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    private Collection<Page> pages;

    public Site(Status status, LocalDateTime statusTime, String url, String name) {
        this(name, url);
        this.status = status;
        this.statusTime = statusTime;
    }

    public Site(String name, String url) {
        this.name = name;
        this.url = url;
    }
}
