package com.watnapp.buddhawajana.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.watnapp.buddhawajana.core.designsystem.R

@OptIn(ExperimentalTextApi::class)
private fun notoThai(weight: Int) = Font(
    resId = R.font.noto_sans_thai,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
val NotoSansThai = FontFamily(
    notoThai(400),
    notoThai(500),
    notoThai(700),
)

// M3 type scale retargeted to Noto Sans Thai with raised line height for Thai diacritics.
val BuddhawajanaTypography = Typography().run {
    copy(
        bodyLarge = bodyLarge.copy(fontFamily = NotoSansThai, lineHeight = 26.sp),
        bodyMedium = bodyMedium.copy(fontFamily = NotoSansThai, lineHeight = 24.sp),
        titleLarge = titleLarge.copy(fontFamily = NotoSansThai),
        titleMedium = titleMedium.copy(fontFamily = NotoSansThai),
        labelLarge = labelLarge.copy(fontFamily = NotoSansThai),
        headlineSmall = headlineSmall.copy(fontFamily = NotoSansThai),
    )
}
