package com.example.cmu_especial.ui.screens.review

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.cmu_especial.domain.model.DessertType
import com.example.cmu_especial.domain.model.Review
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    establishmentId: String,
    onSubmitted: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val ctx = LocalContext.current

    // Estado da UI
    var dessert by remember { mutableStateOf(DessertType.OUTRO) }
    var rating by remember { mutableStateOf(3f) }
    var comment by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // CameraX
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(establishmentId) { viewModel.load(establishmentId) }

    Scaffold(topBar = { TopAppBar(title = { Text("Nova avaliação") }) }) { padding ->
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Estabelecimento: ${ui.establishmentId}", fontWeight = FontWeight.SemiBold)

            ExposedDropdownMenuBoxSample(selected = dessert, onSelected = { dessert = it })

            Text("Classificação: ${rating.toInt()} ★")
            Slider(value = rating, onValueChange = { rating = it.coerceIn(1f, 5f) }, steps = 3, valueRange = 1f..5f)

            OutlinedTextField(
                value = comment, onValueChange = { comment = it },
                label = { Text("Comentário (opcional)") }, modifier = Modifier.fillMaxWidth()
            )

            // CameraX preview + botão tirar foto
            val camGranted = CameraPermissions()
            if (camGranted) {
                Box(Modifier.fillMaxWidth().height(220.dp)) {
                    CameraPreview(imageCapture = imageCapture, modifier = Modifier.matchParentSize())
                    Button(
                        onClick = {
                            scope.launch {
                                val uri = takePictureToGallery(imageCapture, ctx)
                                if (uri != null) photoUri = uri else error = "Falha ao guardar a foto"
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                    ) { Text("Tirar Foto") }
                }
            } else {
                Text("Permissão da câmara não concedida.")
            }

            photoUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Spacer(Modifier.weight(1f))

            Button(
                enabled = !isSubmitting && ui.currentLocation != null && ui.establishmentLocation != null,
                onClick = {
                    error = null
                    isSubmitting = true
                    val review = Review(
                        id = UUID.randomUUID().toString(),
                        userId = ui.userId,
                        userName = ui.userName,
                        establishmentId = ui.establishmentId,
                        dessert = dessert,
                        rating = rating.toInt(),
                        comment = comment.ifBlank { null },
                        photoLocalPath = photoUri?.toString(),
                        photoCloudUrl = null,
                        createdAt = System.currentTimeMillis(),
                        location = ui.currentLocation!! // não chega aqui se for null
                    )
                    scope.launch {
                        val ok = viewModel.submit(review)
                        if (ok) onSubmitted() else error = "Tens de estar a ≤ 50 m e/ou esperar 30 min desde a última."
                        isSubmitting = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isSubmitting) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("Submeter")
            }
        }
    }
}
