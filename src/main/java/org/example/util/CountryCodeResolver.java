package org.example.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Maps country names from GraphML filenames to World Bank ISO 3166-1 alpha-3 reference area codes.
public final class CountryCodeResolver {
    private static final Map<String, String> CODES = new HashMap<>();
    static {
        register("Argentina", "ARG");
        register("Australia", "AUS");
        register("Brazil", "BRA");
        register("Canada", "CAN");
        register("Chile", "CHL");
        register("China", "CHN");
        register("Colombia", "COL");
        register("Denmark", "DNK");
        register("Ecuador", "ECU");
        register("Egypt", "EGY");
        register("Finland", "FIN");
        register("France", "FRA");
        register("Germany", "DEU");
        register("Hong Kong", "HKG");
        register("India", "IND");
        register("Indonesia", "IDN");
        register("Ireland", "IRL");
        register("Italy", "ITA");
        register("Japan", "JPN");
        register("Kenya", "KEN");
        register("Malaysia", "MYS");
        register("Mexico", "MEX");
        register("Morocco", "MAR");
        register("Netherlands", "NLD");
        register("Nigeria", "NGA");
        register("Norway", "NOR");
        register("Peru", "PER");
        register("Philippines", "PHL");
        register("Poland", "POL");
        register("Qatar", "QAT");
        register("Russia", "RUS");
        register("Saudi Arabia", "SAU");
        register("Singapore", "SGP");
        register("South Africa", "ZAF");
        register("South Korea", "KOR");
        register("Spain", "ESP");
        register("Sweden", "SWE");
        register("Switzerland", "CHE");
        register("Thailand", "THA");
        register("Turkey", "TUR");
        register("United Arab Emirates", "ARE");
        register("United Kingdom", "GBR");
        register("United States", "USA");
        register("UAE", "ARE");
        register("UK", "GBR");
        register("USA", "USA");
        register("US", "USA");
        register("Korea", "KOR");
        register("Russia", "RUS");
        register("Russian Federation", "RUS");
    }
    private CountryCodeResolver() {}
    private static void register(String country, String code) {
        CODES.put(normaliseKey(country), code);
    }
    public static String resolve(String countryName) {
        if (countryName == null || countryName.isBlank()) return null;
        String key = normaliseKey(countryName);
        String direct = CODES.get(key);
        if (direct != null) return direct;
        for (Map.Entry<String, String> e : CODES.entrySet()) {
            if (key.contains(e.getKey()) || e.getKey().contains(key)) {
                return e.getValue();
            }
        }
        return null;
    }
    private static String normaliseKey(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
