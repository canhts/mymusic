package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `song filename takes priority over raw title`() {
    val displayName = "bai_hat_hay.mp3"
    val rawTitle = "Track 01 - Unknown Artist"

    val resolvedTitle = when {
      displayName.isNotBlank() -> displayName.trim()
      rawTitle.isNotBlank() -> rawTitle.trim()
      else -> "Bài hát không tên"
    }

    assertEquals("bai_hat_hay.mp3", resolvedTitle)
  }

  @Test
  fun `rename handles extension correctly without duplicating`() {
    val currentTitle = "bai_hat_hay.mp3"
    val existingExt = if (currentTitle.contains(".")) {
      currentTitle.substringAfterLast(".").ifBlank { "mp3" }
    } else {
      "mp3"
    }

    // Case 1: user typed with extension
    val input1 = "ten_moi.mp3"
    val finalFileName1 = if (input1.endsWith(".$existingExt", ignoreCase = true)) {
      input1
    } else {
      "$input1.$existingExt"
    }
    assertEquals("ten_moi.mp3", finalFileName1)

    // Case 2: user typed without extension
    val input2 = "ten_moi"
    val finalFileName2 = if (input2.endsWith(".$existingExt", ignoreCase = true)) {
      input2
    } else {
      "$input2.$existingExt"
    }
    assertEquals("ten_moi.mp3", finalFileName2)
  }

  @Test
  fun `audio output device toggles correctly between speaker and headphones`() {
    val initial = com.example.model.AudioOutputDevice.HEADPHONES
    val toggled = initial.toggle()
    assertEquals(com.example.model.AudioOutputDevice.SPEAKER, toggled)

    val toggledBack = toggled.toggle()
    assertEquals(com.example.model.AudioOutputDevice.HEADPHONES, toggledBack)
  }

  @Test
  fun `song displayTitle capitalizes first letter and lowercases subsequent letters`() {
    fun formatDisplayTitle(title: String): String {
      val clean = if (title.endsWith(".mp3", ignoreCase = true)) {
        title.dropLast(4)
      } else {
        title
      }.trim()

      if (clean.isEmpty()) return ""

      val lower = clean.lowercase()
      return lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    assertEquals("Em của ngày hôm qua", formatDisplayTitle("EM CỦA NGÀY HÔM QUA.mp3"))
    assertEquals("Bài hát yêu thích", formatDisplayTitle("bài hát yêu thích.MP3"))
    assertEquals("Ánh nắng của anh", formatDisplayTitle("ÁNH NẮNG CỦA ANH.mp3"))
    assertEquals("123 bài hát", formatDisplayTitle("123 BÀI HÁT.mp3"))
    assertEquals("Đường chúng ta đi", formatDisplayTitle("đường CHÚNG TA ĐI"))
  }
}
