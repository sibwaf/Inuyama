package ru.dyatel.inuyama

import androidx.activity.result.ActivityResultCaller
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class QrReader(activity: ActivityResultCaller) {

    private val launcher = activity.registerForActivityResult(ScanContract()) {
        continuation.get().resume(it.contents)
    }

    private val lock = Mutex()
    private val continuation = AtomicReference<Continuation<String?>>()

    suspend fun readQrCode(): String? {
        return lock.withLock {
            val result = suspendCoroutine<String?> {
                if (!continuation.compareAndSet(null, it)) {
                    it.resumeWithException(RuntimeException("Another QR scan is already in process"))
                } else {
                    val options = ScanOptions().apply {
                        setOrientationLocked(false)
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    }

                    launcher.launch(options)
                }
            }

            continuation.set(null)

            result
        }
    }
}
