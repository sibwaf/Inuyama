package ru.dyatel.inuyama.screens

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.EditText
import com.wealthfront.magellan.BaseScreenView
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.find
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.textResource
import org.jetbrains.anko.topPadding
import org.jetbrains.anko.verticalLayout
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.components.uniformTextInput
import ru.dyatel.inuyama.transmission.TransmissionConfiguration
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.hideKeyboard

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

    private lateinit var configuration: TransmissionConfiguration

    init {
        verticalLayout {
            lparams(width = matchParent, height = matchParent) {
                leftPadding = DIM_LARGE
                rightPadding = DIM_LARGE
                topPadding = DIM_EXTRA_LARGE
                bottomPadding = DIM_EXTRA_LARGE
            }

            uniformTextInput {
                id = hostViewId
                hintResource = R.string.hint_host
            }
            uniformTextInput {
                id = portViewId
                hintResource = R.string.hint_port

                inputType = InputType.TYPE_CLASS_NUMBER
            }
            uniformTextInput {
                id = pathViewId
                hintResource = R.string.hint_transmission_path
            }
            uniformTextInput {
                id = usernameViewId
                hintResource = R.string.hint_transmission_username
            }
            uniformTextInput {
                id = passwordViewId
                hintResource = R.string.hint_transmission_password

                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            tintedButton {
                textResource = R.string.action_save
                setOnClickListener {
                    screen.activity.hideKeyboard()
                    save()
                }
            }
        }

        hostView = find(hostViewId)
        portView = find(portViewId)
        pathView = find(pathViewId)
        usernameView = find(usernameViewId)
        passwordView = find(passwordViewId)
    }

    fun bindConfiguration(configuration: TransmissionConfiguration) {
        this.configuration = configuration

        hostView.setText(configuration.host)
        portView.setText(configuration.port.toString())
        pathView.setText(configuration.path)
        usernameView.setText(configuration.username)
        passwordView.setText(configuration.password)
    }

    private fun save() {
        with(configuration) {
            host = hostView.text.toString()
            port = portView.text.toString().toInt()
            path = pathView.text.toString()
            username = usernameView.text.toString()
            password = passwordView.text.toString()
        }
        screen.save(configuration)
    }

}

class TransmissionScreen : NavigatableScreen<TransmissionView>(), KodeinAware {

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