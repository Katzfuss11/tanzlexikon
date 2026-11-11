package io.github.katzfuss11.tanzlexikon.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.ui.empfang.EmpfangScreen
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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

/** Die Adressen der Bildschirme. */
object Routes {
    const val START = "start"
    const val TANZ = "tanz/{danceId}"
    const val FIGUR = "figur/{figureId}"
    const val SUCHE = "suche"
    const val PAPIERKORB = "papierkorb"
    const val EINSTELLUNGEN = "einstellungen"
    const val EMPFANG = "empfang"

    fun tanz(danceId: Long) = "tanz/$danceId"
    fun figur(figureId: Long) = "figur/$figureId"
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
    NavHost(navController = nav, startDestination = Routes.START) {
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
            )
        }
        composable(Routes.FIGUR, arguments = listOf(navArgument("figureId") { type = NavType.LongType })) { entry ->
            FigurScreen(figureId = entry.arguments!!.getLong("figureId"), onBack = { nav.popBackStack() })
        }
        composable(Routes.PAPIERKORB) { PapierkorbScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.EINSTELLUNGEN) {
            EinstellungenScreen(onBack = { nav.popBackStack() }, onTrash = { nav.navigate(Routes.PAPIERKORB) })
        }
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
                        nav.navigate(Routes.figur(id)) { popUpTo(Routes.EMPFANG) { inclusive = true } }
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
