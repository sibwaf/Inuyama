package ru.dyatel.inuyama.screens

import android.content.Context
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class NyaaView(context: Context) : BaseScreenView<NyaaScreen>(context) {

}

class NyaaScreen : Screen<NyaaView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    override fun createView(context: Context) = NyaaView(context)

}
