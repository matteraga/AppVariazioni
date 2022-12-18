package me.matteo.appvariazioni.classes.list

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.Icon
import me.matteo.appvariazioni.classes.Type

class ListViewAdapter(context: Context?, items: List<ListViewItem>?) :
    BaseAdapter() {
    private var items: List<ListViewItem>? = null
    private var context: Context? = null

    init {
        this.items = items
        this.context = context
    }

    override fun getCount(): Int {
        return items!!.size
    }

    override fun getItem(position: Int): ListViewItem {
        return items!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, container: ViewGroup): View {
        val inflater = context
            ?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        var convertView: View
        convertView = view ?: inflater.inflate(R.layout.section_name_view, container, false)

        val current = getItem(position)
        when (current.type) {
            Type.SECTION_NAME -> {
                convertView = inflater.inflate(R.layout.section_name_view, container, false)
                convertView.findViewById<TextView>(R.id.sectionName).text = current.title
            }
            Type.NORMAL -> {
                convertView = inflater.inflate(R.layout.list_default_view, container, false)
                convertView.findViewById<TextView>(R.id.boldTitle).text = current.title
                convertView.findViewById<TextView>(R.id.normalSubtitle).text = current.subtitle
                convertView.findViewById<TextView>(R.id.background)
                    .setBackgroundColor(Color.parseColor(current.color))
                val image = when (current.image) {
                    Icon.HISTORY -> R.drawable.history
                    Icon.ENGLISH -> R.drawable.english
                    Icon.PE -> R.drawable.pe
                    Icon.MATH -> R.drawable.math
                    Icon.ITALIAN -> R.drawable.italian
                    Icon.COMPUTER_SCIENCE -> R.drawable.computer_science
                    Icon.NETWORKS -> R.drawable.networks
                    Icon.RELIGION -> R.drawable.religion
                    Icon.TELECOMMUNICATIONS -> R.drawable.telecommunications
                    Icon.TPSIT -> R.drawable.tpsit
                    Icon.ENTRY -> R.drawable.late_entry
                    Icon.EXIT -> R.drawable.early_exit
                    Icon.SUBSTITUTE -> R.drawable.substitution
                    else -> R.drawable.ic_launcher_foreground
                }
                convertView.findViewById<ImageView>(R.id.imageView).setImageResource(image)
            }
            Type.SIMPLE_TEXT_BACKGROUND -> {
                convertView = inflater.inflate(R.layout.simple_text_view, container, false)
                convertView.findViewById<TextView>(R.id.variationText).text = current.title
            }
        }
        return convertView
    }
}