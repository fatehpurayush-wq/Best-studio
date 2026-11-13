package com.beatstudio.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.beatstudio.app.ui.editor.EditorScreen
import com.beatstudio.app.ui.editor.EditorViewModel
import com.beatstudio.app.ui.home.HomeScreen
import com.beatstudio.app.ui.home.HomeViewModel
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class EditorRoute(val projectId: String)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            val homeVm: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeVm,
                onOpenProject = { projectId ->
                    navController.navigate(EditorRoute(projectId))
                }
            )
        }

        composable<EditorRoute> { backStackEntry ->
            val route: EditorRoute = backStackEntry.toRoute()
            val editorVm: EditorViewModel = viewModel()
            EditorScreen(
                projectId = route.projectId,
                viewModel = editorVm,
                onBackToHome = {
                    navController.popBackStack()
                }
            )
        }
    }
}
