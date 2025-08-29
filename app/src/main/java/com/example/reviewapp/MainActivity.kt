package com.example.reviewapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.reviewapp.navigation.AppNavGraph
import com.example.reviewapp.ui.theme.ReviewAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReviewAppTheme {
                val nav = rememberNavController()
                AppNavGraph(nav)
            }
        }
    }
}
