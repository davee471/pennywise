package stud.brokers.pennywise

import android.os.Build

/**
 * Represents the Android platform implementation.
 */
class AndroidPlatform : Platform {
    /**
     * The name of the platform, including the Android API level.
     */
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Returns the current [Platform] instance for Android.
 *
 * @return An instance of [AndroidPlatform].
 */
actual fun getPlatform(): Platform = AndroidPlatform()