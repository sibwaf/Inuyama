package sibwaf.inuyama.app.common.components

import android.content.Context
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon

class IconTabSelector<T>(context: Context) : TabLayout(context) {

    private var options: List<T> = emptyList()
    var selected: T?
        get() {
            return options.getOrNull(selectedTabPosition)
        }
        set(value) {
            val index = options.indexOf(value)
            if (index < 0) {
                throw NoSuchElementException()
            }

            getTabAt(index)!!.select()
        }

    private var listener: ((T?) -> Unit)? = null

    init {
        addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: Tab) {
                listener?.invoke(selected)
            }

            override fun onTabUnselected(tab: Tab) = Unit
            override fun onTabReselected(tab: Tab) = Unit
        })
    }

    fun bindOptions(options: List<Pair<T, IIcon>>) {
        while (tabCount > options.size) {
            removeTabAt(tabCount - 1)
        }
        while (tabCount < options.size) {
            addTab(newTab())
        }

        val tabs = (0 until tabCount).mapNotNull { getTabAt(it) }
        for ((option, tab) in options.zip(tabs)) {
            val drawable = IconicsDrawable(context)
                .icon(option.second)
                .sizeDp(16)
                .colorRes(com.mikepenz.materialize.R.color.md_dark_primary_text)

            tab.icon = drawable
        }

        this.options = options.map { it.first }
    }

    fun onOptionSelected(listener: (T?) -> Unit) {
        this.listener = listener
    }
}

fun <T> Context.iconTabSelector(init: IconTabSelector<T>.() -> Unit) = IconTabSelector<T>(this).apply(init)

fun <T> ViewGroup.iconTabSelector(init: IconTabSelector<T>.() -> Unit) = context.iconTabSelector(init).also { addView(it) }
