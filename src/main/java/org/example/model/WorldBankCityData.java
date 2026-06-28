package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// World Bank Data360 indicators fetched for a city's country
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorldBankCityData implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum FetchStatus { OK, PARTIAL, FAILED, NO_COUNTRY, OFFLINE }
    private String city;
    private String country;
    private String refAreaCode;
    private FetchStatus status = FetchStatus.FAILED;
    private String statusMessage;
    private LocalDateTime fetchedAt;
    private List<WorldBankIndicatorValue> indicators = new ArrayList<>();
    public WorldBankCityData() {
        this.fetchedAt = LocalDateTime.now();
    }
    public WorldBankCityData(CityLocation location) {
        this();
        if (location != null) {
            this.city = location.getCity();
            this.country = location.getCountry();
            this.refAreaCode = location.getRefAreaCode();
        }
    }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getRefAreaCode() { return refAreaCode; }
    public void setRefAreaCode(String refAreaCode) { this.refAreaCode = refAreaCode; }
    public FetchStatus getStatus() { return status; }
    public void setStatus(FetchStatus status) { this.status = status; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
    public List<WorldBankIndicatorValue> getIndicators() {
        return indicators;
    }
    public void setIndicators(List<WorldBankIndicatorValue> indicators) {
        this.indicators = indicators != null ? new ArrayList<>(indicators) : new ArrayList<>();
    }
    public void addIndicator(WorldBankIndicatorValue indicator) {
        if (indicator != null) {
            this.indicators.add(indicator);
        }
    }
    @JsonIgnore
    public List<WorldBankIndicatorValue> getIndicatorsView() {
        return Collections.unmodifiableList(indicators);
    }
    public Optional<WorldBankIndicatorValue> findIndicator(String indicatorId) {
        if (indicatorId == null) return Optional.empty();
        return indicators.stream()
                .filter(i -> indicatorId.equals(i.getIndicatorId()))
                .findFirst();
    }
    public Double getNumericValue(String indicatorId) {
        return findIndicator(indicatorId).map(WorldBankIndicatorValue::getValue).orElse(null);
    }
    @JsonIgnore
    public int countWithValues() {
        return (int) indicators.stream().filter(WorldBankIndicatorValue::hasValue).count();
    }
    @JsonIgnore
    public boolean isUsable() {
        return status == FetchStatus.OK || status == FetchStatus.PARTIAL;
    }
    @JsonIgnore
    public String displayLabel() {
        if (country == null || country.isBlank()) return city != null ? city : "Unknown";
        return city + ", " + country;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldBankCityData that)) return false;
        return Objects.equals(city, that.city)
                && Objects.equals(country, that.country)
                && Objects.equals(refAreaCode, that.refAreaCode);
    }
    @Override
    public int hashCode() {
        return Objects.hash(city, country, refAreaCode);
    }
}
