package org.example.util;

import org.example.model.CityLocation;
// Parses city and country from GraphML names
public final class CityLocationParser {
    private CityLocationParser() {}
    public static CityLocation parse(String graphName) {
        if (graphName == null || graphName.isBlank()) {
            return new CityLocation("Unknown", null, null);
        }
        String normalised = graphName.replace('_', ' ').trim();
        int comma = normalised.lastIndexOf(',');
        if (comma < 0) {
            return new CityLocation(normalised, null, CountryCodeResolver.resolve(normalised));
        }
        String city = normalised.substring(0, comma).trim();
        String country = normalised.substring(comma + 1).trim();
        String code = CountryCodeResolver.resolve(country);
        return new CityLocation(city, country, code);
    }
    public static CityLocation parseFromFile(String graphFile) {
        if (graphFile == null) return parse(null);
        String name = new java.io.File(graphFile).getName();
        if (name.endsWith(".graphml")) {
            name = name.substring(0, name.length() - 8);
        }
        return parse(name);
    }
}