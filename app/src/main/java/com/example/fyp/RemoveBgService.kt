import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RemoveBgService {

    // API key header is included here
    @Headers("X-Api-Key: pVZFkYwC4NEAcxtMEPw4EiYP") // Your remove.bg API Key
    @Multipart
    @POST("v1.0/removebg")
    fun removeBackground(
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>
}
