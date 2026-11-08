package io.github.katzfuss11.tanzlexikon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.katzfuss11.tanzlexikon.ui.TanzlexikonNavHost
import io.github.katzfuss11.tanzlexikon.ui.theme.TanzlexikonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TanzlexikonTheme {
                TanzlexikonNavHost()
            }
        }
    }
}
