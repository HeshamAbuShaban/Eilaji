package dev.anonymous.eilaji.utils

import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.models.CategoriesPharmaceuticalModel
import dev.anonymous.eilaji.models.MedicineModel
import dev.anonymous.eilaji.models.ModelOnBoarding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.models.RatingsModel

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

    /*val listAds: ArrayList<Ad>
        get() {
            val list = ArrayList<Ad>()
            list.add(
                Ad(
                    R.drawable.temp_ads_image,
                    "حمل الروشتة الآن!"
                )
            )
            list.add(
                Ad(
                    R.drawable.temp_ads_image,
                    "حمل الروشتة الآن!"
                )
            )
            list.add(
                Ad(
                    R.drawable.temp_ads_image,
                    "حمل الروشتة الآن!"
                )
            )
            return list
        }*/

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
                    "DAG57DAG58AD4SG4S",
                    R.drawable.temp_medicine_1,
                    "قطرة مسكنة للآلام",
                    20.0,
                    "الاستخدام: يُدعى أن فيكتروفين يستخدم لعلاج عدة حالات صحية مختلفة. يُزعم أنه يعمل كمسكن للألم، ومضاد للالتهاب، ومضاد للحساسية، ومضاد للقلق.\n" +
                            "التركيبة الكيميائية: طريقة صنع فيكتروفين تحتوي على مزيج من المواد الكيميائية الوهمية، ولا يوجد لها أساس علمي أو أدلة على فاعليتها العلاجية.",
                    RatingsModel(4.5f, 4f, 5f, 4f),
                    arrayListOf(
                        "HSF3G4H5SF7GH9HSF",
                        "NTS4NT56NT7SYN44T",
                        "ERY56STR7ST56TU1F",
                        "JRY45RY9UJ005YUR6"
                    ),
                    true
                )
            )
            list.add(
                MedicineModel(
                    "HSF3G4H5SF7GH9HSF",
                    R.drawable.temp_medicine_2,
                    "مثبط هرمونات",
                    13.0,
                    "الاستخدام: يُدعى أن فيكتروفين يستخدم لعلاج عدة حالات صحية مختلفة. يُزعم أنه يعمل كمسكن للألم، ومضاد للالتهاب، ومضاد للحساسية، ومضاد للقلق.\n" +
                            "\n" +
                            "التركيبة الكيميائية: طريقة صنع فيكتروفين تحتوي على مزيج من المواد الكيميائية الوهمية، ولا يوجد لها أساس علمي أو أدلة على فاعليتها العلاجية.",
                    RatingsModel(4f, 4f, 4f, 4f),
                    arrayListOf(),
                    true
                )
            )
            list.add(
                MedicineModel(
                    "NTS4NT56NT7SYN44T",
                    R.drawable.temp_medicine_3,
                    "قطرة مسكنة للآلام",
                    6.0,
                    "الاستخدام: يُدعى أن فيكتروفين يستخدم لعلاج عدة حالات صحية مختلفة. يُزعم أنه يعمل كمسكن للألم، ومضاد للالتهاب، ومضاد للحساسية، ومضاد للقلق.\n" +
                            "\n" +
                            "التركيبة الكيميائية: طريقة صنع فيكتروفين تحتوي على مزيج من المواد الكيميائية الوهمية، ولا يوجد لها أساس علمي أو أدلة على فاعليتها العلاجية.",
                    RatingsModel(4f, 4f, 4f, 4f),
                    arrayListOf(),
                    true
                )
            )
            list.add(
                MedicineModel(
                    "ERY56STR7ST56TU1F",
                    R.drawable.temp_medicine_4,
                    "قطرة مسكنة للآلام",
                    50.0,
                    "الاستخدام: يُدعى أن فيكتروفين يستخدم لعلاج عدة حالات صحية مختلفة. يُزعم أنه يعمل كمسكن للألم، ومضاد للالتهاب، ومضاد للحساسية، ومضاد للقلق.\n" +
                            "\n" +
                            "التركيبة الكيميائية: طريقة صنع فيكتروفين تحتوي على مزيج من المواد الكيميائية الوهمية، ولا يوجد لها أساس علمي أو أدلة على فاعليتها العلاجية.",
                    RatingsModel(4f, 4f, 4f, 4f),
                    arrayListOf(),
                    true
                )
            )
            return list
        }

    /*val listPharmacyDepartmentsModels: ArrayList<PharmacyDepartmentModel>
        get() {
            val list = ArrayList<PharmacyDepartmentModel>()
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_pills,
                    "الأدوية"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_hand,
                    "عناية بالطفل"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_whey,
                    "مكملات"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_teeth,
                    "الفم والاسنان"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_hair_care,
                    "عناية بالشعر"
                )
            )
            list.add(
                PharmacyDepartmentModel(
                    R.drawable.temp_category_check,
                    "عناية بالبشرة"
                )
            )
            return list
        }*/

    val listPharmaciesModels: ArrayList<Pharmacy>
        get() {
            val list = ArrayList<Pharmacy>()
            list.add(
                Pharmacy(
                    "R.drawable.temp_pharmacy_1",
                    "فتيح",
                    arrayListOf("د.محمد محمود فتيح"),
                    "+970597152714",
                    "الرمال - دوار فتوح - مقابل برج المعادي",
                    31.44927529166395,
                    34.39462522569722
                )
            )
            list.add(
                Pharmacy(
                    "R.drawable.temp_pharmacy_2",
                    "تقى عابدين",
                    arrayListOf("د.تقى عابدين"),
                    "+970568856720",
                    "الشجاعية - مقابل برج وطن",
                    31.450573262978924,
                    34.393475898066065
                )
            )
            list.add(
                Pharmacy(
                    "R.drawable.temp_pharmacy_3",
                    "عبق الشجر",
                    arrayListOf("د.محمد الحمامي"),
                    "+970562005006",
                    "الشجاعية - بجوار مسجد الصحابة",
                    31.448934162124537,
                    34.39408320427861
                )
            )
            list.add(
                Pharmacy(
                    "R.drawable.temp_pharmacy_4",
                    "عبد العزيز محمود حبيب",
                    arrayListOf("د.عبد العزيز محمود حبيب"),
                    "+970598756400",
                    "النصيرات - بجوار ابو دلال مول",
                    31.44747884783767,
                    34.392811840775806
                )
            )
            list.add(
                Pharmacy(
                    "R.drawable.temp_pharmacy_5",
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
