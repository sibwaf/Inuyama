package ru.dyatel.inuyama.screens

import android.content.Context
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen

class RutrackerView(context: Context) : BaseScreenView<RutrackerScreen>(context) {

}

class RutrackerScreen : Screen<RutrackerView>() {

    override fun createView(context: Context) = RutrackerView(context)



}
