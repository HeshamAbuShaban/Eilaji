package dev.anonymous.eilaji.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ItemSubCategoryBinding
import dev.anonymous.eilaji.models.server.SubCategory
import dev.anonymous.eilaji.utils.GeneralUtils

class SubCategoriesAdapter(
    private var listSubCategoriesAdapter: ArrayList<SubCategory>,
    private val onSelect: ((String) -> Unit)? = null
) : RecyclerView.Adapter<SubCategoriesAdapter.SubCategoriesViewHolder>() {

    private var lastItemSelected: Int = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubCategoriesViewHolder {
        val binding =
            ItemSubCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return SubCategoriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubCategoriesViewHolder, position: Int) {
        val listModels = listSubCategoriesAdapter[position]
        holder.bind(listModels, lastItemSelected == position) {
            val lastSelected = lastItemSelected
            val newPos = holder.bindingAdapterPosition
            if (newPos == -1) return@bind
            lastItemSelected = newPos
            notifyItemChanged(lastSelected)
            notifyItemChanged(newPos)
            onSelect?.invoke(listSubCategoriesAdapter[newPos].id ?: "")
        }
    }

    override fun getItemCount(): Int {
        return listSubCategoriesAdapter.size
    }

    class SubCategoriesViewHolder(
        private var binding: ItemSubCategoryBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private val context: Context = binding.root.context

        fun bind(
            model: SubCategory,
            itemIsSelected: Boolean,
            onSelected: () -> Unit
        ) {
            binding.apply {
                if (model.imageUrl.isNullOrBlank()) {
                    ivSubCategories.setImageResource(R.drawable.temp_category_pills)
                } else {
                    GeneralUtils.getInstance().loadImage(model.imageUrl).error(R.drawable.temp_category_pills).into(ivSubCategories)
                }
                tvSubCategories.text = model.title

                if (itemIsSelected) {
                    val alphaGray: Int = ContextCompat.getColor(context, R.color.alpha_gray)
                    parentView.setBackgroundColor(alphaGray)
                } else {
                    val transparent: Int =
                        ContextCompat.getColor(context, android.R.color.transparent)
                    parentView.setBackgroundColor(transparent)
                }

                parentView.setOnClickListener {
                    // عند النقر على العنصر نحدده اذا لم يكن محدد
                    if (!itemIsSelected) {
                        onSelected()
                    }
                }
            }
        }
    }
}