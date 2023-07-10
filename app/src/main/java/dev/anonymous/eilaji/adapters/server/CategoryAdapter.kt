package dev.anonymous.eilaji.adapters.server

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.databinding.ItemCategoryBinding
import dev.anonymous.eilaji.models.server.Category
import dev.anonymous.eilaji.utils.GeneralUtils

class CategoryAdapter(
    private var categoryList: ArrayList<Category>,
    private val navToSubListener: (categoryId: String, categoryTitle: String) -> Unit
) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding =
            ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.bind(category, position, categoryList.size, navToSubListener)
    }

    class CategoryViewHolder(private var binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            model: Category,
            position: Int,
            listSize: Int,
            navToSubListener: (categoryId: String, categoryTitle: String) -> Unit
        ) {
            binding.apply {
                if (position == 0 || position == 1) {
                    // margin top first tow item
                    parentCategoryItem.setPadding(0, 60, 0, 0)
                } else if (position == listSize - 2 || position == listSize - 1) {
                    // margin bottom last tow item
                    parentCategoryItem.setPadding(0, 0, 0, 222)
                }

                GeneralUtils.getInstance()
                    .loadImage(model.imageUrl)
                    .into(ivPharmacyDepartment)

                tvPharmacyDepartment.text = model.title

                parentCardCategoryItem.setOnClickListener {
                    navToSubListener(model.id, model.title)
                }
            }
        }
    }

}