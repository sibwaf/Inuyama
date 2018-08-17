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
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.find
import org.jetbrains.anko.imageView
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.view
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_RURANOBE_PROJECT
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RURANOBE_COVER_ASPECT_RATIO
import ru.dyatel.inuyama.RURANOBE_COVER_SIZE
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.utilities.asDate
import ru.dyatel.inuyama.utilities.hideIf
import ru.dyatel.inuyama.utilities.prettyTime

class RuranobeProjectItem(
        val project: RuranobeProject
) : AbstractItem<RuranobeProjectItem, RuranobeProjectItem.ViewHolder>() {

    private companion object {
        val worksMarkerId = View.generateViewId()
        val coverViewId = View.generateViewId()

        val titleViewId = View.generateViewId()
        val authorViewId = View.generateViewId()
        val statusViewId = View.generateViewId()

        val lastUpdateViewId = View.generateViewId()
    }

    init {
        withIdentifier(project.id)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<RuranobeProjectItem>(view) {

        private val worksMarker = view.find<View>(worksMarkerId)
        private val coverView = view.find<ImageView>(coverViewId)

        private val titleView = view.find<TextView>(titleViewId)
        private val authorView = view.find<TextView>(authorViewId)
        private val statusView = view.find<TextView>(statusViewId)

        private val lastUpdateView = view.find<TextView>(lastUpdateViewId)

        private val context = view.context

        private val glide = Glide.with(view)

        override fun unbindView(item: RuranobeProjectItem) {
            glide.clear(coverView)
            titleView.text = null
            authorView.text = null
            statusView.text = null
            lastUpdateView.text = null
        }

        override fun bindView(item: RuranobeProjectItem, payloads: MutableList<Any>?) {
            worksMarker.backgroundColorResource =
                    if (item.project.works) R.color.ruranobe_project_works else R.color.ruranobe_project_main

            val coverUrl = item.project.volumes.firstOrNull()?.coverUrl
            if (coverUrl != null) {
                glide.load(coverUrl).into(coverView)
            }

            titleView.text = item.project.title

            authorView.text = item.project.author
            authorView.hideIf { it.text.isBlank() }

            val volumes = item.project.volumes

            val volumeCount = volumes.count()
            statusView.text = context.resources.getQuantityString(
                    R.plurals.ruranobe_volumes, volumeCount,
                    volumeCount, item.project.status
            )

            val lastUpdateText = volumes.mapNotNull { it.updateDatetime }.max()
                    ?.let { prettyTime.format(it.asDate) }
                    ?: context.getString(R.string.const_never)
            lastUpdateView.text = context.getString(R.string.label_last_update, lastUpdateText)
        }
    }

    override fun getType() = ITEM_TYPE_RURANOBE_PROJECT

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

                view {
                    id = worksMarkerId
                }.lparams(width = DIM_MEDIUM, height = matchParent) {
                    rightMargin = DIM_LARGE
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

                    uniformTextView { id = authorViewId }
                    uniformTextView { id = statusViewId }
                    uniformTextView { id = lastUpdateViewId }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as RuranobeProjectItem
        return project == other.project
    }

    override fun hashCode() = project.hashCode()

}
