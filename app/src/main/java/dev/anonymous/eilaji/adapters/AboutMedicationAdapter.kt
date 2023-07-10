package dev.anonymous.eilaji.adapters

import android.app.Activity
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.PagerAdapter
import dev.anonymous.eilaji.databinding.ItemAlternativeMedicinesBinding
import dev.anonymous.eilaji.databinding.ItemDetilesBinding
import dev.anonymous.eilaji.databinding.ItemRatingBinding
import dev.anonymous.eilaji.models.MedicineModel
import dev.anonymous.eilaji.utils.DummyData

class AboutMedicationAdapter(
    private val medicineModel: MedicineModel,
    private val activity: Activity
) : PagerAdapter() {


    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return when (position) {
            0 -> {
                val binding: ItemAlternativeMedicinesBinding =
                    ItemAlternativeMedicinesBinding.inflate(
                        LayoutInflater.from(activity),
                        container,
                        false
                    )

                binding.recyclerAlternativeMedicines.setHasFixedSize(false)
                val layoutManager = LinearLayoutManager(
                    activity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                binding.recyclerAlternativeMedicines.layoutManager = layoutManager
                val adapter = MedicinesAdapter(ArrayList())
                binding.recyclerAlternativeMedicines.adapter = adapter

                Thread {
                    SystemClock.sleep(1500)
                    activity.runOnUiThread {
                        val list: ArrayList<MedicineModel> = ArrayList()
                        for (id in medicineModel.alternativeMedicinesId) {
                            for (model in DummyData.listMedicineModels) {
                                if (model.id == id) {
                                    list.add(model)
                                    break
                                }
                            }
                        }
                        binding.progressAlternativeMedicines.visibility = View.GONE
//                        adapter.setListMedicines(list)
                    }
                }.start()

                container.addView(binding.root)
                binding.root
            }

            1 -> {
                val binding: ItemRatingBinding =
                    ItemRatingBinding.inflate(LayoutInflater.from(activity), container, false)

                binding.ratingBarPrice.rating = medicineModel.ratings.priceRate
                binding.ratingBarSpeedOfRecovery.rating = medicineModel.ratings.speedOfRecoveryRate
                binding.ratingBarMedicineEfficacy.rating = medicineModel.ratings.medicineEfficacyRate
                binding.ratingBarTasteOfMedicine.rating = medicineModel.ratings.tasteOfMedicineRate

                container.addView(binding.root)
                binding.root
            }

            else -> {
                val binding: ItemDetilesBinding =
                    ItemDetilesBinding.inflate(LayoutInflater.from(activity), container, false)

                binding.tvDetailsMedicines.text = medicineModel.details

                container.addView(binding.root)
                binding.root
            }
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return 3
    }
}