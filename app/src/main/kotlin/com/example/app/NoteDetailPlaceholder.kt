package com.example.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla de detalle - placeholder para demostrar que la navegacion
 * desde el feature ocurre via lambda y sin acoplamiento al feature.
 */
@Composable
fun NoteDetailPlaceholder(noteId: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Detalle de la nota $noteId")
    }
}
