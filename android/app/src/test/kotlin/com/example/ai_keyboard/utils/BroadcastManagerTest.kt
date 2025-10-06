package com.example.ai_keyboard.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BroadcastManager
 * Verifies broadcast sending functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BroadcastManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.packageName).thenReturn("com.example.ai_keyboard")
    }
    
    @Test
    fun `test sendToKeyboard sends broadcast with correct action`() {
        // Given
        val action = "com.example.ai_keyboard.ACTION_TEST"
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        
        // When
        BroadcastManager.sendToKeyboard(mockContext, action)
        
        // Then
        verify(mockContext).sendBroadcast(intentCaptor.capture())
        
        val capturedIntent = intentCaptor.value
        assert(capturedIntent.action == action) {
            "Expected action '$action', got '${capturedIntent.action}'"
        }
    }
    
    @Test
    fun `test sendToKeyboard sets package name`() {
        // Given
        val action = "com.example.ai_keyboard.ACTION_TEST"
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        
        // When
        BroadcastManager.sendToKeyboard(mockContext, action)
        
        // Then
        verify(mockContext).sendBroadcast(intentCaptor.capture())
        
        val capturedIntent = intentCaptor.value
        assert(capturedIntent.`package` == "com.example.ai_keyboard") {
            "Expected package 'com.example.ai_keyboard', got '${capturedIntent.`package`}'"
        }
    }
    
    @Test
    fun `test sendToKeyboard with extras`() {
        // Given
        val action = "com.example.ai_keyboard.ACTION_THEME_CHANGED"
        val extras = Bundle().apply {
            putString("themeId", "dark")
            putString("themeName", "Dark Theme")
        }
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        
        // When
        BroadcastManager.sendToKeyboard(mockContext, action, extras)
        
        // Then
        verify(mockContext).sendBroadcast(intentCaptor.capture())
        
        val capturedIntent = intentCaptor.value
        assert(capturedIntent.getStringExtra("themeId") == "dark")
        assert(capturedIntent.getStringExtra("themeName") == "Dark Theme")
    }
    
    @Test
    fun `test sendToKeyboard without extras`() {
        // Given
        val action = "com.example.ai_keyboard.ACTION_SIMPLE"
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        
        // When
        BroadcastManager.sendToKeyboard(mockContext, action, null)
        
        // Then
        verify(mockContext).sendBroadcast(intentCaptor.capture())
        
        val capturedIntent = intentCaptor.value
        assert(capturedIntent.extras == null || capturedIntent.extras!!.isEmpty)
    }
    
    @Test
    fun `test multiple broadcasts are sent separately`() {
        // Given
        val action1 = "com.example.ai_keyboard.ACTION_1"
        val action2 = "com.example.ai_keyboard.ACTION_2"
        
        // When
        BroadcastManager.sendToKeyboard(mockContext, action1)
        BroadcastManager.sendToKeyboard(mockContext, action2)
        
        // Then
        verify(mockContext, times(2)).sendBroadcast(any(Intent::class.java))
    }
}

