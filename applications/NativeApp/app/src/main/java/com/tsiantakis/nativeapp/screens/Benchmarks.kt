package com.tsiantakis.nativeapp.screens

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Trace
import android.provider.ContactsContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tsiantakis.nativeapp.BuildConfig
import com.tsiantakis.nativeapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

const val REPETITIONS = 30

var testsRunning by mutableStateOf(false)

inline fun repeatBlock(iterations: Int, block: () -> Unit) {
    testsRunning = true
    check(iterations > 0)
    repeat(iterations) {
        block()
    }
    testsRunning = false
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Benchmarks(
) {
    val context = LocalContext.current

    val debugBuild = BuildConfig.BUILD_TYPE
    val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val profileable =
        context.applicationInfo.isProfileableByShell || context.applicationInfo.isProfileable

    val requiredPermissions = rememberMultiplePermissionsState(
        listOf(
            READ_CONTACTS, WRITE_CONTACTS, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION
        )
    )

    val scope = rememberCoroutineScope()
    /**
     * This launched effect checks whether we have all the required permissions each time this
     * composable enters the composition (ie. the user navigates to this page)
     */
    LaunchedEffect(Unit) {
        if (!requiredPermissions.allPermissionsGranted) {
            requiredPermissions.launchMultiplePermissionRequest()
        }
    }

    val sensorManager = remember { context.getSystemService<SensorManager>() }
    var currentTestRunning by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("Build type: $debugBuild")
        Text("Debuggable flag: $debuggable")
        Text("Profileable flag: $profileable")

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "accelerometer"
                repeatBlock(REPETITIONS) {
                    accelerometerTest(sensorManager)
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run accelerometer test") }

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "image"
                val filename = "solar_system.jpg"
                loadImageToDevice(context = context, filename)
                repeatBlock(REPETITIONS) {
                    imageTest(context, filename)
                }
                val deletedImage = context.deleteFile(filename)
                if (deletedImage) {
                    Timber.i("Image deleted successfully after testing!")
                } else {
                    Timber.e("Image could not be removed!")
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run image test") }

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "contact"
                repeatBlock(REPETITIONS) {
                    contactTest(context, "John Doe", "6934567890")
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run contact test") }

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "light"
                repeatBlock(REPETITIONS) {
                    lightTest(sensorManager)
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run light test") }

        Button(enabled = !testsRunning, onClick = {
            scope.launch {
                currentTestRunning = "math"
                repeatBlock(REPETITIONS) {
                    mathTest()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Run math test") }

        AnimatedVisibility(testsRunning) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
                Text(
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    text = "The $currentTestRunning test is running. Please wait..."
                )
            }
        }
    }
}

/**
 * 1.Read a single value (x,y,z) from the accelerometer sensor
 */
suspend fun accelerometerTest(sensorManager: SensorManager?) =
    suspendCancellableCoroutine { continuation ->
        require(sensorManager != null)
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val cookie = Random.nextInt()
        check(accelerometerSensor != null && continuation.isActive)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                check(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && continuation.isActive)
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                Trace.endAsyncSection("native:accelerometer", cookie)
                sensorManager.unregisterListener(this)
                Timber.i("Accelerometer values read x:$x y:$y z:$z")
                continuation.resume {}
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        Trace.beginAsyncSection("native:accelerometer", cookie)
        sensorManager.registerListener(
            listener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST
        )
        continuation.invokeOnCancellation { e ->
            Timber.e(e)
            sensorManager.unregisterListener(listener)
        }
    }

suspend fun loadImageToDevice(context: Context, fileName: String) {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            context.resources.openRawResource(R.raw.solar_system).use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            throw IllegalStateException("Image test cannot proceed unless the image is first loaded into the device")
        }
    }
}

/**
 * 2. Loading an image from the device filesystem to the memory
 */
suspend fun imageTest(context: Context, fileName: String) = withContext(Dispatchers.IO) {
    var bytes: ByteArray = byteArrayOf()
    val cookie = Random.nextInt()
    val result = runCatching {
        Trace.beginAsyncSection("native:image", cookie)
        val file = File(context.filesDir, fileName)
        bytes = file.readBytes()
        Trace.endAsyncSection("native:image", cookie)
    }
    result.fold(onSuccess = { _ ->
        Timber.i("Read ${bytes.size} bytes from disk")
    }, onFailure = { e ->
        Timber.e(e)
        throw IllegalStateException("Image benchmark failed")
    })
}

/**
 * 3. Creating a contact with a name an a number in the database
 */
suspend fun contactTest(context: Context, name: String, phoneNumber: String) =
    withContext(Dispatchers.IO) {
        val ops = ArrayList<ContentProviderOperation>()
        val cookie = Random.nextInt()
        var contactID: Array<ContentProviderResult> = arrayOf()
        val result = runCatching {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    ).withValue(
                        ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name
                    ).build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    ).withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    ).build()
            )
            Trace.beginAsyncSection("native:contacts", cookie)
            contactID = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Trace.endAsyncSection("native:contacts", cookie)
        }
        result.fold(onSuccess = {
            val parsedID: Long = contactID[0].uri!!.let { ContentUris.parseId(it) }
            Timber.i("Created contact successfully with id: $parsedID")
            val contactUri = ContentUris.withAppendedId(
                ContactsContract.RawContacts.CONTENT_URI, parsedID
            )
            val deleted = context.contentResolver.delete(contactUri, null, null)
            Timber.i("Deleted $deleted contacts successfully")
        }, onFailure = { e ->
            Timber.e(e)
            throw IllegalStateException("Contact benchmark failed")
        })
    }

/**
 * 4. Getting a reading from the hardware light sensor
 */
suspend fun lightTest(sensorManager: SensorManager?) = suspendCancellableCoroutine { continuation ->
    require(sensorManager != null)
    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    val cookie = Random.nextInt()
    check(lightSensor != null)
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_LIGHT && continuation.isActive) {
                val lux = event.values[0]
                Trace.endAsyncSection("native:light", cookie)
                sensorManager.unregisterListener(this)
                Timber.i("Light value read as $lux")
                continuation.resume {}
            } else {
                throw IllegalStateException("Light test failed")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    Trace.beginAsyncSection("native:light", cookie)
    sensorManager.registerListener(
        listener, lightSensor, SensorManager.SENSOR_DELAY_FASTEST
    )
    continuation.invokeOnCancellation { e ->
        Timber.e(e)
        sensorManager.unregisterListener(listener)
    }
}

/**
 * 5. Math test
 */
suspend fun mathTest() = withContext(Dispatchers.Default) {
    var localResult = 0.0
    val cookie = Random.nextInt()
    Trace.beginAsyncSection("native:math", cookie)
    for (j in 1..5) {
        for (k in 1..1_000_000) {
            localResult += log2(k.toDouble()) + (3 * k / 2 * j) + sqrt(k.toDouble()) + k.toDouble()
                .pow(j - 1)
        }
    }
    Trace.endAsyncSection("native:math", cookie)
    Timber.i("Calculated math result as $localResult")
}
