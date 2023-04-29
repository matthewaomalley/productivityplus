package lewisu.mattomalley.finalproject

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions

import android.graphics.Paint
import androidx.core.content.ContextCompat


class Adapter(
    options: FirebaseRecyclerOptions<ListItem>,
    val clickListener: (Int) -> Unit
) : FirebaseRecyclerAdapter<ListItem, Adapter.listItemViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): listItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return listItemViewHolder(layoutInflater, parent)
    }

    override fun onBindViewHolder(holder: listItemViewHolder, position: Int, model: ListItem) {
        holder.bind(model)
        holder.itemView.setOnClickListener { clickListener(position) }
    }

    inner class listItemViewHolder(inflater: LayoutInflater, parent: ViewGroup?) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.list_item, parent, false)) {
        private val listItemTitleView: TextView
        private val priorityRatingView: TextView

        init {
            listItemTitleView = itemView.findViewById(R.id.title_text_view)
            priorityRatingView = itemView.findViewById(R.id.priority_text_view)
        }

        fun bind(listItem: ListItem) {
            listItemTitleView.text = listItem.title

            // assign priority text based on number of stars
            when (listItem.priority.toDouble()) {
                in 0.5..1.5 -> {
                    priorityRatingView.text = "Low"
                    priorityRatingView.setTextColor(ContextCompat.getColor(itemView.context, R.color.ratingYellow))
                }
                in 2.0..3.5 -> {
                    priorityRatingView.text = "Med"
                    priorityRatingView.setTextColor(ContextCompat.getColor(itemView.context, R.color.ratingOrange))
                }
                in 4.0..5.0 -> {
                    priorityRatingView.text = "High"
                    priorityRatingView.setTextColor(ContextCompat.getColor(itemView.context, R.color.ratingRed))
                }
                else -> {
                    priorityRatingView.text = ""
                    priorityRatingView.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                }
            }

            // change the style of title and priority based on completion
            if (listItem.complete) {
                listItemTitleView.paintFlags = listItemTitleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                priorityRatingView.paintFlags = priorityRatingView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                listItemTitleView.alpha = 0.5f
                priorityRatingView.alpha = 0.5f
            } else {
                listItemTitleView.paintFlags = listItemTitleView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                priorityRatingView.paintFlags = priorityRatingView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                listItemTitleView.alpha = 1.0f
                priorityRatingView.alpha = 1.0f
            }
        }
    }
}