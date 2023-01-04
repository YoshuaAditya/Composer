package com.example.composer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
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
import org.robolectric.Robolectric
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
@Config(application = HiltTestApplication::class,instrumentedPackages = ["androidx.loader.content"])
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

    lateinit var mainViewModel :MainViewModel

    @Before
    @Throws(Exception::class)
    fun init() {
        ShadowLog.stream = System.out // Redirect Logcat to console
        hiltRule.inject()
        mainViewModel= MainViewModel(chatRepository, apiInterface)
    }

    @Test
    fun testObjects() {
        assertNotNull(chatDatabase)
        assertNotNull(apiInterface)
        assertNotNull(chatRepository)
    }

    @Test
    fun step1_testInsertChats() {
        //To properly wait for retrofit, we separate retrofit methods in one test case before asserting.
        //This way retrofit will finish first then next test case is executed
        val chat = Chat("author", "body")
        //For some reason this also affects robolectric viewModel below, maybe because of using dagger hilt?
        mainViewModel.insert(chat)
        mainViewModel.getComment("100")
        //It will show livedata Cannot invoke setValue on a background thread error, but it's not used for testing purpose
    }

    @Test
    fun step2_performClicksOnActivityResumed() {
        //Reminder to make ChatDatabase worker into comment to workaround WorkManager not initialized properly error
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            controller.setup() // Moves Activity to RESUMED state
            val activity: MainActivity = controller.get()
            composeTestRule.onRoot().printToLog("robolectric")
            composeTestRule.onNodeWithText("Leone_Fay@orrin.com").assertExists()
            composeTestRule.onNodeWithText("author").assertExists()
            composeTestRule.onNodeWithText("Delete Chat").performClick()
            composeTestRule.onRoot().printToLog("robolectric")
            composeTestRule.onNodeWithText("author").assertDoesNotExist()
            assertEquals(1,activity.mainViewModel.chats.value?.size)
        }
    }
}