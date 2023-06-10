package dev.anonymous.eilaji.utils

import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.models.AdModel
import dev.anonymous.eilaji.models.CategoriesPharmaceuticalModel
import dev.anonymous.eilaji.models.MedicineModel
import dev.anonymous.eilaji.models.ModelOnBoarding
import dev.anonymous.eilaji.models.PharmacyDepartmentModel
import dev.anonymous.eilaji.models.PharmacyModel

object DummyData {
    val listModelOnBoarding: ArrayList<ModelOnBoarding>
        get() {
            val list = ArrayList<ModelOnBoarding>()
            list.add(
                ModelOnBoarding(
                    R.drawable.on_boarding_1,
                    "هذا النص هو مثال لنص يمكن أن يستبدل في نفس المساحة، لقد تم توليد هذا النص من مولد النص "
                )
            )
            list.add(
                ModelOnBoarding(
                    R.drawable.on_boarding_2,
                    "هذا النص هو مثال لنص يمكن أن يستبدل في نفس المساحة، لقد تم توليد هذا النص من مولد النص "
                )
            )
            list.add(
                ModelOnBoarding(
                    R.drawable.on_boarding_3,
                    "هذا النص هو مثال لنص يمكن أن يستبدل في نفس المساحة، لقد تم توليد هذا النص من مولد النص "
                )
            )
            return list
        }

    val listAdModels: ArrayList<AdModel>
        get() {
            val list = ArrayList<AdModel>()
            list.add(
                AdModel(
                    R.drawable.temp_ads_image,
                    "حمل الروشتة الآن!"
                )
            )
            list.add(
                AdModel(
                    R.drawable.temp_ads_image,
                    "حمل الروشتة الآن!"
                )
            )
            list.add(
                AdModel(
                    R.drawable.temp_ads_image,
                    "حمل الروشتة الآن!"
                )
            )
            return list
        }

    val listCategoriesPharmaceuticalModels: ArrayList<CategoriesPharmaceuticalModel>
        get() {
            val list = ArrayList<CategoriesPharmaceuticalModel>()
            list.add(
                CategoriesPharmaceuticalModel(
                    R.drawable.temp_pharm_1,
                    "عيون"
                )
            )
            list.add(
                CategoriesPharmaceuticalModel(
                    R.drawable.temp_pharm_2,
                    "أذن"
                )
            )
            list.add(
                CategoriesPharmaceuticalModel(
                    R.drawable.temp_pharm_3,
                    "فطريات"
                )
            )
            list.add(
                CategoriesPharmaceuticalModel(
                    R.drawable.temp_pharm_4,
                    "التهابات"
                )
            )
            list.add(
                CategoriesPharmaceuticalModel(
                    R.drawable.temp_pharm_1,
                    "عيون"
                )
            )
            list.add(
                CategoriesPharmaceuticalModel(
                    R.drawable.temp_pharm_2,
                    "أذن"
                )
            )
            list.add(
                CategoriesPharmaceuticalModel(
                    R.drawable.temp_pharm_3,
                    "فطريات"
                )
            )
            list.add(
                CategoriesPharmaceuticalModel(
                    R.drawable.temp_pharm_4,
                    "التهابات"
                )
            )
            return list
        }

    val listMedicineModels: ArrayList<MedicineModel>
        get() {
            val list = ArrayList<MedicineModel>()
            list.add(
                MedicineModel(
                    R.drawable.temp_medicine_1,
                    "قطرة مسكنة للآلام",
                    20.0,
                    true
                )
            )
            list.add(
                MedicineModel(
                    R.drawable.temp_medicine_2,
                    "مثبط هرمونات",
                    7.0,
                    false
                )
            )
            list.add(
                MedicineModel(
                    R.drawable.temp_medicine_3,
                    "قطرة مسكنة للآلام",
                    53.0,
                    false
                )
            )
            list.add(
                MedicineModel(
                    R.drawable.temp_medicine_1,
                    "قطرة مسكنة للآلام",
                    20.0,
                    true
                )
            )
            list.add(
                MedicineModel(
                    R.drawable.temp_medicine_2,
                    "مثبط هرمونات",
                    7.0,
                    false
                )
            )
            return list
        }

    val listPharmacyDepartmentsModels: ArrayList<PharmacyDepartmentModel>
        get() {
            val list = ArrayList<PharmacyDepartmentModel>()
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_1,
                    "الأدوية"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_2,
                    "عناية بالطفل"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_3,
                    "مكملات"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_1,
                    "الفم والاسنان"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_2,
                    "عناية بالشعر"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_3,
                    "عناية بالبشرة"
                )
            )
            return list
        }

    val listPharmaciesModels: ArrayList<PharmacyModel>
        get() {
            val list = ArrayList<PharmacyModel>()
            list.add(
                PharmacyModel(
                    R.drawable.temp_pharmacy_1,
                    "فتيح",
                    arrayListOf("د.محمد محمود فتيح"),
                    "+970597152714",
                    "الرمال - دوار فتوح - مقابل برج المعادي",
                    31.44927529166395,
                    34.39462522569722
                )
            )
            list.add(
                PharmacyModel(
                    R.drawable.temp_pharmacy_2,
                    "تقى عابدين",
                    arrayListOf("د.تقى عابدين"),
                    "+970568856720",
                    "الشجاعية - مقابل برج وطن",
                    31.450573262978924,
                    34.393475898066065
                )
            )
            list.add(
                PharmacyModel(
                    R.drawable.temp_pharmacy_3,
                    "عبق الشجر",
                    arrayListOf("د.محمد الحمامي"),
                    "+970562005006",
                    "الشجاعية - بجوار مسجد الصحابة",
                    31.448934162124537,
                    34.39408320427861
                )
            )
            list.add(
                PharmacyModel(
                    R.drawable.temp_pharmacy_4,
                    "عبد العزيز محمود حبيب",
                    arrayListOf("د.عبد العزيز محمود حبيب"),
                    "+970598756400",
                    "النصيرات - بجوار ابو دلال مول",
                    31.44747884783767,
                    34.392811840775806
                )
            )
            list.add(
                PharmacyModel(
                    R.drawable.temp_pharmacy_5,
                    "الرمادي",
                    arrayListOf("د.خليل الكحلوت"),
                    "+9705993236841",
                    "النصيرات - السوق - منتصف شارع القسام",
                    31.447918798916653,
                    34.39636771288554
                )
            )
            return list
        }
}
