package com.example.rutaarasof

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rutaarasof.ui.MainScreen
import com.example.rutaarasof.ui.theme.RutaARASOFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RutaARASOFTheme {
                MainScreen()
            }
        }
    }
}
