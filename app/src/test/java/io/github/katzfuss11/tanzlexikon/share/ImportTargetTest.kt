package io.github.katzfuss11.tanzlexikon.share

import io.github.katzfuss11.tanzlexikon.lexikon.CategoryWithDances
import io.github.katzfuss11.tanzlexikon.lexikon.DanceSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportTargetTest {
    private val existing = listOf(
        CategoryWithDances(1, "Standard", listOf(DanceSummary(10, "Tango", 0))),
        CategoryWithDances(2, "Latein", listOf(DanceSummary(20, "Rumba", 3), DanceSummary(21, "Salsa", 0))),
        CategoryWithDances(3, "Meine Kurse", listOf(DanceSummary(30, "Casino", 1))),
    )

    @Test fun proposeTarget_matchesDanceIgnoringCase_acrossCategories() {
        assertEquals(ImportTarget.Existing(20, "Rumba", "Latein"), proposeTarget(existing, "Latein", " rumba "))
        // Tanz liegt beim Empfänger in einer anderen Kategorie: trotzdem dorthin.
        assertEquals(ImportTarget.Existing(30, "Casino", "Meine Kurse"), proposeTarget(existing, "Latein", "casino"))
    }

    @Test fun proposeTarget_unknownDance_proposesNewInExistingCategorySpelling() {
        assertEquals(ImportTarget.New("Latein", "Bachata"), proposeTarget(existing, "latein", "Bachata"))
        assertEquals(ImportTarget.New("Hochzeit", "Eröffnungswalzer"), proposeTarget(existing, "Hochzeit", "Eröffnungswalzer"))
    }
}
