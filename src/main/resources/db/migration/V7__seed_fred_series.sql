-- Reference data: the FRED series MarketAtlas tracks. Licensing metadata is stored with each
-- series; classifications are taken from the FRED series pages and should be re-checked
-- there before any series is added or exposed more widely.
INSERT INTO data_series
(provider, external_id, name, frequency, units, source_organization, copyright_class, attribution)
VALUES
    ('FRED', 'CPIAUCSL',
     'Consumer Price Index for All Urban Consumers: All Items (seasonally adjusted)',
     'MONTHLY', 'Index 1982-1984=100', 'U.S. Bureau of Labor Statistics',
     'PUBLIC_DOMAIN_CITATION_REQUESTED',
     'Source: U.S. Bureau of Labor Statistics, retrieved from FRED, Federal Reserve Bank of St. Louis'),
    ('FRED', 'CPIAUCNS',
     'Consumer Price Index for All Urban Consumers: All Items (not seasonally adjusted)',
     'MONTHLY', 'Index 1982-1984=100', 'U.S. Bureau of Labor Statistics',
     'PUBLIC_DOMAIN_CITATION_REQUESTED',
     'Source: U.S. Bureau of Labor Statistics, retrieved from FRED, Federal Reserve Bank of St. Louis'),
    ('FRED', 'DCOILBRENTEU',
     'Crude Oil Prices: Brent - Europe',
     'DAILY', 'US dollars per barrel', 'U.S. Energy Information Administration',
     'PUBLIC_DOMAIN_CITATION_REQUESTED',
     'Source: U.S. Energy Information Administration, retrieved from FRED, Federal Reserve Bank of St. Louis'),
    ('FRED', 'DHHNGSP',
     'Henry Hub Natural Gas Spot Price',
     'DAILY', 'US dollars per million BTU', 'U.S. Energy Information Administration',
     'PUBLIC_DOMAIN_CITATION_REQUESTED',
     'Source: U.S. Energy Information Administration, retrieved from FRED, Federal Reserve Bank of St. Louis');