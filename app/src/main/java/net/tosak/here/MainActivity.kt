package net.tosak.here

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import net.tosak.here.ui.ProximityApp
import net.tosak.here.ui.theme.HereTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HereTheme {
                ProximityApp()
            }
        }
    }
}