package dev.anonymous.eilaji

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
}