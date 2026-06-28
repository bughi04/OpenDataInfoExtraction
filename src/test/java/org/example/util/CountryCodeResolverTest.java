package org.example.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
// Parameterized tests verifying country-name to ISO 3166-1 alpha-3 code resolution, including aliases and blank input.
class CountryCodeResolverTest {
    @ParameterizedTest
    @CsvSource({
            "Netherlands, NLD",
            "netherlands, NLD",
            "  Spain  , ESP",
            "United States, USA",
            "United Kingdom, GBR",
            "South Korea, KOR",
            "UAE, ARE",
            "UK, GBR",
            "US, USA",
            "Russian Federation, RUS"
    })
    void resolvesKnownCountries(String input, String expected) {
        assertEquals(expected, CountryCodeResolver.resolve(input));
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void blankInputReturnsNull(String input) {
        assertNull(CountryCodeResolver.resolve(input));
    }
    @Test
    void partialMatchResolvesSubstring() {
        assertEquals("ESP", CountryCodeResolver.resolve("Spain"));
        assertEquals("DEU", CountryCodeResolver.resolve("Germany"));
    }
    @Test
    void unknownCountryReturnsNull() {
        assertNull(CountryCodeResolver.resolve("Atlantis"));
        assertNull(CountryCodeResolver.resolve("Narnia"));
    }
}