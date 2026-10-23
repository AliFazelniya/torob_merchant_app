package ir.torob.seller.data.repository

import android.content.Context
import android.content.SharedPreferences
import ir.torob.seller.data.model.*
import ir.torob.seller.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("seller_auth_prefs", Context.MODE_PRIVATE)

    private val api = ApiClient.getService(context)

    private val _isLoggedIn = MutableStateFlow(hasToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun hasToken(): Boolean = !prefs.getString("access_token", null).isNullOrEmpty()

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun getShopId(): String = prefs.getString("shop_id", "shop_51d0c1a6edf81ba759726bab") ?: "shop_51d0c1a6edf81ba759726bab"

    fun getSellerName(): String = prefs.getString("full_name", "علی رضایی (فروشگاه موبایل تخت جمشید)") ?: "علی رضایی (فروشگاه موبایل تخت جمشید)"

    fun getPhoneNumber(): String = prefs.getString("phone_number", "09123456789") ?: "09123456789"

    fun getSellerId(): String = prefs.getString("seller_id", "seller_test_001") ?: "seller_test_001"

    suspend fun requestOtp(phone: String): Result<RequestOtpResponse> {
        return try {
            val response = api.requestOtp(RequestOtpRequest(phone))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "خطا در درخواست کد تایید"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(phone: String, code: String): Result<TokenResponse> {
        return try {
            val response = api.verifyOtp(VerifyOtpRequest(phone, code))
            if (response.isSuccessful && response.body() != null) {
                val tokenResp = response.body()!!
                prefs.edit()
                    .putString("access_token", tokenResp.accessToken)
                    .putString("seller_id", tokenResp.seller.sellerId)
                    .putString("shop_id", tokenResp.seller.shopId)
                    .putString("phone_number", tokenResp.seller.phoneNumber)
                    .putString("full_name", tokenResp.seller.fullName)
                    .apply()
                _isLoggedIn.value = true
                Result.success(tokenResp)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "کد تایید اشتباه یا منقضی است"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loginDemoSeller(customPhone: String = "09123456789") {
        prefs.edit()
            .putString("access_token", "token_seller_test_001")
            .putString("seller_id", "seller_test_001")
            .putString("shop_id", "shop_51d0c1a6edf81ba759726bab")
            .putString("phone_number", customPhone)
            .putString("full_name", "علی رضایی (فروشگاه موبایل تخت جمشید)")
            .apply()
        _isLoggedIn.value = true
    }

    fun logout() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
    }
}
