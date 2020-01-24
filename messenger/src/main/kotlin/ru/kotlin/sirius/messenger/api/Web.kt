package ru.kotlin.sirius.messenger.api


import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import retrofit2.Call
import retrofit2.Callback
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

// https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md

// retrofit2 async Call to suspend fun interoperability
suspend fun <T> Call<T>.invokeAsync(): T = doInvokeAsync()

// also: do something useful on invoking coroutine thread while HTTP request is being processed
suspend fun <T> Call<T>.invokeAsyncAndAlso(also: () -> Unit = {}): T = doInvokeAsync(also)

open class SuspendAwareThrowable(message: String? = null) : Throwable(message) {
    private val _cause = AtomicReference<Throwable>(null)

    override var cause: Throwable?
        get() = _cause.get()
        set(value) {
            if (!_cause.compareAndSet(null, value)) {
                throw IllegalStateException("Cannot modify cause because it's already set (see cause of this one)", _cause.get())
            }
        }
}
class CallNotExecutedException(message: String?) : SuspendAwareThrowable(message)
internal class CallFailedException(message: String?, code: Int) : CancellationException("$message, code: $code")

internal suspend fun <T> Call<T>.doInvokeAsync(meanwhile: () -> Unit = {}): T {
    val channel = Channel<T>(capacity = UNLIMITED)
    val callSite = CallNotExecutedException(request().url().toString()) // remember invocation point
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: retrofit2.Response<T>) {
            val result = response.body()
            if (result != null && call.isExecuted && response.isSuccessful) {
                channel.offer(result)
            } else {
                channel.cancel(CallFailedException(response.errorBody()?.string(), response.code()))
            }
        }
        override fun onFailure(call: Call<T>, t: Throwable) {
            channel.cancel(t as? CancellationException ?: CancellationException("invokeAsync failed: ${t.message}", t))
        }
    })
    meanwhile()
    return try {
        channel.receive()
    } catch (t: Throwable) {
        callSite.cause = t
        throw callSite
    }
}

@JvmName(name = "invokeAsyncParallel")
fun <T> List<Call<T>>.invokeAsync(): Channel<CallResult<T>> = doInvokeAsync()

@JvmName(name = "invokeAsyncAndAlsoParallel")
fun <T> List<Call<T>>.invokeAsyncAndAlso(also: () -> Unit = {}): Channel<CallResult<T>> = doInvokeAsync(also)

@JvmName(name = "doInvokeAsyncParallel")
fun <T> List<Call<T>>.doInvokeAsync(meanwhile: () -> Unit = {}): Channel<CallResult<T>> {
    val channel = Channel<CallResult<T>>(capacity = UNLIMITED)
    if (size == 0) {
        channel.close()
        return channel
    }
    val countDown = AtomicInteger(size)
    forEach {
        it.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: retrofit2.Response<T>) {
                val result = response.body()
                if (result != null && call.isExecuted && response.isSuccessful) {
                    channel.offer(CallResult(call = call, data = result))
                } else {
                    channel.offer(CallResult(
                            call = call,
                            error = CallFailedException(response.errorBody()?.string(), response.code())
                    ))
                }
                if (countDown.decrementAndGet() == 0) {
                    channel.close()
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                channel.offer(CallResult(
                        call = call,
                        error = t
                ))
                if (countDown.decrementAndGet() == 0) {
                    channel.close()
                }
            }
        })
    }
    meanwhile()
    return channel
}

object NoError: Throwable()

data class CallResult<T>(
        val call: Call<T>,
        val data: T? = null,
        val error: Throwable = NoError
)
