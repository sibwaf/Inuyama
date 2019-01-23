package ru.dyatel.inuyama.screens

import android.content.Context
import android.view.ViewGroup
import com.wealthfront.magellan.Screen
import com.wealthfront.magellan.ScreenView
import ru.dyatel.inuyama.utilities.act

@Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
abstract class NavigatableScreen<V> : Screen<V>() where V : ViewGroup, V : ScreenView<*> {

    override fun onShow(context: Context) {
        super.onShow(context)
        act.syncNavigation()
    }

}
