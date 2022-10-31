package ru.vladimirsazonov.SiteSearchEngine.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "field")
@Data
public class Selector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String selector;

    @Column(nullable = false)
    private float weight;
}
