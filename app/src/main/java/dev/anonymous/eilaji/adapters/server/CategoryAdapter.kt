package dev.anonymous.eilaji.adapters.server

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ItemCategoryBinding
import dev.anonymous.eilaji.network.CategoryDto
import dev.anonymous.eilaji.utils.GeneralUtils

class CategoryAdapter(
    private var categoryList: List<CategoryDto>,
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
            model: CategoryDto,
            position: Int,
            listSize: Int,
            navToSubListener: (categoryId: String, categoryTitle: String) -> Unit
        ) {
            binding.apply {
                if (position == 0 || position == 1) {
                    parentCategoryItem.setPadding(0, 60, 0, 0)
                } else if (position == listSize - 2 || position == listSize - 1) {
                    parentCategoryItem.setPadding(0, 0, 0, 222)
                }

                val fallbackRes = when (model.nameEn.lowercase()) {
                    "pain relievers" -> R.drawable.temp_medicine_1
                    "antibiotics" -> R.drawable.temp_medicine_2
                    "vitamins & supplements", "vitamins" -> R.drawable.temp_medicine_3
                    "skin care" -> R.drawable.temp_medicine_4
                    "cold & flu", "cold" -> R.drawable.temp_category_pills
                    "digestive health", "digestive" -> R.drawable.temp_medicine_2
                    else -> R.drawable.temp_category_pills
                }
                if (model.iconUrl.isNullOrBlank() || model.iconUrl == "/images/categories/default.png") {
                    ivPharmacyDepartment.setImageResource(fallbackRes)
                } else {
                    GeneralUtils.getInstance()
                        .loadImage(model.iconUrl ?: "")
                        .error(fallbackRes)
                        .into(ivPharmacyDepartment)
                }

                tvPharmacyDepartment.text = model.nameEn.ifBlank { model.nameAr }

                parentCardCategoryItem.setOnClickListener {
                    navToSubListener(model.id, model.nameEn)
                }
            }
        }
    }

}
