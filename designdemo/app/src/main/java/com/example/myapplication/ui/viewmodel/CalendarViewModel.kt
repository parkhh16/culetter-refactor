package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.CalendarDetailData
import com.example.myapplication.data.model.CalendarEntry
import com.example.myapplication.data.model.RecordDetail
import com.example.myapplication.data.repository.CalendarRepository
import com.example.myapplication.utils.FirebaseTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * 달력 화면 ViewModel
 */
class CalendarViewModel : ViewModel() {
    
    private val calendarRepository = CalendarRepository()
    private val firebaseTokenManager = FirebaseTokenManager()
    
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    fun loadCalendarEntries(yearMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Firebase 토큰을 가져올 수 없습니다. 로그인이 필요합니다."
                )
                return@launch
            }
            
            calendarRepository.getCalendarEntries(token, yearMonth.year, yearMonth.monthValue)
                .onSuccess { response ->
                    // 날짜별 색상 매핑 생성
                    val badgeColors = response.result.associate { entry ->
                        LocalDate.parse(entry.entryDate) to entry.color
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        calendarEntries = response.result,
                        badgeDates = badgeColors.keys,
                        badgeColors = badgeColors,
                        progress = response.progress,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "데이터를 불러오는 중 오류가 발생했습니다."
                    )
                }
        }
    }
    
    fun refresh(yearMonth: YearMonth) {
        loadCalendarEntries(yearMonth)
    }
    
    fun getEntryForDate(date: LocalDate): CalendarEntry? {
        return _uiState.value.calendarEntries.find { entry ->
            LocalDate.parse(entry.entryDate) == date
        }
    }
    
    fun loadCalendarDetail(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetail = true, detailError = null)
            
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    isLoadingDetail = false,
                    detailError = "Firebase 토큰을 가져올 수 없습니다. 로그인이 필요합니다."
                )
                return@launch
            }
            
            val dateString = date.toString() // "2025-09-18" 형식
            calendarRepository.getCalendarDetail(token, dateString)
                .onSuccess { detailData ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetail = false,
                        calendarDetail = detailData,
                        detailError = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetail = false,
                        detailError = exception.message ?: "상세 데이터를 불러오는 중 오류가 발생했습니다."
                    )
                }
        }
    }
    
    fun clearDetail() {
        _uiState.value = _uiState.value.copy(
            calendarDetail = null,
            detailError = null
        )
    }
}

data class CalendarUiState(
    val isLoading: Boolean = false,
    val calendarEntries: List<CalendarEntry> = emptyList(),
    val badgeDates: Set<LocalDate> = emptySet(),
    val badgeColors: Map<LocalDate, String> = emptyMap(), // 날짜별 색상 매핑
    val progress: Int = 0, // 0-100 진행률
    val error: String? = null,
    val isLoadingDetail: Boolean = false,
    val calendarDetail: CalendarDetailData? = null,
    val detailError: String? = null
)
