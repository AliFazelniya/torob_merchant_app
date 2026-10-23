package ir.torob.seller.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.torob.seller.data.remote.ApiClient
import ir.torob.seller.data.repository.PushRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val serverBaseUrl: String = "http://10.0.2.2:8080/api/",
    val fcmToken: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val pushRepo = PushRepository(context)
    private val prefs = context.getSharedPreferences("seller_auth_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            notificationsEnabled = pushRepo.areNotificationsEnabled(),
            serverBaseUrl = prefs.getString("custom_server_url", "http://10.0.2.2:8080/api/") ?: "http://10.0.2.2:8080/api/",
            fcmToken = pushRepo.getStoredToken()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled, isSaving = true)
        viewModelScope.launch {
            pushRepo.setNotificationsEnabled(enabled)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                message = if (enabled) "دریافت نوتیفیکیشن فعال شد" else "نوتیفیکیشن‌ها غیرفعال شدند"
            )
        }
    }

    fun updateServerUrl(url: String) {
        prefs.edit().putString("custom_server_url", url).apply()
        ApiClient.resetClient()
        _uiState.value = _uiState.value.copy(serverBaseUrl = url, message = "آدرس سرور بروزرسانی شد")
    }
}
