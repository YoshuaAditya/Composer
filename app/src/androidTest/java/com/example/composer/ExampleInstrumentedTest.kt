package com.example.composer

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.composer.data.Chat

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun newChatTest_currentLabelExists() {
        composeTestRule.setContent {
            ExtendedFloatingActionButton(text = { Text(text = "New Chat") }, onClick = { /*TODO*/ })
        }
        composeTestRule.onRoot().printToLog("currentLabelExists")//check in run test logcat

        composeTestRule
            .onNodeWithText("New Chat")
            .assertExists()
    }

    @Test
    fun conversation_currentLabelExists() {
        val list= listOf<Chat>(Chat("author","body"))
        composeTestRule.setContent {
            Conversation(list, rememberLazyListState())
        }
        composeTestRule.onRoot().printToLog("currentLabelExists")//check in run test logcat

        composeTestRule
            .onNodeWithText("body")
            .assertExists()
        composeTestRule
            .onNodeWithText("author")
            .assertExists()
    }


    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.composer", appContext.packageName)
    }
}