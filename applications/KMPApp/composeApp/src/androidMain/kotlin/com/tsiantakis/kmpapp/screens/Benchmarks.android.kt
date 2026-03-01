package com.tsiantakis.kmpapp.screens

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Trace
import android.provider.ContactsContract
import androidx.core.content.getSystemService
import com.tsiantakis.kmpapp.BuildConfig
import com.tsiantakis.kmpapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import kotlin.random.Random


actual object AppContext {
    private var value: WeakReference<Context?>? = null
    fun set(context: Context) {
        value = WeakReference(context)
    }

    internal fun get(): Context {
        return value?.get() ?: throw RuntimeException("Context Error")
    }
}

actual fun debugBuild(): String = BuildConfig.BUILD_TYPE


actual fun debuggable(context: AppContext): Boolean =
    (context.get().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

actual fun profileable(context: AppContext): Boolean =
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
        context.get().applicationInfo.isProfileableByShell || context.get().applicationInfo.isProfileable
    } else {
        false
    }

actual fun traceBeginAsyncSection(methodName: String, cookie: Int) =
    Trace.beginAsyncSection(methodName, cookie)

actual fun traceEndAsyncSection(methodName: String, cookie: Int) =
    Trace.endAsyncSection(methodName, cookie)

actual suspend fun accelerometerTest(context: AppContext): Unit =
    suspendCancellableCoroutine { continuation ->
        val sensorManager = AppContext.get().getSystemService<SensorManager>()
            ?: throw IllegalStateException("Null Sensor Manager!")
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val cookie = Random.nextInt()
        check(accelerometerSensor != null)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                check(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && continuation.isActive)
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                Trace.endAsyncSection("kmp:accelerometer", cookie)
                sensorManager.unregisterListener(this)
                Timber.i("Accelerometer values read x:$x y:$y z:$z")
                continuation.resume(Unit) { _, _, _ -> }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        Trace.beginAsyncSection("kmp:accelerometer", cookie)
        sensorManager.registerListener(
            listener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST
        )
        continuation.invokeOnCancellation { e ->
            Timber.e(e)
            sensorManager.unregisterListener(listener)
        }
    }

actual suspend fun loadImageToDevice(context: AppContext, fileName: String) {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.get().filesDir, fileName)
            context.get().resources.openRawResource(R.raw.solar_system).use { inputStream ->
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

actual suspend fun imageTest(context: AppContext, fileName: String) = withContext(Dispatchers.IO) {
    var bytes: ByteArray = byteArrayOf()
    val cookie = Random.nextInt()
    val result = runCatching {
        Trace.beginAsyncSection("kmp:image", cookie)
        val file = File(context.get().filesDir, fileName)
        bytes = file.readBytes()
        Trace.endAsyncSection("kmp:image", cookie)
    }
    result.fold(onSuccess = { _ ->
        Timber.i("Read ${bytes.size} bytes from disk")
    }, onFailure = { e ->
        Timber.e(e)
        throw IllegalStateException("Image benchmark failed")
    })
}

actual suspend fun deleteImageFromDevice(context: AppContext, fileName: String) {
    val file = File(context.get().filesDir, fileName)
    val result = runCatching {
        file.delete()
    }
    result.fold(onSuccess = {
        Timber.i("Image deleted successfully")
    }, onFailure = { e ->
        Timber.e(e)
    })
}

actual suspend fun contactTest(context: AppContext, name: String, phoneNumber: String) =
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
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name
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
            Trace.beginAsyncSection("kmp:contacts", cookie)
            contactID = context.get().contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Trace.endAsyncSection("kmp:contacts", cookie)
        }
        result.fold(onSuccess = {
            val parsedID: Long = contactID[0].uri!!.let { ContentUris.parseId(it) }
            Timber.i("Created contact successfully with id: $parsedID")
            val contactUri = ContentUris.withAppendedId(
                ContactsContract.RawContacts.CONTENT_URI, parsedID
            )
            val deleted = context.get().contentResolver.delete(contactUri, null, null)
            Timber.i("Deleted $deleted contacts successfully")
        }, onFailure = { e ->
            Timber.e(e)
            throw IllegalStateException("Contact benchmark failed")
        })
    }

actual suspend fun lightTest(context: AppContext) = suspendCancellableCoroutine { continuation ->
    val sensorManager = AppContext.get().getSystemService<SensorManager>()
        ?: throw IllegalStateException("Null Sensor Manager!")
    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    val cookie = Random.nextInt()
    check(lightSensor != null)
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_LIGHT && continuation.isActive) {
                val lux = event.values[0]
                Trace.endAsyncSection("kmp:light", cookie)
                sensorManager.unregisterListener(this)
                Timber.i("Light value read as $lux")
                continuation.resume(Unit) { _, _, _ -> }
            } else {
                throw IllegalStateException("Light test failed")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    Trace.beginAsyncSection("kmp:light", cookie)
    sensorManager.registerListener(
        listener, lightSensor, SensorManager.SENSOR_DELAY_FASTEST
    )
    continuation.invokeOnCancellation { e ->
        Timber.e(e)
        sensorManager.unregisterListener(listener)
    }
}

actual fun printToLog(message: String) {
    Timber.i(message)
}