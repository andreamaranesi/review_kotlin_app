package com.project.review.ui.recyclerview_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.project.review.R
import com.project.review.ui.filters.FilterWord

/**
 * adapter of the RecyclerView of the recurring words of the Filters class
 *
 * @see com.project.review.ui.filters.Filters.setPopularWords
 */
class FilterWordAdapter(
    val listener: Actions,
) : ListAdapter<FilterWord, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FilterWord>() {
            override fun areItemsTheSame(oldItem: FilterWord, newItem: FilterWord): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: FilterWord, newItem: FilterWord): Boolean {
                return oldItem.checked == newItem.checked
            }
        }
    }

    interface Actions {
        fun onClick(list: MutableList<FilterWord>)
    }

    class Adapter(view:View) : RecyclerView.ViewHolder(view) {
        val button: Chip = view.findViewById(R.id.word)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.word_wallet_button, parent, false)
        return Adapter(view)
    }


    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val adapter = holder as Adapter
        val filterWord = getItem(position)

        adapter.button.isChecked = filterWord.checked
        adapter.button.text = filterWord.name

        adapter.button.setOnClickListener {
            filterWord.checked = !filterWord.checked
            currentList.firstOrNull { it.name == filterWord.name }?.checked = filterWord.checked
            this.listener.onClick(currentList)
        }


    }


}

