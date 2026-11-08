package com.gpsalarm.app.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaceSearchTest {

    private val eiffel = """
        {"type":"FeatureCollection","features":[
          {"type":"Feature","properties":{"name":"Tour Eiffel","housenumber":"5","street":"Avenue Anatole France",
            "city":"Paris","state":"Île-de-France","country":"France"},
           "geometry":{"type":"Point","coordinates":[2.2945,48.8583]}},
          {"type":"Feature","properties":{"name":"Eiffel Tower","county":"Improvement District No. 9",
            "state":"Alberta","country":"Canada"},
           "geometry":{"type":"Point","coordinates":[-116.235,51.3336]}}
        ]}
    """.trimIndent()

    @Test
    fun `builds a readable name from photon properties and reads lon,lat order`() {
        val places = parsePlaces(eiffel)

        assertEquals(2, places.size)
        assertEquals("Tour Eiffel", places[0].title)
        assertEquals("Avenue Anatole France 5, Paris, Île-de-France, France", places[0].subtitle)
        assertEquals(48.8583, places[0].lat, 1e-9)
        assertEquals(2.2945, places[0].lng, 1e-9)
    }

    @Test
    fun `falls back to county when there is no city`() {
        assertEquals("Improvement District No. 9, Alberta, Canada", parsePlaces(eiffel)[1].subtitle)
    }

    @Test
    fun `repeated parts are collapsed`() {
        val json = """{"features":[{"properties":{"name":"Paris","city":"Paris","country":"France"},
            "geometry":{"coordinates":[2.35,48.85]}}]}"""
        assertEquals("Paris, France", parsePlaces(json).single().displayName)
    }

    @Test
    fun `empty feature list yields no places`() {
        assertEquals(emptyList(), parsePlaces("""{"features":[]}"""))
    }

    @Test
    fun `features without coordinates or any name are skipped`() {
        val json = """{"features":[
            {"properties":{"name":"No geometry"}},
            {"properties":{},"geometry":{"coordinates":[1.0,2.0]}},
            {"properties":{"name":"Fine"},"geometry":{"coordinates":[1.0,2.0]}}
        ]}"""
        assertEquals(listOf("Fine"), parsePlaces(json).map { it.title })
    }

    @Test
    fun `reverse geocode reads display name`() {
        assertEquals("Charleston Road, Mountain View", parseReverse("""{"display_name":"Charleston Road, Mountain View"}"""))
    }

    @Test
    fun `reverse geocode error body yields null`() {
        assertNull(parseReverse("""{"error":"Unable to geocode"}"""))
    }
}
