package sibwaf.inuyama.app.common

import android.content.Context
import com.mikepenz.iconics.typeface.IIcon
import com.wealthfront.magellan.Screen

// TODO: WTF? should be an interface
abstract class ModuleScreenProvider {
    abstract fun getIcon(): IIcon
    abstract fun getTitle(context: Context): String
    abstract fun getScreenClass(): Class<out Screen<*>>
}
