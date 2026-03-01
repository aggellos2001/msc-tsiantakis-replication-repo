package com.tsiantakis.kmpapp

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tsiantakis.kmpapp.screens.AppContext
import timber.log.Timber

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setContent {
            LocalView.current.keepScreenOn = true
            val requiredPermissions = rememberMultiplePermissionsState(
                listOf(
                    READ_CONTACTS, WRITE_CONTACTS, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION
                )
            )
            LaunchedEffect(Unit) {
                if (!requiredPermissions.allPermissionsGranted) {
                    requiredPermissions.launchMultiplePermissionRequest()
                }
            }
            AppContext.set(applicationContext)
            App()
        }
    }
}