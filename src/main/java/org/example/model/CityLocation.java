package org.example.model;

import java.io.Serializable;
import java.util.Objects;
// City and country parsed from a GraphML filename
public class CityLocation implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String city;
    private final String country;
    private final String refAreaCode;
    public CityLocation(String city, String country, String refAreaCode) {
        this.city = city;
        this.country = country;
        this.refAreaCode = refAreaCode;
    }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public String getRefAreaCode() { return refAreaCode; }
    public boolean hasCountryCode() {
        return refAreaCode != null && !refAreaCode.isBlank();
    }
    public String displayLabel() {
        if (country == null || country.isBlank()) return city;
        return city + ", " + country;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CityLocation that)) return false;
        return Objects.equals(city, that.city)
                && Objects.equals(country, that.country)
                && Objects.equals(refAreaCode, that.refAreaCode);
    }
    @Override
    public int hashCode() {
        return Objects.hash(city, country, refAreaCode);
    }
}
