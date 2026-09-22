package com.example.myapplication.data.state

import com.example.myapplication.data.model.Record

/**
 * 기록 상세 화면을 위한 전역 상태
 */
object RecordState {
    private var _selectedRecord: Record? = null
    
    fun setSelectedRecord(record: Record) {
        _selectedRecord = record
    }
    
    fun getSelectedRecord(): Record? = _selectedRecord
    
    fun clearSelectedRecord() {
        _selectedRecord = null
    }
}
