package io.github.katzfuss11.tanzlexikon.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    @Test fun parseVersion_comparesNumericallyNotAsText() {
        assertTrue(parseVersion("v0.10.0")!! > parseVersion("0.9.3")!!)
    }

    @Test fun parseVersion_missingPartsAndSuffixes() {
        assertEquals(Version(1, 2, 0), parseVersion("1.2"))
        assertEquals(Version(1, 2, 0), parseVersion("1.2.0-beta"))
        assertEquals(Version(1, 2, 3), parseVersion("V1.2.3+7"))
    }

    @Test fun parseVersion_garbage_returnsNull() {
        assertNull(parseVersion("abc"))
        assertNull(parseVersion(""))
        assertNull(parseVersion("1.x.3"))
    }
}
