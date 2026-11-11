package io.github.katzfuss11.tanzlexikon.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.ui.empfang.EmpfangScreen
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.TanzlexikonApp
import io.github.katzfuss11.tanzlexikon.ui.einstellungen.EinstellungenScreen
import io.github.katzfuss11.tanzlexikon.ui.figur.FigurScreen
import io.github.katzfuss11.tanzlexikon.ui.papierkorb.PapierkorbScreen
import io.github.katzfuss11.tanzlexikon.ui.start.StartScreen
import io.github.katzfuss11.tanzlexikon.ui.tanz.TanzScreen
import io.github.katzfuss11.tanzlexikon.ui.suche.SucheScreen
import io.github.katzfuss11.tanzlexikon.ui.versionen.VersionsverlaufScreen

/** Die Adressen der Bildschirme. */
object Routes {
    const val START = "start"
    const val TANZ = "tanz/{danceId}"
    const val FIGUR = "figur/{figureId}?neu={neu}"
    const val SUCHE = "suche"
    const val PAPIERKORB = "papierkorb"
    const val EINSTELLUNGEN = "einstellungen"
    const val EMPFANG = "empfang"
    const val VERSIONSVERLAUF = "versionsverlauf"

    fun tanz(danceId: Long) = "tanz/$danceId"
    /** [neu]: frisch angelegt oder empfangen, dann wird einmal Verknüpfen vorgeschlagen. */
    fun figur(figureId: Long, neu: Boolean = false) = "figur/$figureId?neu=$neu"
}

/**
 * Holt ein ViewModel, das Zugriff auf die Dienste der App bekommt.
 * In [create] liefert `createSavedStateHandle()` einen Zustand, der das Beenden durch Android übersteht.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: CreationExtras.(TanzlexikonApp) -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory { initializer { create(this, this[APPLICATION_KEY] as TanzlexikonApp) } },
)

@Composable
fun TanzlexikonNavHost(nav: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as TanzlexikonApp
    val pendingImport by app.pendingImport.collectAsStateWithLifecycle()
    // Wurde eine .tanzfigur geöffnet, direkt zum Empfang springen.
    LaunchedEffect(pendingImport) {
        if (pendingImport != null && nav.currentDestination?.route != Routes.EMPFANG) {
            nav.navigate(Routes.EMPFANG) { launchSingleTop = true }
        }
    }
    // Beim Hineingehen gleitet der Bildschirm nach links weg, beim Zurückgehen nach rechts.
    val slide = tween<IntOffset>(SLIDE_MS)
    NavHost(
        navController = nav,
        startDestination = Routes.START,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, slide) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slide) },
    ) {
        composable(Routes.START) {
            StartScreen(
                onOpenDance = { nav.navigate(Routes.tanz(it)) },
                onSearch = { nav.navigate(Routes.SUCHE) },
                onTrash = { nav.navigate(Routes.PAPIERKORB) },
                onSettings = { nav.navigate(Routes.EINSTELLUNGEN) },
                onOpenFigure = { nav.navigate(Routes.figur(it)) },
            )
        }
        composable(Routes.SUCHE) {
            SucheScreen(
                onBack = { nav.popBackStack() },
                onOpenFigure = { nav.navigate(Routes.figur(it)) },
                onOpenDance = { nav.navigate(Routes.tanz(it)) },
            )
        }
        composable(Routes.TANZ, arguments = listOf(navArgument("danceId") { type = NavType.LongType })) { entry ->
            TanzScreen(
                danceId = entry.arguments!!.getLong("danceId"),
                onBack = { nav.popBackStack() },
                onOpenFigure = { nav.navigate(Routes.figur(it)) },
                onFigureCreated = { nav.navigate(Routes.figur(it, neu = true)) },
            )
        }
        composable(
            Routes.FIGUR,
            arguments = listOf(
                navArgument("figureId") { type = NavType.LongType },
                navArgument("neu") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            FigurScreen(
                figureId = entry.arguments!!.getLong("figureId"),
                isNew = entry.arguments!!.getBoolean("neu"),
                onBack = { nav.popBackStack() },
                onOpenFigure = { nav.openLinkedFigure(it.id, it.danceId) },
            )
        }
        composable(Routes.PAPIERKORB) { PapierkorbScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.EINSTELLUNGEN) {
            EinstellungenScreen(
                onBack = { nav.popBackStack() },
                onTrash = { nav.navigate(Routes.PAPIERKORB) },
                onChangelog = { nav.navigate(Routes.VERSIONSVERLAUF) },
            )
        }
        composable(Routes.VERSIONSVERLAUF) { VersionsverlaufScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.EMPFANG) {
            // Die Datei wird beim Betreten festgehalten, damit ein späteres Teilen sie nicht austauscht.
            val uri = remember { app.pendingImport.value }
            fun leave() {
                app.pendingImport.value = null
                if (!nav.popBackStack()) nav.navigate(Routes.START)
            }
            if (uri == null) {
                LaunchedEffect(Unit) { leave() }
            } else {
                EmpfangScreen(
                    uri = uri,
                    onClose = ::leave,
                    onAdded = { id ->
                        app.pendingImport.value = null
                        nav.navigate(Routes.figur(id, neu = true)) { popUpTo(Routes.EMPFANG) { inclusive = true } }
                    },
                )
            }
        }
    }
    // Hinweis auf neue Versionen, über jedem Bildschirm.
    UpdateDialogs()
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}

/**
 * Sprung über „Siehe auch“: Der Verlauf wird zu Startseite → Tanz der Figur → Figur.
 * Zurück führt so in den Tanz der gerade gezeigten Figur, nicht durch alle vorher besuchten Figuren.
 */
fun NavController.openLinkedFigure(figureId: Long, danceId: Long) {
    navigate(Routes.tanz(danceId)) { popUpTo(Routes.START) }
    navigate(Routes.figur(figureId))
}

/** Dauer eines Bildschirmwechsels. */
private const val SLIDE_MS = 300
