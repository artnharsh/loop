package com.example.loop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.loop.theme.LoopTheme
import com.example.loop.ui.MainViewModel
import com.example.loop.ui.StrictBlockApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoopTheme {
                StrictBlockApp(viewModel)
            }
        }
    }
}
