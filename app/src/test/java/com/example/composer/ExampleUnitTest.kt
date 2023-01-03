package com.example.composer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.asLiveData
import com.example.composer.data.Chat
import com.example.composer.data.ChatDatabase
import com.example.composer.data.ChatRepository
import com.example.composer.retrofit.ApiInterface
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import javax.inject.Inject


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@HiltAndroidTest
//IllegalStateException: No instrumentation registered! Must run under a registering instrumentation.
//need to depend robolectric and add this two lines
@Config(application = HiltTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Inject
    lateinit var chatDatabase: ChatDatabase

    @Inject
    lateinit var apiInterface: ApiInterface

    @Inject
    lateinit var chatRepository: ChatRepository

    @Before
    @Throws(Exception::class)
    fun init() {
        ShadowLog.stream = System.out // Redirect Logcat to console
        hiltRule.inject()
    }

    @Test
    fun testObjects() {
        assertNotNull(chatDatabase)
        assertNotNull(apiInterface)
        assertNotNull(chatRepository)
    }

    @Test
    fun testInsertChats() {
        val mainViewModel = MainViewModel(chatRepository, apiInterface)
        val chat = Chat("author", "body")
        mainViewModel.insert(chat)
        mainViewModel.chats=chatRepository.getChats().asLiveData()
        assertEquals("author", mainViewModel.chats.value?.get(0)?.author)
        //seems like unit testing database/dao/livedata has its issues, for now leave the code just for reference
    }

    @Test
    fun robolectric() {
        val mainViewModel = MainViewModel(chatRepository, apiInterface)
        composeTestRule.setContent { MainActivityContent(mainViewModel, MainActivity()) }
        composeTestRule.onNodeWithText("New Chat").assertExists()
        composeTestRule.onNodeWithText("Delete Chat").assertExists()
        //"SDK 33 Main Thread @coroutine#3" java.lang.IllegalStateException: WorkManager is not initialized properly.  You have explicitly disabled WorkManagerInitializer in your manifest, have not manually called WorkManager#initialize at this point, and your Application does not implement Configuration.Provider.
    }
}