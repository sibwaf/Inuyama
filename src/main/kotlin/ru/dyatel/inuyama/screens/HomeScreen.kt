package ru.dyatel.inuyama.screens

import android.content.Context
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import ru.dyatel.inuyama.R

class HomeView(context: Context) : BaseScreenView<HomeScreen>(context) {

}

class HomeScreen : Screen<HomeView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    override fun createView(context: Context) = HomeView(context)

    override fun getTitle(context: Context) = context.getString(R.string.screen_home)!!
}
