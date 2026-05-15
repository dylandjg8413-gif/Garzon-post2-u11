package com.example.app.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app.feature.notes.components.NotesList
import kotlinx.coroutines.launch

/**
 * Pantalla principal del feature de notas.
 *
 * Patrones aplicados:
 *
 *  1. uiState se recolecta con collectAsStateWithLifecycle, NO con collectAsState.
 *     Asi la suscripcion se pausa cuando la pantalla esta en background y se
 *     reanuda al volver, evitando trabajo innecesario.
 *
 *  2. events (SharedFlow) se procesan dentro de un LaunchedEffect(Unit). Como
 *     los SharedFlow no replayan por defecto, cada evento se entrega una sola
 *     vez al unico colector activo. La key Unit garantiza que el colector
 *     vive lo que dura la composicion.
 *
 *  3. La navegacion se inyecta como lambda (onNavigateToDetail). El feature
 *     NO conoce ni Navigation Compose ni el grafo del :app, lo cual lo hace
 *     reusable y testable sin un NavHost real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }

    // Procesamiento de eventos one-shot: navegacion y snackbars.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesEvent.NavigateToDetail -> onNavigateToDetail(event.noteId)
                NotesEvent.NoteDeleted -> scope.launch {
                    snackbarHostState.showSnackbar("Nota eliminada")
                }
                NotesEvent.RefreshCompleted -> scope.launch {
                    snackbarHostState.showSnackbar("Sincronizado con el servidor")
                }
                is NotesEvent.RefreshFailed -> scope.launch {
                    snackbarHostState.showSnackbar("Sin red: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis notas") },
                actions = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("Sync")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Nueva") },
                icon = { Text("+") },
                onClick = { showDialog = true },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                NotesUiState.Loading -> LoadingState()
                is NotesUiState.Error -> ErrorState(message = state.message)
                is NotesUiState.Success -> NotesList(
                    notes = state.notes,
                    onNoteClick = viewModel::onNoteClicked,
                    onDelete = viewModel::deleteNote,
                )
            }
        }
    }

    if (showDialog) {
        NewNoteDialog(
            onDismiss = { showDialog = false },
            onConfirm = { title, content ->
                viewModel.addNote(title, content)
                showDialog = false
            },
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NewNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva nota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titulo") },
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenido") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, content) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
