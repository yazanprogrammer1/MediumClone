package com.example.mediumclone.ui.screens.subscription

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureCheckoutScreen(
    navController: NavController,
    viewModel: SecureCheckoutViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onPaymentSuccess: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    
    val isProcessing by viewModel.isProcessing.collectAsState()
    var showOtpDialog by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    // Card Flip State
    var isBackVisible by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (isBackVisible) 180f else 0f)

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            onPaymentSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Secure Checkout", fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Credit Card Visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp)
                ) {
                   if (rotation <= 90f) {
                       // Front
                       Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                           Text("BANK OF MOCK", color = Color.White, fontWeight = FontWeight.Bold)
                           Text(
                               text = cardNumber.chunked(4).joinToString(" ").ifBlank { "#### #### #### ####" },
                               color = Color.White,
                               fontSize = 22.sp,
                               fontWeight = FontWeight.Bold
                           )
                           Row(
                               modifier = Modifier.fillMaxWidth(),
                               horizontalArrangement = Arrangement.SpaceBetween
                           ) {
                               Column {
                                   Text("CARD HOLDER", color = Color.LightGray, fontSize = 10.sp)
                                   Text(cardHolder.uppercase().ifBlank { "YOUR NAME" }, color = Color.White)
                               }
                               Column {
                                   Text("EXPIRES", color = Color.LightGray, fontSize = 10.sp)
                                   Text(expiryDate.ifBlank { "MM/YY" }, color = Color.White)
                               }
                           }
                       }
                   } else {
                       // Back (Flipped)
                       Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                           Column(modifier = Modifier.fillMaxSize()) {
                               Spacer(modifier = Modifier.height(20.dp))
                               Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.Black))
                               Spacer(modifier = Modifier.height(20.dp))
                               Row(modifier = Modifier.fillMaxWidth()) {
                                   Spacer(modifier = Modifier.weight(1f))
                                   Box(modifier = Modifier.width(60.dp).height(30.dp).background(Color.White), contentAlignment = Alignment.Center) {
                                       Text(cvv, color = Color.Black)
                                   }
                                   Spacer(modifier = Modifier.width(20.dp))
                               }
                           }
                       }
                   }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Form
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) cardNumber = it },
                    label = { Text("Card Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry (MM/YY)") },
                        modifier = Modifier.weight(1f),
                         shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { 
                            if (it.length <= 3) cvv = it
                            isBackVisible = true // Auto flip
                        },
                        label = { Text("CVV") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                             if (isBackVisible) {
                                 IconButton(onClick = { isBackVisible = false }) {
                                     Icon(Icons.Default.ArrowBack, contentDescription = "Flip Front")
                                 }
                             }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it; isBackVisible = false },
                    label = { Text("Card Holder Name") },
                    modifier = Modifier.fillMaxWidth(),
                     shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = { 
                        // Simulate pre-check then show OTP
                         kotlinx.coroutines.GlobalScope.launch {
                             // Quick loading state local for "Verifying"
                             // Ideally handled by VM but for mixed UI state logic:
                             showOtpDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = cardNumber.length == 16 && !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Pay Securely")
                    }
                }
            }
            
            // OTP Dialog (Simulated 3D Secure)
            if (showOtpDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    title = { Text("Bank Verification") },
                    text = { 
                         Column {
                             Text("Please enter the code sent to your mobile ending in ****88.")
                             Spacer(modifier = Modifier.height(16.dp))
                             OutlinedTextField(
                                 value = "1234",
                                 onValueChange = {},
                                 label = { Text("OTP Code") },
                                 readOnly = true // Prefilled for demo
                             )
                         }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showOtpDialog = false
                            viewModel.processPayment(onSuccess = {
                                showSuccess = true
                            })
                        }) {
                            Text("Submit")
                        }
                    }
                )
            }
            
            // Success Overlay
             if (showSuccess) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(64.dp))
                        Text("Payment Successful!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

