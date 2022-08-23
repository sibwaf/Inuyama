package ru.dyatel.inuyama.screens

import android.content.Context
import android.view.ViewGroup
import android.widget.SearchView
import androidx.annotation.StringRes
import com.wealthfront.magellan.Screen
import com.wealthfront.magellan.ScreenView
import io.objectbox.BoxStore
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
import io.objectbox.reactive.SubscriptionBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.anko.find
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.utilities.subscribeFor
import java.util.concurrent.atomic.AtomicLong

@Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
abstract class InuScreen<V> : Screen<V>(), KodeinAware where V : ViewGroup, V : ScreenView<*> {

    protected val context: Context?
        get() = activity

    final override val kodein by closestKodein { activity!! }

    val boxStore by instance<BoxStore>()
    private val boxStoreObservers = mutableListOf<DataSubscription>()

    private val jobIdProvider = AtomicLong(1)
    private val jobs = mutableMapOf<Long, Job>()

    val searchView by lazy { activity.find<SearchView>(R.id.search) }

    @StringRes
    protected open val titleResource = 0
    protected open val titleText = ""

    protected fun attachDataObserver(subscription: DataSubscription) {
        boxStoreObservers += subscription
    }

    protected inline fun <reified T> observe(
        init: SubscriptionBuilder<Class<T>>.() -> Unit,
        crossinline onUpdate: () -> Unit
    ): DataSubscription {
        return boxStore.subscribeFor<T>()
            .on(AndroidScheduler.mainThread())
            .apply(init)
            .observer { onUpdate() }
            .also { attachDataObserver(it) }
    }

    protected inline fun <reified T> observeChanges(crossinline onUpdate: () -> Unit) = observe<T>({ onlyChanges() }, onUpdate)

    protected fun generateJobId(): Long = jobIdProvider.getAndIncrement()

    protected fun attachJob(job: Job, id: Long = generateJobId()) {
        jobs[id] = job
    }

    protected fun launchJob(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        id: Long = generateJobId(),
        replacing: Boolean = false,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val existing = jobs[id]
        if (existing != null && !existing.isCompleted) {
            if (replacing) {
                existing.cancel()
            } else {
                return existing
            }
        }

        return CoroutineScope(dispatcher)
            .launch(block = block)
            .also { attachJob(it, id) }
    }

    abstract override fun createView(context: Context): V

    override fun onHide(context: Context) {
        for (observer in boxStoreObservers) {
            observer.cancel()
        }
        boxStoreObservers.clear()

        for (job in jobs.values) {
            job.cancel()
        }
        jobs.clear()

        super.onHide(context)
    }

    override fun getTitle(context: Context): String {
        if (titleText.isNotBlank()) {
            return titleText
        }

        if (titleResource != 0) {
            return context.getString(titleResource)
        }

        return ""
    }
}
