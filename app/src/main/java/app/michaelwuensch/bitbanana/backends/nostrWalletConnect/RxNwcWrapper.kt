package app.michaelwuensch.bitbanana.backends.nostrWalletConnect

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

object RxNwcWrapper {

    /**
     * @param call    A suspend function that performs the actual network or I/O work (e.g. `client.getInfo()`).
     * @param mapper  A function to map the result [R] to a new type [T].
     *
     * @return        A Single that, upon subscription, runs the suspend function in a coroutine,
     *                maps the result, and emits it to the Single observer.
     */
    fun <R, T : Any> makeRxCall(
        call: suspend () -> R,
        mapper: (R) -> T
    ): Single<T> {
        return Single.create { emitter: SingleEmitter<T> ->

            // Launch a coroutine in IO context
            val job = GlobalScope.launch(Dispatchers.IO) {
                try {
                    val result = call()
                    if (!emitter.isDisposed) {
                        val mapped = mapper(result) // map the result from type R to type T
                        emitter.onSuccess(mapped)
                    }
                } catch (e: CancellationException) {
                    // If the coroutine is cancelled, you may choose to do nothing
                    // or call emitter.onError() if desired.
                } catch (e: Exception) {
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                }
            }

            // If the Single is disposed, cancel the coroutine job
            emitter.setCancellable { job.cancel() }
        }
    }
}