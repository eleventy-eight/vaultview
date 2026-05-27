package com.vaultview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vaultview.ui.BrowseViewModel
import com.vaultview.ui.VaultViewApp
import com.vaultview.ui.theme.VaultViewTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BrowseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaultViewTheme {
                VaultViewApp(viewModel = viewModel)
            }
        }
    }
}
