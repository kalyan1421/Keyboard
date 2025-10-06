package com.example.ai_keyboard.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Unit tests for BaseManager
 * Verifies the abstract base class functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BaseManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockPrefs: SharedPreferences
    
    private lateinit var testManager: TestManager
    
    /**
     * Concrete test implementation of BaseManager
     */
    private inner class TestManager(context: Context) : BaseManager(context) {
        override fun getPreferencesName() = "test_prefs"
        
        fun testLogD(message: String) = logD(message)
        fun testLogE(message: String, throwable: Throwable? = null) = logE(message, throwable)
        
        fun getPrefsPublic() = prefs
    }
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences
        `when`(mockContext.getSharedPreferences("test_prefs", Context.MODE_PRIVATE))
            .thenReturn(mockPrefs)
        
        testManager = TestManager(mockContext)
        
        // Enable log output for testing
        ShadowLog.stream = System.out
    }
    
    @Test
    fun `test getPreferencesName returns correct name`() {
        // When
        val prefsName = testManager.getPreferencesName()
        
        // Then
        assert(prefsName == "test_prefs") {
            "Expected 'test_prefs', got '$prefsName'"
        }
    }
    
    @Test
    fun `test prefs are lazily initialized`() {
        // When
        val prefs = testManager.getPrefsPublic()
        
        // Then
        verify(mockContext).getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        assert(prefs == mockPrefs)
    }
    
    @Test
    fun `test logD uses class name as tag`() {
        // When
        testManager.testLogD("Test debug message")
        
        // Then - verify log was created with class name as tag
        val logs = ShadowLog.getLogsForTag("TestManager")
        assert(logs.isNotEmpty()) { "Expected log with tag 'TestManager'" }
        assert(logs.any { it.msg == "Test debug message" && it.type == Log.DEBUG })
    }
    
    @Test
    fun `test logE without exception`() {
        // When
        testManager.testLogE("Test error message")
        
        // Then
        val logs = ShadowLog.getLogsForTag("TestManager")
        assert(logs.any { it.msg == "Test error message" && it.type == Log.ERROR })
    }
    
    @Test
    fun `test logE with exception`() {
        // Given
        val testException = RuntimeException("Test exception")
        
        // When
        testManager.testLogE("Test error with exception", testException)
        
        // Then
        val logs = ShadowLog.getLogsForTag("TestManager")
        assert(logs.any { 
            it.msg == "Test error with exception" && 
            it.type == Log.ERROR &&
            it.throwable == testException
        })
    }
    
    @Test
    fun `test initialize can be called`() {
        // When/Then - should not throw
        testManager.initialize()
    }
    
    @Test
    fun `test prefs are only initialized once`() {
        // When
        val prefs1 = testManager.getPrefsPublic()
        val prefs2 = testManager.getPrefsPublic()
        
        // Then
        verify(mockContext, times(1)).getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        assert(prefs1 === prefs2) { "Expected same SharedPreferences instance" }
    }
    
    @Test
    fun `test multiple managers with different prefs names`() {
        // Given
        val manager1 = object : BaseManager(mockContext) {
            override fun getPreferencesName() = "prefs_1"
        }
        val manager2 = object : BaseManager(mockContext) {
            override fun getPreferencesName() = "prefs_2"
        }
        
        val mockPrefs1 = mock(SharedPreferences::class.java)
        val mockPrefs2 = mock(SharedPreferences::class.java)
        
        `when`(mockContext.getSharedPreferences("prefs_1", Context.MODE_PRIVATE))
            .thenReturn(mockPrefs1)
        `when`(mockContext.getSharedPreferences("prefs_2", Context.MODE_PRIVATE))
            .thenReturn(mockPrefs2)
        
        // When
        val prefs1 = (manager1 as Any).let { 
            val prefsField = BaseManager::class.java.getDeclaredField("prefs\$delegate")
            prefsField.isAccessible = true
            val lazy = prefsField.get(it) as Lazy<*>
            lazy.value as SharedPreferences
        }
        
        val prefs2 = (manager2 as Any).let {
            val prefsField = BaseManager::class.java.getDeclaredField("prefs\$delegate")
            prefsField.isAccessible = true
            val lazy = prefsField.get(it) as Lazy<*>
            lazy.value as SharedPreferences
        }
        
        // Then
        verify(mockContext).getSharedPreferences("prefs_1", Context.MODE_PRIVATE)
        verify(mockContext).getSharedPreferences("prefs_2", Context.MODE_PRIVATE)
        assert(prefs1 !== prefs2) { "Expected different SharedPreferences instances" }
    }
}

