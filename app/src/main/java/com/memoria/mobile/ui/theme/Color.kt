package com.memoria.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// MemorIA Palette — Medisafe-inspired Blue, replicating D:\DEV\MemorIA
// Primary royal blue family
val BluePrimary = Color(0xFF2563EB)
val BluePrimaryDark = Color(0xFF1E40AF)
val BluePrimaryLight = Color(0xFFDBEAFE)
val BlueSecondary = Color(0xFF60A5FA)

// Feedback & status colors matching web variables (--success-color, --danger-color, --warning-color)
val GreenOk = Color(0xFF10B981) // Web: #10b981 (--success-color)
val RedMiss = Color(0xFFEF4444)  // Web: #ef4444 (--danger-color)
val Snooze = Color(0xFFF59E0B)   // Web: #f59e0b (--warning-color)
val Amber = Color(0xFFF59E0B)    // Web: #f59e0b (--warning-color)

// Neutral surfaces matching web variables (--bg-secondary, --bg-primary)
val SurfaceLight = Color(0xFFF9FAFB) // Web: #f9fafb (--bg-secondary)
val SurfaceDark = Color(0xFF0F172A)  // Web: #0f172a (dark mode / slate-900)
val TextPrimary = Color(0xFF1F2937)  // Web: #1f2937 (--text-primary)
val TextSecondary = Color(0xFF6B7280)// Web: #6b7280 (--text-secondary)
val BorderLight = Color(0xFFE5E7EB)  // Web: #e5e7eb (--border-color)

// Backward compatibility aliases
val Teal = BluePrimary
val TealDark = BluePrimaryDark
val TealLight = BluePrimaryLight
