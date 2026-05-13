package org.movzx.dibella.util

import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient

fun Interceptor.Chain.withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
    return TimeoutChainWrapper(this, readTimeout = unit.toMillis(timeout.toLong()))
}

fun Interceptor.Chain.withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
    return TimeoutChainWrapper(this, connectTimeout = unit.toMillis(timeout.toLong()))
}

fun Interceptor.Chain.withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
    return TimeoutChainWrapper(this, writeTimeout = unit.toMillis(timeout.toLong()))
}

private class TimeoutChainWrapper(
    private val original: Interceptor.Chain,
    private val connectTimeout: Long = -1,
    private val readTimeout: Long = -1,
    private val writeTimeout: Long = -1,
) : Interceptor.Chain {
    override fun request(): okhttp3.Request = original.request()

    override fun proceed(request: okhttp3.Request): okhttp3.Response {
        val client = OkHttpClient.Builder()
            .connectTimeout(
                if (connectTimeout > 0) TimeUnit.MILLISECONDS.convert(connectTimeout, TimeUnit.MILLISECONDS) else original.connectTimeoutMillis().toLong(),
                TimeUnit.MILLISECONDS
            )
            .readTimeout(
                if (readTimeout > 0) TimeUnit.MILLISECONDS.convert(readTimeout, TimeUnit.MILLISECONDS) else original.readTimeoutMillis().toLong(),
                TimeUnit.MILLISECONDS
            )
            .writeTimeout(
                if (writeTimeout > 0) TimeUnit.MILLISECONDS.convert(writeTimeout, TimeUnit.MILLISECONDS) else original.writeTimeoutMillis().toLong(),
                TimeUnit.MILLISECONDS
            )
            .build()
        return client.newCall(request).execute()
    }

    override fun connection(): okhttp3.Connection? = original.connection()

    override fun call(): Call = original.call()

    override fun connectTimeoutMillis(): Int = if (connectTimeout > 0) connectTimeout.toInt() else original.connectTimeoutMillis()

    override fun readTimeoutMillis(): Int = if (readTimeout > 0) readTimeout.toInt() else original.readTimeoutMillis()

    override fun writeTimeoutMillis(): Int = if (writeTimeout > 0) writeTimeout.toInt() else original.writeTimeoutMillis()

    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
        return TimeoutChainWrapper(this, connectTimeout = unit.toMillis(timeout.toLong()), readTimeout = readTimeout, writeTimeout = writeTimeout)
    }

    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
        return TimeoutChainWrapper(this, connectTimeout = connectTimeout, readTimeout = unit.toMillis(timeout.toLong()), writeTimeout = writeTimeout)
    }

    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
        return TimeoutChainWrapper(this, connectTimeout = connectTimeout, readTimeout = readTimeout, writeTimeout = unit.toMillis(timeout.toLong()))
    }
}
