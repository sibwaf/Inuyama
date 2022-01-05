package sibwaf.inuyama.app.common.components

import android.content.Context
import com.google.android.material.floatingactionbutton.FloatingActionButton

inline fun Context.floatingActionButton(init: FloatingActionButton.() -> Unit) = FloatingActionButton(this).apply(init)
