package io.github.katzfuss11.tanzlexikon.ui.tanz

import io.github.katzfuss11.tanzlexikon.ui.papierkorb.daysLeft
import org.junit.Assert.assertEquals
import org.junit.Test

class DaysLeftTest {
    private val day = 24L * 60 * 60 * 1000

    @Test fun daysLeft_countsDownAndNeverNegative() {
        val now = 1_800_000_000_000L
        assertEquals(30, daysLeft(now, now))
        assertEquals(29, daysLeft(now, now - day))
        assertEquals(1, daysLeft(now, now - 29 * day - 1))
        assertEquals(0, daysLeft(now, now - 31 * day))
    }
}
