package ru.dyatel.inuyama

import ru.dyatel.inuyama.model.Update

abstract class Watcher {

    private val listeners = mutableListOf<() -> Unit>()

    abstract fun checkUpdates(): List<String>
    abstract fun dispatchUpdates(dispatcher: UpdateDispatcher)
    abstract fun listUpdates(): List<Update>

    fun addUpdateListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeUpdateListener(listener: () -> Unit) {
        listeners -= listener
    }

    protected fun notifyListeners() {
        listeners.forEach { it() }
    }

}
