package com.watnapp.buddhawajana.ui

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Settings → About. Mirrors the iOS app's Phase 4.1 settings: an About section with
 * Version + Build rows only. No toggles — the app hardcodes light mode + Thai locale.
 * Version/build are read from PackageManager at runtime (no BuildConfig flag needed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val info: AppVersionInfo = remember {
        readVersionInfo(context.packageManager, context.packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ตั้งค่า") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "เกี่ยวกับแอป",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
            ListItem(
                headlineContent = { Text("เวอร์ชัน") },
                trailingContent = { Text(info.versionName) },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("หมายเลขบิลด์") },
                trailingContent = { Text(info.versionCode) },
            )
        }
    }
}

private data class AppVersionInfo(val versionName: String, val versionCode: String)

private fun readVersionInfo(pm: PackageManager, packageName: String): AppVersionInfo {
    val pkg: PackageInfo = pm.getPackageInfo(packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pkg.longVersionCode.toString()
    } else {
        @Suppress("DEPRECATION")
        pkg.versionCode.toString()
    }
    return AppVersionInfo(versionName = pkg.versionName ?: "—", versionCode = code)
}
