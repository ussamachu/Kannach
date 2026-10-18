package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kannach", appName)
  }

  @Test
  @Config(qualifiers = "ar")
  fun `read arabic launcher string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("كُنَّاش", appName)
  }

  @Test
  fun `test draft storage and retrieval`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = KannachRepository(context)
    val testKey = "2026-10-16"
    val testDraft = EntryDraft(text = "مسودة اختبار لطيفة", color = "#5aa25f")
    repo.saveDraft(testKey, testDraft)
    val retrieved = repo.getDraft(testKey)
    assertEquals("مسودة اختبار لطيفة", retrieved?.text)
    repo.clearDraft(testKey)
    assertEquals(null, repo.getDraft(testKey))
  }
}
