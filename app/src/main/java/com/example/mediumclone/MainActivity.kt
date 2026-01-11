package com.example.mediumclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mediumclone.ui.theme.MediumCloneTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediumCloneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val auth = FirebaseAuth.getInstance()
                    
                    // Deep Link Handling
                    var startDestination = if (auth.currentUser != null) "home" else "login"
                    val intent = this@MainActivity.intent
                    val data = intent?.data
                    
                    if (intent?.action == android.content.Intent.ACTION_VIEW && data != null) {
                        if (data.pathSegments.size > 1 && data.pathSegments[0] == "article") {
                            val articleId = data.pathSegments[1]
                            startDestination = "article/$articleId"
                        }
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") { 
                            com.example.mediumclone.ui.screens.auth.LoginScreen(navController = navController)
                        }
                        composable("register") {
                            com.example.mediumclone.ui.screens.auth.RegisterScreen(navController = navController)
                        }
                        composable("home") {
                            com.example.mediumclone.ui.screens.home.HomeScreen(navController = navController)
                        }
                        composable(
                            route = "editor?draftId={draftId}",
                            arguments = listOf(androidx.navigation.navArgument("draftId") { nullable = true })
                        ) {
                            com.example.mediumclone.ui.screens.editor.EditorScreen(navController = navController)
                        }
                        composable("editor/{articleId}") { backStackEntry ->
                            val articleId = backStackEntry.arguments?.getString("articleId")
                            com.example.mediumclone.ui.screens.editor.EditorScreen(
                                navController = navController,
                                articleId = articleId
                            )
                        }
                        composable("article/{articleId}") { backStackEntry ->
                            val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
                            com.example.mediumclone.ui.screens.article.ArticleDetailScreen(
                                articleId = articleId,
                                navController = navController
                            )
                        }
                        composable("profile") {
                            com.example.mediumclone.ui.screens.profile.ProfileScreen(
                                navController = navController,
                                userId = null
                            )
                        }
                        composable("profile/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            com.example.mediumclone.ui.screens.profile.ProfileScreen(
                                navController = navController,
                                userId = userId
                            )
                        }
                        composable("search") {
                            com.example.mediumclone.ui.screens.search.SearchScreen(
                                navController = navController
                            )
                        }
                        composable("search/{tag}") { backStackEntry ->
                            val tag = backStackEntry.arguments?.getString("tag") ?: ""
                            com.example.mediumclone.ui.screens.search.SearchScreen(
                                tag = tag,
                                navController = navController
                            )
                        }
                        composable("saved_articles") {
                            com.example.mediumclone.ui.screens.saved.SavedArticlesScreen(
                                navController = navController
                            )
                        }
                        composable("subscription") {
                            com.example.mediumclone.ui.screens.subscription.SubscriptionScreen(navController = navController)
                        }
                        composable("secure_checkout") {
                            com.example.mediumclone.ui.screens.subscription.SecureCheckoutScreen(
                                navController = navController,
                                onPaymentSuccess = {
                                     // Success handled by VM and Screen pops back
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}