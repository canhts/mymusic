package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import java.util.Locale

/**
 * Ngôn ngữ hiển thị của ứng dụng:
 * - [VI]: Tiếng Việt (Việt Nam (VN))
 * - [EN]: Tiếng Anh (English (US))
 */
enum class AppLanguage(
    val id: String,
    val languageTag: String,
    val shortCode: String,
    val displayName: String
) {
    VI(
        id = "vi",
        languageTag = "vi-VN",
        shortCode = "VN",
        displayName = "Việt Nam (VN)"
    ),
    EN(
        id = "en",
        languageTag = "en-US",
        shortCode = "US",
        displayName = "English (US)"
    );

    fun toLocale(): Locale = Locale.forLanguageTag(languageTag)

    companion object {
        val DEFAULT = VI

        fun fromId(id: String?): AppLanguage {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}

/**
 * Bọc [Context] để cung cấp [Resources] đã áp dụng [Locale] mục tiêu
 * trong khi vẫn giữ nguyên chuỗi `baseContext` trỏ tới [Activity] gốc.
 */
internal class LocalizedContextWrapper(
    base: Context,
    private val localizedResources: Resources
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
    override fun getAssets(): AssetManager = localizedResources.assets
}

internal tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
