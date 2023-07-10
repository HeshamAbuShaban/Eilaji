package dev.anonymous.eilaji.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ItemSubCategoryBinding
import dev.anonymous.eilaji.models.SubCategory
import dev.anonymous.eilaji.utils.GeneralUtils

class SubCategoriesAdapter(private var listSubCategoriesAdapter: ArrayList<SubCategory>) :
    RecyclerView.Adapter<SubCategoriesAdapter.SubCategoriesViewHolder>() {

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

        // نرسل للعنصر اذا كان تم تحديده او لا
        holder.bind(listModels, lastItemSelected == position) {
            // نحفظ موقع اخر عنصر تم تحديده قبل ان نحدثه
            val lastSelected = lastItemSelected

            // نغير موقع اخر عنصر تم تحديده حتى يصبح العنصر الاخير غير محدد عند تحديثه
            lastItemSelected = holder.absoluteAdapterPosition

            // نحدث العنصر السابق حتى يخفي التحديد
            notifyItemChanged(lastSelected)

            // نحدث العنصر الذي تم تحديده
            notifyItemChanged(position)
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
                GeneralUtils.getInstance().loadImage(model.imageUrl).into(ivSubCategories)
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