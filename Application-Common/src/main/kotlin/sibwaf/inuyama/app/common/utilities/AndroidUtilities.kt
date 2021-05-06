package sibwaf.inuyama.app.common.utilities

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

val Activity.supportFragmentManager: FragmentManager
    get() = (this as AppCompatActivity).supportFragmentManager
