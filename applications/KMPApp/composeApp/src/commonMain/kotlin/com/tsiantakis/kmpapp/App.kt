package com.tsiantakis.kmpapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tsiantakis.kmpapp.screens.Benchmarks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("KMP App") }) }) { paddingValues ->
            Column(
                Modifier.padding(paddingValues)
            ) { Benchmarks() }
        }
    }
}
