package ru.dyatel.inuyama.screens

import android.content.Context
import android.support.design.widget.TextInputEditText
import android.text.InputType
import android.view.View
import android.view.ViewManager
import android.widget.EditText
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.design.textInputEditText
import org.jetbrains.anko.design.textInputLayout
import org.jetbrains.anko.find
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.textResource
import org.jetbrains.anko.topPadding
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.PreferenceHelper
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.disableSuggestions
import ru.dyatel.inuyama.disableUiExtraction
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.transmission.TransmissionConfiguration

class TransmissionView(context: Context) : BaseScreenView<TransmissionScreen>(context) {

    private companion object {
        val hostViewId = View.generateViewId()
        val portViewId = View.generateViewId()
        val pathViewId = View.generateViewId()
        val usernameViewId = View.generateViewId()
        val passwordViewId = View.generateViewId()
    }

    private val hostView: EditText
    private val portView: EditText
    private val pathView: EditText
    private val usernameView: EditText
    private val passwordView: EditText

    init {
        verticalLayout {
            lparams(width = matchParent, height = matchParent) {
                leftPadding = DIM_LARGE
                rightPadding = DIM_LARGE
                topPadding = DIM_EXTRA_LARGE
                bottomPadding = DIM_EXTRA_LARGE
            }

            createInput {
                id = hostViewId
                hintResource = R.string.hint_transmission_host
            }
            createInput {
                id = portViewId
                hintResource = R.string.hint_transmission_port

                inputType = InputType.TYPE_CLASS_NUMBER
            }
            createInput {
                id = pathViewId
                hintResource = R.string.hint_transmission_path
            }
            createInput {
                id = usernameViewId
                hintResource = R.string.hint_transmission_username
            }
            createInput {
                id = passwordViewId
                hintResource = R.string.hint_transmission_password

                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            tintedButton {
                textResource = R.string.action_save
                setOnClickListener { save() }
            }
        }

        hostView = find(hostViewId)
        portView = find(portViewId)
        pathView = find(pathViewId)
        usernameView = find(usernameViewId)
        passwordView = find(passwordViewId)
    }

    private inline fun ViewManager.createInput(init: TextInputEditText.() -> Unit) {
        textInputLayout {
            lparams(width = matchParent, height = wrapContent) {
                bottomMargin = DIM_LARGE
            }

            textInputEditText {
                disableUiExtraction()
                disableSuggestions()

                init()

                leftPadding = DIM_LARGE
                rightPadding = DIM_LARGE
            }
        }
    }

    fun bindConfiguration(configuration: TransmissionConfiguration) {
        hostView.setText(configuration.host)
        portView.setText(configuration.port.toString())
        pathView.setText(configuration.path)
        usernameView.setText(configuration.username)
        passwordView.setText(configuration.password)
    }

    private fun save() {
        val configuration = TransmissionConfiguration(
                hostView.text.toString(),
                portView.text.toString().toInt(),
                pathView.text.toString(),
                usernameView.text.toString(),
                passwordView.text.toString()
        )
        screen.save(configuration)
    }

}

class TransmissionScreen : Screen<TransmissionView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val preferenceHelper by instance<PreferenceHelper>()

    override fun createView(context: Context) = TransmissionView(context)

    override fun onShow(context: Context) {
        super.onShow(context)
        view.bindConfiguration(preferenceHelper.transmission)
    }

    fun save(configuration: TransmissionConfiguration) {
        preferenceHelper.transmission = configuration
    }

    override fun getTitle(context: Context) = context.getString(R.string.screen_transmission)!!

}