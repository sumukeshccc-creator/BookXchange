package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBookScanned: (title: String, author: String, condition: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    
    var isScanning by remember { mutableStateOf(false) }
    var currentScanMode by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            isScanning = true
            scanError = null
            
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val apiKey = BuildConfig.GEMINI_API_KEY
                        val prompt = when(currentScanMode) {
                            "barcode" -> "You are a Barcode (ISBN) Scanner. Read the barcode and output a JSON with 'title' and 'author'. ONLY JSON, no other text."
                            "ocr" -> "You are a Visual OCR Matrix. Read the text on this cover and output a JSON with 'title' and 'author'. ONLY JSON."
                            "condition" -> "You are an AI Book Analyzer. Analyze this physical book's cover. Extract its 'title' and 'author'. Also evaluate its visual condition and determine a 'condition' grade from exactly: 'Mint', 'Good', 'Fair', 'Poor'. Return ONLY a valid JSON object with keys 'title', 'author', and 'condition'."
                            else -> "Analyze this image and return a JSON with 'title' and 'author'."
                        }
                        
                        val requestBody = GenerateContentRequest(
                            contents = listOf(Content(
                                parts = listOf(
                                    Part(text = prompt),
                                    Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                                )
                            ))
                        )
                        
                        val response = RetrofitClient.service.generateContent(apiKey, requestBody)
                        var text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                        if (text != null && text.startsWith("```json")) {
                            text = text.removePrefix("```json").removeSuffix("```").trim()
                        }
                        text ?: "Error: No response text"
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                }
                
                isScanning = false
                if (result.startsWith("Error")) {
                    scanError = result
                } else {
                    try {
                        val json = org.json.JSONObject(result)
                        title = json.optString("title", title)
                        author = json.optString("author", author)
                        val scannedCondition = json.optString("condition", condition)
                        if (scannedCondition in listOf("Mint", "Good", "Fair", "Poor")) {
                            condition = scannedCondition
                        } else if (condition.isEmpty() && currentScanMode != "condition") {
                            condition = "Good" // Default if not scanned
                        }
                    } catch (e: Exception) {
                        scanError = "Failed to parse book details. Result: $result"
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            scanError = "Camera permission is required."
        }
    }

    fun launchCamera(mode: String) {
        currentScanMode = mode
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Scanner", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Manual entry is disabled for safety.\nPlease use AI to verify the book.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { launchCamera("barcode") }, 
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = "Barcode")
                        Spacer(Modifier.height(4.dp))
                        Text("ISBN")
                    }
                }
                
                Button(
                    onClick = { launchCamera("ocr") }, 
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "OCR")
                        Spacer(Modifier.height(4.dp))
                        Text("OCR Cover")
                    }
                }
            }
            
            Button(
                onClick = { launchCamera("condition") },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Condition")
                    Spacer(Modifier.height(4.dp))
                    Text("AI Strict Verification")
                }
            }
            
            if (isScanning) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
                Text("Gemini is analyzing the image...", style = MaterialTheme.typography.bodySmall)
            }
            
            if (scanError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = scanError!!, 
                        color = MaterialTheme.colorScheme.onErrorContainer, 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (title.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Scan Result:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Title: $title", fontWeight = FontWeight.Bold)
                        Text("Author: $author")
                        Text("Condition: $condition", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { 
                        if (title.isNotBlank() && author.isNotBlank() && condition.isNotBlank()) {
                            onBookScanned(title, author, condition)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isScanning
                ) {
                    Text("Add Verified Book to Library", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}
