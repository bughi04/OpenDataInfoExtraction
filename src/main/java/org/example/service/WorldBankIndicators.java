package org.example.service;

// Curated World Development Indicators (WDI) fetched via the World Bank Data360 API.
public enum WorldBankIndicators {
    GDP_PER_CAPITA(
            "WB_WDI_NY_GDP_PCAP_CD",
            "GDP per capita",
            "GDP per capita (current US$)"),
    GNI_PER_CAPITA(
            "WB_WDI_NY_GNP_PCAP_CD",
            "GNI per capita",
            "GNI per capita (current US$)"),
    POPULATION(
            "WB_WDI_SP_POP_TOTL",
            "Population",
            "Population, total"),
    URBAN_POP_PCT(
            "WB_WDI_SP_URB_TOTL_IN_ZS",
            "Urban population",
            "Urban population (% of total population)"),
    POP_GROWTH(
            "WB_WDI_SP_POP_GROW",
            "Population growth",
            "Population growth (annual %)"),
    CO2_PER_CAPITA(
            "WB_WDI_EN_GHG_CO2_PC_CE_AR5",
            "CO2 emissions",
            "CO2 emissions excluding LULUCF (metric tons per capita)"),
    ENERGY_USE(
            "WB_WDI_EG_USE_PCAP_KG_OE",
            "Energy use",
            "Energy use (kg of oil equivalent per capita)"),
    INTERNET_USERS(
            "WB_WDI_IT_NET_USER_ZS",
            "Internet users",
            "Individuals using the Internet (% of population)"),
    LOGISTICS_INDEX(
            "WB_WDI_LP_LPI_OVRL_XQ",
            "Logistics performance",
            "Logistics Performance Index overall (1=low to 5=high)"),
    RAIL_LINES(
            "WB_WDI_IS_RRS_TOTL_KM",
            "Rail lines",
            "Rail lines (total route-km)"),
    TRANSPORT_INFRA(
            "WB_WDI_LP_LPI_INFR_XQ",
            "Transport infrastructure",
            "LPI: Quality of trade and transport infrastructure (1-5)"),
    ELECTRIC_POWER(
            "WB_WDI_EG_ELC_ACCS_ZS",
            "Electricity access",
            "Access to electricity (% of population)");
    private final String id;
    private final String shortLabel;
    private final String description;
    WorldBankIndicators(String id, String shortLabel, String description) {
        this.id = id;
        this.shortLabel = shortLabel;
        this.description = description;
    }
    public String getId() { return id; }
    public String getShortLabel() { return shortLabel; }
    public String getDescription() { return description; }
    public static WorldBankIndicators byId(String id) {
        if (id == null) return null;
        for (WorldBankIndicators ind : values()) {
            if (ind.id.equals(id)) return ind;
        }
        return null;
    }
}
