package com.example.myapplication.data.repo

import com.example.myapplication.data.model.RecordItem

/**
 * 데이터 소스 인터페이스 및 임시 구현(Fake).
 * 추후 API/DB 등으로 교체하기 쉬우려고 계층 분리.
 */
interface RecordRepository {
    fun getTodayRecords(): List<RecordItem>
}

class FakeRecordRepository : RecordRepository {
    override fun getTodayRecords(): List<RecordItem> = listOf(
        RecordItem(time = "09:05", text = "블라블라블라블라블 ...."),
        RecordItem(time = "14:00", text = "블라블라블라블라블 ....")
    )
}
