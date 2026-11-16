package com.example.ui.theme

/**
 * Chế độ giao diện Sáng / Tối của ứng dụng:
 * - [SYSTEM]: Tự động đồng bộ theo cài đặt Sáng/Tối của hệ thống thiết bị.
 * - [LIGHT]: Luôn dùng giao diện Sáng.
 * - [DARK]: Luôn dùng giao diện Tối.
 */
enum class ThemeMode(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }

    companion object {
        fun fromId(id: String?): ThemeMode {
            return entries.firstOrNull { it.id == id } ?: SYSTEM
        }
    }
}
