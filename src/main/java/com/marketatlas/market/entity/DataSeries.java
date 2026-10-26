package com.marketatlas.market.entity;

import com.marketatlas.enums.CopyrightClass;
import com.marketatlas.enums.SeriesFrequency;
import com.marketatlas.enums.SeriesProvider;
import jakarta.persistence.*;

@Entity
@Table(name = "data_series")
public class DataSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeriesProvider provider;

    @Column(nullable = false, length = 100)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeriesFrequency frequency;

    @Column(nullable = false, length = 100)
    private String units;

    @Column(nullable = false, length = 100)
    private String sourceOrganization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CopyrightClass copyrightClass;

    @Column(nullable = false, length = 500)
    private String attribution;

    protected DataSeries() {
    }

    public DataSeries(SeriesProvider provider, String externalId, String name,
                      SeriesFrequency frequency, String units, String sourceOrganization,
                      CopyrightClass copyrightClass, String attribution) {
        this.provider = provider;
        this.externalId = externalId;
        this.name = name;
        this.frequency = frequency;
        this.units = units;
        this.sourceOrganization = sourceOrganization;
        this.copyrightClass = copyrightClass;
        this.attribution = attribution;
    }

    public Long getId() { return id; }
    public SeriesProvider getProvider() { return provider; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public SeriesFrequency getFrequency() { return frequency; }
    public String getUnits() { return units; }
    public String getSourceOrganization() { return sourceOrganization; }
    public CopyrightClass getCopyrightClass() { return copyrightClass; }
    public String getAttribution() { return attribution; }
}