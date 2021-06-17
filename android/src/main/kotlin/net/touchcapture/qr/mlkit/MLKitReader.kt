package net.touchcapture.qr.mlkit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions

import java.io.IOException

internal class MLKitReader(width: Int, height: Int, private val context: Activity, options: BarcodeScannerOptions,
                           private val startedCallback: MLKitStartedCallback, communicator: MLKitCallbacks,
                           texture: SurfaceTexture) {

    val mlkitCamera: Camera = if (Build.VERSION.SDK_INT >= 21) {
        Log.i(TAG, "Using new camera API.")
        Camera2(width, height, texture, context, MLKitDetector(communicator, options))
    } else {
        Log.i(TAG, "Using old camera API.")
        Camera1(width, height, texture, context, MLKitDetector(communicator, options))
    }

    @Throws(IOException::class, Exception::class)
    fun start() {
        if (!hasCameraHardware(context)) {
            throw Exception(Exception.Reason.NoHardware)
        }
        if (!checkCameraPermission(context)) {
            throw Exception(Exception.Reason.NoPermissions)
        } else {
            try {
                mlkitCamera.start()
                startedCallback.started()
            } catch (t: Throwable) {
                startedCallback.startingFailed(t)
            }
        }
    }

    fun stop() {
        mlkitCamera.stop()
    }

    // Ignore because FEATURE_CAMERA_ANY isn't available on API 16
    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    private fun hasCameraHardware(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } else {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }
    }

    private fun checkCameraPermission(context: Context): Boolean {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        val res = context.checkCallingOrSelfPermission(permissions[0])
        return res == PackageManager.PERMISSION_GRANTED
    }

    internal interface MLKitStartedCallback {
        fun started()
        fun startingFailed(t: Throwable?)
    }

    internal class Exception(private val reason: Reason) : java.lang.Exception("MLKit reader failed because $reason") {
        fun reason(): Reason {
            return reason
        }

        internal enum class Reason {
            NoHardware, NoPermissions, NoBackCamera
        }
    }

    companion object {
        private const val TAG = "qr.mlkit.reader"
    }

}