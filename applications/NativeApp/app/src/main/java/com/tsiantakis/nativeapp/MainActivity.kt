package com.tsiantakis.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.tsiantakis.nativeapp.screens.Benchmarks
import com.tsiantakis.nativeapp.ui.theme.NativeAppTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContent {
            NativeAppTheme {
                LocalView.current.keepScreenOn = true
                Scaffold(
                    topBar = { TopAppBar(title = { Text("NativeApp") }) }) { paddingValues ->
                    Column(
                        Modifier.padding(paddingValues)
                    ) { Benchmarks() }
                }
            }
        }
    }
}
