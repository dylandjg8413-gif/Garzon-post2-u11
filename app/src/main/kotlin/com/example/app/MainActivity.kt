package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app.core.ui.theme.NotesAppTheme
import com.example.app.feature.notes.NotesScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity unica que aloja un NavHost de Compose. El grafo de navegacion
 * conecta el feature :notes con el resto de la app inyectando la lambda
 * onNavigateToDetail. El feature NO conoce el NavController.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesAppTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "notes") {
                    composable("notes") {
                        NotesScreen(
                            onNavigateToDetail = { noteId ->
                                navController.navigate("notes/$noteId")
                            },
                        )
                    }
                    composable("notes/{noteId}") { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getString("noteId").orEmpty()
                        NoteDetailPlaceholder(noteId = noteId)
                    }
                }
            }
        }
    }
}
