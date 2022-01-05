package ru.dyatel.inuyama.utilities

interface Editor<T> {
    fun fillFrom(data: T)
    fun buildValue(): T
}

interface ChangePublisher<T> {
    fun onChange(listener: (T) -> Unit)
}

interface ListenableEditor<T> : Editor<T>, ChangePublisher<T>

interface PublishListenerHolder<T> : ChangePublisher<T> {
    fun notifyListener(valueProvider: () -> T)
}

class PublishListenerHolderImpl<T> : PublishListenerHolder<T> {

    private var listener: ((T) -> Unit)? = null

    override fun notifyListener(valueProvider: () -> T) {
        listener?.invoke(valueProvider())
    }

    override fun onChange(listener: (T) -> Unit) {
        this.listener = listener
    }
}

fun <T> PublishListenerHolder<T>.withAdditionalListener(listener: (T) -> Unit): PublishListenerHolder<T> {
    return object : PublishListenerHolder<T> by this {
        override fun notifyListener(valueProvider: () -> T) {
            val value = valueProvider()
            this@withAdditionalListener.notifyListener { value }
            listener.invoke(value)
        }
    }
}

interface BoundPublishListenerHolder<T> : ChangePublisher<T> {
    fun notifyListener()
}

fun <T> PublishListenerHolder<T>.withValueProvider(provider: () -> T): BoundPublishListenerHolder<T> {
    return object : BoundPublishListenerHolder<T>, ChangePublisher<T> by this {
        override fun notifyListener() {
            notifyListener(provider)
        }
    }
}

fun <T> PublishListenerHolder<T>.withEditor(editor: Editor<T>) = withValueProvider { editor.buildValue() }

interface BatchingBoundPublishListenerHolder<T> : BoundPublishListenerHolder<T> {
    fun notifyAfterBatch(block: () -> Unit)
}

fun <T> BoundPublishListenerHolder<T>.withBatching(): BatchingBoundPublishListenerHolder<T> {
    return object : BatchingBoundPublishListenerHolder<T>, BoundPublishListenerHolder<T> by this {

        private var isBatchRunning = false

        override fun notifyListener() {
            if (!isBatchRunning) {
                this@withBatching.notifyListener()
            }
        }

        override fun notifyAfterBatch(block: () -> Unit) {
            isBatchRunning = true
            try {
                block()

                isBatchRunning = false
                notifyListener()
            } finally {
                isBatchRunning = false
            }
        }
    }
}
