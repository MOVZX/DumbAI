package `is`.xyz.mpv

import android.content.Context
import android.view.Surface
import androidx.annotation.Keep
import org.movzx.dumbai.util.Logger

@Keep
object MPVLib {
    private const val TAG = "MPVLib"

    var initialized = false
        private set

    var lastOwnerId: Int = -1

    init {
        System.loadLibrary("player")
    }

    private external fun create(context: Context)

    private external fun init()

    external fun destroy()

    private external fun attachSurface(surface: Surface)

    private external fun detachSurface()

    private external fun command(cmd: Array<String>)

    private external fun setPropertyString(name: String, value: String)

    private external fun setPropertyInt(name: String, value: Int)

    private external fun setPropertyBoolean(name: String, value: Boolean)

    private external fun setPropertyDouble(name: String, value: Double)

    external fun getPropertyString(name: String): String?

    external fun getPropertyInt(name: String): Int?

    external fun getPropertyBoolean(name: String): Boolean?

    external fun getPropertyDouble(name: String): Double?

    external fun observeProperty(name: String, format: Int)

    private external fun setOptionString(name: String, value: String)

    external fun grabThumbnail(width: Int): ByteArray?

    fun initialize(context: Context) {
        synchronized(this) {
            if (!initialized) {
                try {
                    create(context.applicationContext)
                    init()
                    initialized = true
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to initialize mpv", e)
                }
            }
        }
    }

    fun safeAttachSurface(surface: Surface) = synchronized(this) { attachSurface(surface) }

    fun safeDetachSurface() = synchronized(this) { detachSurface() }

    fun safeCommand(cmd: Array<String>) = synchronized(this) { command(cmd) }

    fun safeSetPropertyString(name: String, value: String) =
        synchronized(this) { setPropertyString(name, value) }

    fun safeSetPropertyInt(name: String, value: Int) =
        synchronized(this) { setPropertyInt(name, value) }

    fun safeSetPropertyBoolean(name: String, value: Boolean) =
        synchronized(this) { setPropertyBoolean(name, value) }

    fun safeSetPropertyDouble(name: String, value: Double) =
        synchronized(this) { setPropertyDouble(name, value) }

    fun safeSetOptionString(name: String, value: String) =
        synchronized(this) { setOptionString(name, value) }

    fun safeGetPropertyInt(name: String): Int? = synchronized(this) { getPropertyInt(name) }

    fun safeGetPropertyDouble(name: String): Double? =
        synchronized(this) { getPropertyDouble(name) }

    fun safeGetPropertyBoolean(name: String): Boolean? =
        synchronized(this) { getPropertyBoolean(name) }

    fun safeGetPropertyLong(name: String): Long? =
        synchronized(this) { getPropertyInt(name)?.toLong() }

    @JvmStatic fun eventProperty(property: String) {}

    @JvmStatic fun eventProperty(property: String, value: Long) {}

    @JvmStatic fun eventProperty(property: String, value: Boolean) {}

    @JvmStatic fun eventProperty(property: String, value: String) {}

    @JvmStatic fun eventProperty(property: String, value: Double) {}

    @JvmStatic fun event(eventId: Int) {}

    @JvmStatic
    fun logMessage(prefix: String, level: Int, text: String) {
        if (level <= 20) Logger.d(TAG, "[\$prefix] \$text")
    }
}
