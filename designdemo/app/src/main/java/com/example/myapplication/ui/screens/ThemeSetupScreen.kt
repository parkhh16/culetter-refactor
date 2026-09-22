package com.example.myapplication.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.VideoBackground
import com.example.myapplication.ui.viewmodel.ThemeSetupViewModel
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.delay
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSetupScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    hazeState: HazeState
) {
    val viewModel: ThemeSetupViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // 성공 시 홈으로 이동 (GET 요청으로 자동 처리됨)
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Log.d("ThemeSetupScreen", "isSuccess = true 감지됨")
            delay(1000) // 1초 대기하여 성공 메시지를 볼 수 있게 함
            Log.d("ThemeSetupScreen", "홈으로 이동 시작")
            onSuccess()
            Log.d("ThemeSetupScreen", "onSuccess() 호출 완료")
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // 상단 바
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0x99592813)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "테마 설정",
                    color = Color(0x99592813),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(20.dp))
            
            // 스크롤 가능한 콘텐츠
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 테마명 입력
                ThemeInputSection(
                    title = "테마명",
                    value = uiState.theme,
                    onValueChange = viewModel::updateTheme,
                    placeholder = "예: 특화프로젝트 화이팅"
                )
                
                // 색상 선택
                ColorSelectionSection(
                    selectedColor = uiState.selectedColor,
                    onColorSelected = viewModel::updateColor
                )
                
                // 종료일 선택
                DateSelectionSection(
                    selectedDate = uiState.endDate,
                    onDateSelected = viewModel::updateEndDate
                )
                
                // 에러 메시지
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            // 생성 버튼
            Button(
                onClick = viewModel::createTheme,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x99592813)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (uiState.isSuccess) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0x99592813)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "테마 생성 완료!",
                        color = Color(0x99592813),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "테마 생성",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeInputSection(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color(0x99592813),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0x99592813),
                    unfocusedTextColor = Color(0x99592813),
                    focusedBorderColor = Color(0x99592813),
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true
            )
        }
    }
}

@Composable
private fun ColorSelectionSection(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    val pastelColors = listOf(
        "#E3F2FD" to "파랑",
        "#FCE4EC" to "핑크", 
        "#F3E5F5" to "보라",
        "#E8F5E8" to "초록",
        "#FFF8E1" to "노랑"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "색상 선택",
                color = Color(0x99592813),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                pastelColors.forEach { (colorHex, colorName) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .clickable { onColorSelected(colorHex) }
                                .then(
                                if (selectedColor == colorHex) {
                                    Modifier.border(
                                        width = 3.dp,
                                        color = Color.Black,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                                )
                        )
                        Text(
                            text = colorName,
                            color = Color(0x99592813),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSelectionSection(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "종료일",
                color = Color(0x99592813),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = selectedDate?.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")) ?: "",
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text("종료일을 선택해주세요", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0x99592813),
                        unfocusedTextColor = Color(0x99592813),
                        focusedBorderColor = Color(0x99592813),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Spacer(Modifier.width(8.dp))
                
                Button(
                    onClick = { 
                        showDatePicker(context, selectedDate, onDateSelected)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x99592813)
                    )
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "달력 열기",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun showDatePicker(
    context: Context,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    try {
        val calendar = Calendar.getInstance()
        
        // 현재 선택된 날짜가 있으면 해당 날짜로 설정
        selectedDate?.let { date ->
            calendar.set(date.year, date.monthValue - 1, date.dayOfMonth)
        }
        
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                try {
                    val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    onDateSelected(selectedDate)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // 오늘 이후의 날짜만 선택 가능하도록 설정
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        
        datePickerDialog.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
