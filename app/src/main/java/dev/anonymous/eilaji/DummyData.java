package dev.anonymous.eilaji;

import java.util.ArrayList;

public class DummyData {

    public static ArrayList<ModelOnBoarding> getListModelOnBoarding() {
        ArrayList<ModelOnBoarding> list = new ArrayList<>();
        list.add(new ModelOnBoarding(
                R.drawable.on_boarding_1,
                "هذا النص هو مثال لنص يمكن أن يستبدل في نفس المساحة، لقد تم توليد هذا النص من مولد النص "
        ));
        list.add(new ModelOnBoarding(
                R.drawable.on_boarding_2,
                "هذا النص هو مثال لنص يمكن أن يستبدل في نفس المساحة، لقد تم توليد هذا النص من مولد النص "
        ));
        list.add(new ModelOnBoarding(
                R.drawable.on_boarding_3,
                "هذا النص هو مثال لنص يمكن أن يستبدل في نفس المساحة، لقد تم توليد هذا النص من مولد النص "
        ));
        return list;
    }
}
