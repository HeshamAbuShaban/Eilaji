package dev.anonymous.eilaji.utils

import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.models.AdModel
import dev.anonymous.eilaji.models.CategoriesPharmaceuticalModel
import dev.anonymous.eilaji.models.MedicineModel
import dev.anonymous.eilaji.models.ModelOnBoarding
import dev.anonymous.eilaji.models.PharmacyDepartmentModel
import dev.anonymous.eilaji.models.PharmacyModel
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

    val listPharmacyDepartmentsModels: ArrayList<PharmacyDepartmentModel>
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
        }

    val listPharmaciesModels: ArrayList<PharmacyModel>
        get() {
            val list = ArrayList<PharmacyModel>()
            list.add(
                PharmacyModel(
                    "adfgdfgfdgg7dsf7g6df",
                    "https://firebasestorage.googleapis.com/v0/b/eilaji-9b01b.appspot.com/o/v9IN7O0myxRoeKfUuaDdgTrJJ1n1%2FPharmaciesImages%2F359d464b-6ea2-4056-bcfe-37cb62512abb.jpg?alt=media&token=be6a8417-b9c4-4d52-b449-86af68c87a2c",
                    "فتيح",
                    "+970597152714",
                    "الرمال - دوار فتوح - مقابل برج المعادي",
                    31.44927529166395,
                    34.39462522569722,
                    "cHvTb0OtQsWROdx5QxL-oV:APA91bEzVhhR3kw03tsZf1L7vaFVHJR-5OEsFtHJvuMoU7FVTbVfuvVPCnTpth9sCpT41VR2aQv-DiPQ3U8pv-RR0ujV1etY-otlCxDzwPl8yz6zab8nxXtPKxrQTWleLm1lgzbLw-Rf"
                )
            )
            list.add(
                PharmacyModel(
                    "adfgdfg345fdg7dsf7g6df",
                    "https://firebasestorage.googleapis.com/v0/b/eilaji-9b01b.appspot.com/o/v9IN7O0myxRoeKfUuaDdgTrJJ1n1%2FPharmaciesImages%2F359d464b-6ea2-4056-bcfe-37cb62512abb.jpg?alt=media&token=be6a8417-b9c4-4d52-b449-86af68c87a2c",
                    "تقى عابدين",
                    "+970568856720",
                    "الشجاعية - مقابل برج وطن",
                    31.450573262978924,
                    34.393475898066065,
                    "cJ7SP3IeSnagT4EczukGPm:APA91bG6vpAyuoXDQDLLqnTiWyXc3Lw0z2oiXvP1XD6FN4pttQXvkQq70qrui4umlY8BfyM_1aq74pykevcGTLWthTFQKroq2KUHgKiXJG0uZ58oN2_w_LyzOq3bHU_ZeTisUImafUmT"
                )
            )
            list.add(
                PharmacyModel(
                    "adfgdfgfdg7dsf34t7g6df",
                    "https://firebasestorage.googleapis.com/v0/b/eilaji-9b01b.appspot.com/o/v9IN7O0myxRoeKfUuaDdgTrJJ1n1%2FPharmaciesImages%2F359d464b-6ea2-4056-bcfe-37cb62512abb.jpg?alt=media&token=be6a8417-b9c4-4d52-b449-86af68c87a2c",
                    "عبق الشجر",
                    "+970562005006",
                    "الشجاعية - بجوار مسجد الصحابة",
                    31.448934162124537,
                    34.39408320427861,
                    "cJ7SP3IeSnagT4EczukGPm:APA91bG6vpAyuoXDQDLLqnTiWyXc3Lw0z2oiXvP1XD6FN4pttQXvkQq70qrui4umlY8BfyM_1aq74pykevcGTLWthTFQKroq2KUHgKiXJG0uZ58oN2_w_LyzOq3bHU_ZeTisUImafUmT"
                )
            )
            list.add(
                PharmacyModel(
                    "adfgdfgfdg7dsf7g6df",
                    "https://firebasestorage.googleapis.com/v0/b/eilaji-9b01b.appspot.com/o/v9IN7O0myxRoeKfUuaDdgTrJJ1n1%2FPharmaciesImages%2F359d464b-6ea2-4056-bcfe-37cb62512abb.jpg?alt=media&token=be6a8417-b9c4-4d52-b449-86af68c87a2c",
                    "عبد العزيز محمود حبيب",
                    "+970598756400",
                    "النصيرات - بجوار ابو دلال مول",
                    31.44747884783767,
                    34.392811840775806,
                    "cJ7SP3IeSnagT4EczukGPm:APA91bG6vpAyuoXDQDLLqnTiWyXc3Lw0z2oiXvP1XD6FN4pttQXvkQq70qrui4umlY8BfyM_1aq74pykevcGTLWthTFQKroq2KUHgKiXJG0uZ58oN2_w_LyzOq3bHU_ZeTisUImafUmT"
                )
            )
            list.add(
                PharmacyModel(
                    "adfgdfgfdg7d34tsf7g6df",
                    "https://firebasestorage.googleapis.com/v0/b/eilaji-9b01b.appspot.com/o/v9IN7O0myxRoeKfUuaDdgTrJJ1n1%2FPharmaciesImages%2F359d464b-6ea2-4056-bcfe-37cb62512abb.jpg?alt=media&token=be6a8417-b9c4-4d52-b449-86af68c87a2c",
                    "الرمادي",
                    "+9705993236841",
                    "النصيرات - السوق - منتصف شارع القسام",
                    31.447918798916653,
                    34.39636771288554,
                    "cJ7SP3IeSnagT4EczukGPm:APA91bG6vpAyuoXDQDLLqnTiWyXc3Lw0z2oiXvP1XD6FN4pttQXvkQq70qrui4umlY8BfyM_1aq74pykevcGTLWthTFQKroq2KUHgKiXJG0uZ58oN2_w_LyzOq3bHU_ZeTisUImafUmT"
                )
            )
            return list
        }
}
