package ru.dyatel.inuyama.screens

import android.content.Context
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import org.kodein.di.android.closestKodein

class MainScreenView(context: Context) : BaseScreenView<MainScreen>(context) {

}

class MainScreen : Screen<MainScreenView>() {

    private val kodein by closestKodein { activity }

    override fun createView(context: Context) = MainScreenView(context)
}
