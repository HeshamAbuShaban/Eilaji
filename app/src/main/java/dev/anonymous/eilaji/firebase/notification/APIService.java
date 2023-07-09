package dev.anonymous.eilaji.firebase.notification;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAUelekwo:APA91bEVSD7LASB29h85n5dA2Jhepv-M3rkCAcRV1WEqPv0bf9rYAS-V7UxfZwCcnVDC9BR5_tmklmZyOCTM93oV8Q6S3b17jRDFgnKCW8f9ERDEf6RasfYxc9Urc80xsYm1HFZjJdv9"
    })
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
