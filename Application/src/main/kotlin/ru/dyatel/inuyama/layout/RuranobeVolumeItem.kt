package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.find
import org.jetbrains.anko.imageView
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_RURANOBE_VOLUME
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RURANOBE_COVER_ASPECT_RATIO
import ru.dyatel.inuyama.RURANOBE_COVER_SIZE
import ru.dyatel.inuyama.layout.components.uniformTextView
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.utilities.prettyTime
import ru.sibwaf.inuyama.common.utilities.asDate

class RuranobeVolumeItem(val volume: RuranobeVolume) : AbstractItem<RuranobeVolumeItem, RuranobeVolumeItem.ViewHolder>() {

    private companion object {
        val coverViewId = View.generateViewId()
        val titleViewId = View.generateViewId()
        val statusViewId = View.generateViewId()
        val lastUpdateViewId = View.generateViewId()
    }

    init {
        withIdentifier(volume.id)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<RuranobeVolumeItem>(view) {

        private val coverView = view.find<ImageView>(coverViewId)
        private val titleView = view.find<TextView>(titleViewId)
        private val statusView = view.find<TextView>(statusViewId)
        private val lastUpdateView = view.find<TextView>(lastUpdateViewId)

        private val context = view.context

        private val glide = Glide.with(view)

        override fun unbindView(item: RuranobeVolumeItem) {
            glide.clear(coverView)
            titleView.text = null
            statusView.text = null
            lastUpdateView.text = null
        }

        override fun bindView(item: RuranobeVolumeItem, payloads: MutableList<Any>?) {
            glide.load(item.volume.coverUrl).into(coverView)

            titleView.text = item.volume.title
            statusView.text = when (item.volume.status) {
                "no_eng" -> context.getString(R.string.ruranobe_status_no_eng)
                "done" -> context.getString(R.string.ruranobe_status_done)
                "translating" -> context.getString(R.string.ruranobe_status_translating)
                "proofread" -> context.getString(R.string.ruranobe_status_proofread)
                "decor" -> context.getString(R.string.ruranobe_status_decor)
                "queue" -> context.getString(R.string.ruranobe_status_queue)
                "external_done" -> context.getString(R.string.ruranobe_status_external_done)
                "external_dropped" -> context.getString(R.string.ruranobe_status_external_dropped)
                else -> item.volume.status
            }

            val lastUpdateText = item.volume.updateDatetime
                    ?.let { prettyTime.format(it.asDate) }
                    ?: context.getString(R.string.const_never)
            lastUpdateView.text = context.getString(R.string.label_last_update, lastUpdateText)
        }
    }

    override fun getType() = ITEM_TYPE_RURANOBE_VOLUME

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.cardView {
            lparams(width = matchParent, height = wrapContent) {
                margin = DIM_LARGE
            }

            linearLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                val coverWidth = DIM_EXTRA_LARGE * RURANOBE_COVER_SIZE
                val coverHeight = (coverWidth / RURANOBE_COVER_ASPECT_RATIO).toInt()
                imageView {
                    id = coverViewId
                }.lparams(width = coverWidth, height = coverHeight) {
                    gravity = Gravity.CENTER_VERTICAL
                }

                verticalLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        leftMargin = DIM_LARGE
                    }

                    uniformTextView {
                        id = titleViewId
                        gravity = Gravity.CENTER_HORIZONTAL
                    }.lparams(width = matchParent) {
                        bottomMargin = DIM_LARGE
                    }

                    uniformTextView { id = statusViewId }
                    uniformTextView { id = lastUpdateViewId }
                }
            }
        }
    }
}