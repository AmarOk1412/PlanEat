package com.planeat.planeat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.compose.AppTheme
import com.google.accompanist.adaptive.calculateDisplayFeatures

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setContent {
            AppTheme(dynamicColor=false, darkTheme=false) {
                val windowSize = calculateWindowSizeClass(this)

                PlanEatApp(
                    windowSize = windowSize,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
fun PlanEatAppPreview() {
    AppTheme(dynamicColor=false, darkTheme=false) {
        PlanEatApp(
            windowSize = WindowSizeClass.calculateFromSize(DpSize(400.dp, 900.dp)),
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, widthDp = 700, heightDp = 500)
@Composable
fun PlanEatAppPreviewTablet() {
    AppTheme(dynamicColor=false, darkTheme=false) {
        PlanEatApp(
            windowSize = WindowSizeClass.calculateFromSize(DpSize(700.dp, 500.dp)),
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, widthDp = 500, heightDp = 700)
@Composable
fun PlanEatAppPreviewTabletPortrait() {
    AppTheme(dynamicColor=false, darkTheme=false) {
        PlanEatApp(
            windowSize = WindowSizeClass.calculateFromSize(DpSize(500.dp, 700.dp)),
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, widthDp = 1100, heightDp = 600)
@Composable
fun PlanEatAppPreviewDesktop() {
    AppTheme(dynamicColor=false, darkTheme=false) {
        PlanEatApp(
            windowSize = WindowSizeClass.calculateFromSize(DpSize(1100.dp, 600.dp)),
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, widthDp = 600, heightDp = 1100)
@Composable
fun PlanEatAppPreviewDesktopPortrait() {
    AppTheme(dynamicColor=false, darkTheme=false) {
        PlanEatApp(
            windowSize = WindowSizeClass.calculateFromSize(DpSize(600.dp, 1100.dp)),
        )
    }
}
