package io.github.katzfuss11.tanzlexikon.ui

import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationTest {
    private lateinit var nav: TestNavHostController

    @Before
    fun setUp() {
        nav = TestNavHostController(ApplicationProvider.getApplicationContext())
        nav.navigatorProvider.addNavigator(ComposeNavigator())
        nav.graph = nav.createGraph(startDestination = Routes.START) {
            composable(Routes.START) {}
            composable(Routes.SUCHE) {}
            composable(Routes.TANZ, arguments = listOf(navArgument("danceId") { type = NavType.LongType })) {}
            composable(
                Routes.FIGUR,
                arguments = listOf(
                    navArgument("figureId") { type = NavType.LongType },
                    navArgument("neu") { type = NavType.BoolType; defaultValue = false },
                ),
            ) {}
        }
    }

    private fun stack() = nav.currentBackStack.value.mapNotNull { e ->
        when (e.destination.route) {
            Routes.START -> "start"
            Routes.SUCHE -> "suche"
            Routes.TANZ -> "tanz/${e.arguments?.getLong("danceId")}"
            Routes.FIGUR -> "figur/${e.arguments?.getLong("figureId")}"
            else -> null
        }
    }

    @Test
    fun openLinkedFigure_afterSeveralJumps_backLeadsToDanceOfCurrentFigure() {
        nav.navigate(Routes.tanz(1))
        nav.navigate(Routes.figur(10))
        nav.openLinkedFigure(figureId = 20, danceId = 2)
        nav.openLinkedFigure(figureId = 30, danceId = 3)
        nav.openLinkedFigure(figureId = 31, danceId = 3)

        assertEquals(listOf("start", "tanz/3", "figur/31"), stack())
    }

    @Test
    fun openLinkedFigure_fromSearch_alsoHasThreeLevels() {
        nav.navigate(Routes.SUCHE)
        nav.navigate(Routes.figur(10))
        nav.openLinkedFigure(figureId = 20, danceId = 2)

        assertEquals(listOf("start", "tanz/2", "figur/20"), stack())
    }
}
