package io.github.katzfuss11.tanzlexikon.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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

/** Holt ein ViewModel, das Zugriff auf die Dienste der App bekommt. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (TanzlexikonApp) -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory { initializer { create(this[APPLICATION_KEY] as TanzlexikonApp) } },
)

@Composable
fun TanzlexikonNavHost(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = Routes.START) {
        composable(Routes.START) {
            StartScreen(
                onOpenDance = { nav.navigate(Routes.tanz(it)) },
                onSearch = { nav.navigate(Routes.SUCHE) },
                onTrash = { nav.navigate(Routes.PAPIERKORB) },
                onSettings = { nav.navigate(Routes.EINSTELLUNGEN) },
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
        composable(Routes.EINSTELLUNGEN) { Placeholder(onBack = { nav.popBackStack() }) }
        composable(Routes.EMPFANG) { Placeholder(onBack = { nav.popBackStack() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Placeholder(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.coming_soon))
        }
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}
