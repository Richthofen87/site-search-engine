package ru.vladimirsazonov.SiteSearchEngine.model;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Collection;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor(force = true)
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SeqGenLemma")
    @SequenceGenerator(name = "SeqGenLemma", sequenceName = "seq_lemma")
    private int id;

    @Column(nullable = false)
    private final String lemma;

    @Column(nullable = false)
    private int frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private final Site site;

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY)
    private Collection<SearchIndex> searchIndexCollection;

    public Lemma(String lemma, int frequency, Site site) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.site = site;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return getId() == lemma1.getId() && getFrequency() == lemma1.getFrequency()
                && getLemma().equals(lemma1.getLemma());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getLemma(), getFrequency());
    }
}
