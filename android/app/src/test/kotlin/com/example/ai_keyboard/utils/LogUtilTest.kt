package com.example.ai_keyboard.utils

import android.util.Log
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Unit tests for LogUtil
 * Verifies centralized logging functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LogUtilTest {
    
    @Test
    fun `test debug logging outputs correctly`() {
        // Enable all logs for testing
        ShadowLog.stream = System.out
        
        // When
        LogUtil.d("TestTag", "Test debug message")
        
        // Then - verify log was recorded
        val logs = ShadowLog.getLogsForTag("TestTag")
        assert(logs.isNotEmpty()) { "Expected debug log to be recorded" }
        assert(logs.any { it.msg == "Test debug message" && it.type == Log.DEBUG })
    }
    
    @Test
    fun `test error logging with exception`() {
        ShadowLog.stream = System.out
        val testException = RuntimeException("Test exception")
        
        // When
        LogUtil.e("TestTag", "Test error message", testException)
        
        // Then
        val logs = ShadowLog.getLogsForTag("TestTag")
        assert(logs.isNotEmpty()) { "Expected error log to be recorded" }
        assert(logs.any { it.msg == "Test error message" && it.type == Log.ERROR })
    }
    
    @Test
    fun `test error logging without exception`() {
        ShadowLog.stream = System.out
        
        // When
        LogUtil.e("TestTag", "Test error without exception")
        
        // Then
        val logs = ShadowLog.getLogsForTag("TestTag")
        assert(logs.any { it.msg == "Test error without exception" && it.type == Log.ERROR })
    }
    
    @Test
    fun `test warning logging`() {
        ShadowLog.stream = System.out
        
        // When
        LogUtil.w("TestTag", "Test warning message")
        
        // Then
        val logs = ShadowLog.getLogsForTag("TestTag")
        assert(logs.any { it.msg == "Test warning message" && it.type == Log.WARN })
    }
    
    @Test
    fun `test warning logging with exception`() {
        ShadowLog.stream = System.out
        val testException = IllegalStateException("Test warning exception")
        
        // When
        LogUtil.w("TestTag", "Test warning with exception", testException)
        
        // Then
        val logs = ShadowLog.getLogsForTag("TestTag")
        assert(logs.any { it.msg == "Test warning with exception" && it.type == Log.WARN })
    }
    
    @Test
    fun `test info logging`() {
        ShadowLog.stream = System.out
        
        // When
        LogUtil.i("TestTag", "Test info message")
        
        // Then
        val logs = ShadowLog.getLogsForTag("TestTag")
        assert(logs.any { it.msg == "Test info message" && it.type == Log.INFO })
    }
    
    @Test
    fun `test multiple log calls with different tags`() {
        ShadowLog.stream = System.out
        
        // When
        LogUtil.d("Tag1", "Message 1")
        LogUtil.d("Tag2", "Message 2")
        LogUtil.e("Tag1", "Error 1")
        
        // Then
        val tag1Logs = ShadowLog.getLogsForTag("Tag1")
        val tag2Logs = ShadowLog.getLogsForTag("Tag2")
        
        assert(tag1Logs.size == 2) { "Expected 2 logs for Tag1, got ${tag1Logs.size}" }
        assert(tag2Logs.size == 1) { "Expected 1 log for Tag2, got ${tag2Logs.size}" }
    }
}

