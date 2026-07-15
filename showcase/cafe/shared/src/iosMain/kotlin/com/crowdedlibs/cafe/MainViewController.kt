package com.crowdedlibs.cafe

import androidx.compose.ui.window.ComposeUIViewController
import com.crowdedlibs.cafe.ui.CafeApp
import platform.UIKit.UIViewController

@Suppress("unused", "FunctionName") // Called from Swift (iosApp/ContentView.swift).
fun MainViewController(): UIViewController = ComposeUIViewController {
    // SwiftUI owns the full-screen iOS host. Applying Compose safe-area padding
    // here as well creates a visibly oversized double inset.
    CafeApp(applySafeAreaInsets = false)
}
