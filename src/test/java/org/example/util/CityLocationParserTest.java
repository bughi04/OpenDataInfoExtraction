package org.example.util;

import org.example.model.CityLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests for parsing city and country names from GraphML filenames and resolving World Bank country codes.
class CityLocationParserTest {
    @Test
    void parsesCityAndCountryFromGraphName() {
        CityLocation loc = CityLocationParser.parse("Barcelona,_Spain");
        assertEquals("Barcelona", loc.getCity());
        assertEquals("Spain", loc.getCountry());
        assertEquals("ESP", loc.getRefAreaCode());
        assertEquals("Barcelona, Spain", loc.displayLabel());
        assertTrue(loc.hasCountryCode());
    }
    @Test
    void parsesWashingtonDcWithUnderscores() {
        CityLocation loc = CityLocationParser.parse("Washington_DC,_United_States");
        assertEquals("Washington DC", loc.getCity());
        assertEquals("United States", loc.getCountry());
        assertEquals("USA", loc.getRefAreaCode());
    }
    @Test
    void parseFromFileStripsExtensionAndPath() {
        CityLocation loc = CityLocationParser.parseFromFile("C:/graphs/Amsterdam,_Netherlands.graphml");
        assertEquals("Amsterdam", loc.getCity());
        assertEquals("Netherlands", loc.getCountry());
        assertEquals("NLD", loc.getRefAreaCode());
    }
    @Test
    void nameWithoutCommaUsesWholeStringAsCity() {
        CityLocation loc = CityLocationParser.parse("Singapore");
        assertEquals("Singapore", loc.getCity());
        assertNull(loc.getCountry());
        assertEquals("SGP", loc.getRefAreaCode());
    }
    @Test
    void blankInputReturnsUnknown() {
        CityLocation loc = CityLocationParser.parse("  ");
        assertEquals("Unknown", loc.getCity());
        assertNull(loc.getCountry());
        assertNull(loc.getRefAreaCode());
        assertFalse(loc.hasCountryCode());
    }
}