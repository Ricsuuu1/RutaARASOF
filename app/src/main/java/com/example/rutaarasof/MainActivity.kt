package com.example.rutaarasof

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rutaarasof.ui.CampusViewModel
import com.example.rutaarasof.ui.screens.MapScreen
import com.example.rutaarasof.ui.theme.RutaARASOFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RutaARASOFTheme {
                val viewModel: CampusViewModel = viewModel()
                MapScreen(viewModel = viewModel)
            }
        }
    }
}
