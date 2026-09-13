package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppColors.FacebookBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PakistanGreen = Color(0xFF006600)
private val BrandBlue = Color(0xFF1877F2)

enum class PhoneAuthStep {
    ENTER_PHONE,
    VERIFY_OTP,
    COMPLETE_PROFILE
}

@Composable
fun PhoneAuthDialog(
    onDismiss: () -> Unit,
    onSendOtp: (
        phoneNumber: String,
        activity: Activity?,
        onCodeSent: (verificationId: String, testCode: String?) -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onVerifyOtp: (
        phoneNumber: String,
        verificationId: String,
        otpCode: String,
        fullName: String?,
        username: String?,
        onResult: (success: Boolean, isNewUserNeedProfile: Boolean, message: String) -> Unit
    ) -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var currentStep by rememberSaveable { mutableStateOf(PhoneAuthStep.ENTER_PHONE) }
    var rawPhoneInput by rememberSaveable { mutableStateOf("") }
    var formattedPhone by rememberSaveable { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf("") }
    var devTestCode by rememberSaveable { mutableStateOf<String?>(null) }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendCountdown by rememberSaveable { mutableIntStateOf(60) }
    var canResend by rememberSaveable { mutableStateOf(false) }

    // Start timer when entering OTP step
    LaunchedEffect(currentStep, resendCountdown) {
        if (currentStep == PhoneAuthStep.VERIFY_OTP && resendCountdown > 0) {
            canResend = false
            delay(1000L)
            resendCountdown -= 1
            if (resendCountdown == 0) {
                canResend = true
            }
        }
    }

    // Determine Pakistani carrier preview from phone input
    val cleanDigits = rawPhoneInput.filter { it.isDigit() }
    val carrierName = remember(cleanDigits) {
        val prefix = when {
            cleanDigits.startsWith("03") && cleanDigits.length >= 4 -> cleanDigits.take(4)
            cleanDigits.startsWith("3") && cleanDigits.length >= 3 -> "0" + cleanDigits.take(3)
            cleanDigits.startsWith("923") && cleanDigits.length >= 5 -> "0" + cleanDigits.substring(2, 5)
            else -> ""
        }
        when (prefix) {
            "0300", "0301", "0302", "0303", "0304", "0305", "0306", "0307", "0308", "0309" -> "Jazz / Mobilink"
            "0310", "0311", "0312", "0313", "0314", "0315", "0316", "0317", "0318" -> "Zong 4G"
            "0320", "0321", "0322", "0323", "0324", "0325" -> "Warid"
            "0330", "0331", "0332", "0333", "0334", "0335", "0336", "0337" -> "Ufone 4G"
            "0340", "0341", "0342", "0343", "0344", "0345", "0346", "0347", "0348", "0349" -> "Telenor"
            "0355" -> "SCOM (Chitral & GB)"
            else -> if (cleanDigits.length >= 3) "Pakistan Cellular" else null
        }
    }

    fun normalizePakistaniNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        return when {
            digits.startsWith("923") && digits.length >= 12 -> "+$digits"
            digits.startsWith("03") && digits.length >= 11 -> "+92" + digits.substring(1)
            digits.startsWith("3") && digits.length >= 10 -> "+92$digits"
            else -> "+92" + digits.trimStart('0')
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .testTag("phone_auth_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PakistanGreen.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🇵🇰", fontSize = 22.sp)
                                }
                            }
                            Column {
                                Text(
                                    text = "Mobile Number Login",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Firebase SMS Verification",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp).testTag("close_phone_auth_btn")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error Banner
                    errorMessage?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentStep,
                        label = "PhoneAuthSteps"
                    ) { step ->
                        when (step) {
                            PhoneAuthStep.ENTER_PHONE -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Enter your Pakistani mobile number. We'll send a 6-digit verification code by SMS via Firebase Phone Authentication.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )

                                    // Phone Number Input with 🇵🇰 Flag and +92 Prefix
                                    OutlinedTextField(
                                        value = rawPhoneInput,
                                        onValueChange = { input ->
                                            rawPhoneInput = input
                                            errorMessage = null
                                        },
                                        placeholder = { Text("0300 1234567") },
                                        label = { Text("Pakistani Mobile Number") },
                                        leadingIcon = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                                            ) {
                                                Text("🇵🇰", fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "+92",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(1.dp)
                                                        .height(20.dp)
                                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (carrierName != null) {
                                                Surface(
                                                    color = PakistanGreen.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.padding(end = 8.dp)
                                                ) {
                                                    Text(
                                                        text = carrierName,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = PakistanGreen,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pakistan_phone_input")
                                    )

                                    // Quick select sample Chitral numbers for testing
                                    Column {
                                        Text(
                                            text = "Quick Demo / Test Numbers (Chitral & Yarkhoon):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val demoNumbers = listOf(
                                                "03459876543" to "Ali Khan",
                                                "03001234567" to "Chitral News",
                                                "03555551234" to "SCOM Yarkhoon"
                                            )
                                            demoNumbers.forEach { (num, label) ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            rawPhoneInput = num
                                                            errorMessage = null
                                                        }
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(6.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text(num, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Send SMS Button
                                    Button(
                                        onClick = {
                                            val clean = rawPhoneInput.filter { it.isDigit() }
                                            if (clean.length < 10) {
                                                errorMessage = "Please enter a valid 10 or 11-digit Pakistani mobile number (e.g. 0300 1234567)."
                                                return@Button
                                            }
                                            val normalized = normalizePakistaniNumber(rawPhoneInput)
                                            formattedPhone = normalized
                                            isLoading = true
                                            errorMessage = null

                                            onSendOtp(
                                                normalized,
                                                activity,
                                                { vId, testCode ->
                                                    isLoading = false
                                                    verificationId = vId
                                                    devTestCode = testCode
                                                    resendCountdown = 60
                                                    currentStep = PhoneAuthStep.VERIFY_OTP
                                                    Toast.makeText(context, "Verification SMS requested", Toast.LENGTH_SHORT).show()
                                                },
                                                { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                }
                                            )
                                        },
                                        enabled = !isLoading && rawPhoneInput.filter { it.isDigit() }.length >= 10,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("send_phone_otp_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Requesting SMS...", fontSize = 14.sp)
                                        } else {
                                            Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Send Verification OTP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }

                                    // Firebase Security info note
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Secured with Firebase Phone Authentication & Cloud Firestore",
                                            fontSize = 10.sp,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }

                            PhoneAuthStep.VERIFY_OTP -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Number badge with change button
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.PhoneIphone,
                                                    contentDescription = null,
                                                    tint = BrandBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = formattedPhone,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    currentStep = PhoneAuthStep.ENTER_PHONE
                                                    otpCode = ""
                                                    errorMessage = null
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Change", fontSize = 11.sp, color = BrandBlue)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Enter the 6-digit OTP code sent to your mobile number via SMS.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Dev/Sandbox Test helper card
                                    val testCodeToShow = devTestCode ?: "123456"
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "📱 Testing in Dev / Emulator Environment",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1B5E20)
                                                )
                                                Text(
                                                    text = "Use verification code: $testCodeToShow",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                            Button(
                                                onClick = { otpCode = testCodeToShow },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp).testTag("autofill_test_otp_btn")
                                            ) {
                                                Text("Auto-fill", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    // 6-digit OTP Input field
                                    OutlinedTextField(
                                        value = otpCode,
                                        onValueChange = { input ->
                                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                                otpCode = input
                                                errorMessage = null
                                            }
                                        },
                                        placeholder = { Text("123456", textAlign = TextAlign.Center) },
                                        label = { Text("6-Digit Verification Code") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.NumberPassword,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("otp_code_input")
                                    )

                                    // Verify Button
                                    Button(
                                        onClick = {
                                            if (otpCode.length != 6) {
                                                errorMessage = "Please enter the full 6-digit verification code."
                                                return@Button
                                            }
                                            isLoading = true
                                            errorMessage = null

                                            onVerifyOtp(
                                                formattedPhone,
                                                verificationId,
                                                otpCode,
                                                null,
                                                null
                                            ) { success, isNewUserNeedProfile, message ->
                                                isLoading = false
                                                if (success) {
                                                    if (isNewUserNeedProfile) {
                                                        // New user needs profile setup
                                                        val suggestedUsername = "user_" + formattedPhone.filter { it.isDigit() }.takeLast(6)
                                                        username = suggestedUsername
                                                        currentStep = PhoneAuthStep.COMPLETE_PROFILE
                                                    } else {
                                                        // Existing user logged in directly
                                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                        onSuccess()
                                                    }
                                                } else {
                                                    errorMessage = message
                                                }
                                            }
                                        },
                                        enabled = !isLoading && otpCode.length == 6,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("verify_phone_otp_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PakistanGreen)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Verifying OTP...", fontSize = 14.sp)
                                        } else {
                                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Verify & Continue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }

                                    // Resend OTP Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (canResend) {
                                            TextButton(
                                                onClick = {
                                                    isLoading = true
                                                    errorMessage = null
                                                    onSendOtp(
                                                        formattedPhone,
                                                        activity,
                                                        { vId, testCode ->
                                                            isLoading = false
                                                            verificationId = vId
                                                            devTestCode = testCode
                                                            resendCountdown = 60
                                                            canResend = false
                                                            Toast.makeText(context, "Resent SMS code", Toast.LENGTH_SHORT).show()
                                                        },
                                                        { err ->
                                                            isLoading = false
                                                            errorMessage = err
                                                        }
                                                    )
                                                }
                                            ) {
                                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Resend OTP Code", fontSize = 12.sp, color = BrandBlue)
                                            }
                                        } else {
                                            Text(
                                                text = "Resend code in ${resendCountdown}s",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }

                            PhoneAuthStep.COMPLETE_PROFILE -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        color = PakistanGreen.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = PakistanGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Phone Verified: $formattedPhone",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = PakistanGreen
                                                )
                                                Text(
                                                    text = "Enter your name to complete your Yarkhoon.com account.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = {
                                            fullName = it
                                            if (username.startsWith("user_") && it.isNotBlank()) {
                                                val autoUser = it.trim().lowercase().filter { c -> c.isLetterOrDigit() }
                                                username = "${autoUser}_chitral"
                                            }
                                            errorMessage = null
                                        },
                                        label = { Text("Full Name *") },
                                        placeholder = { Text("e.g. Sartaj Ahmad") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("phone_setup_fullname_input")
                                    )

                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = {
                                            username = it.trim().lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }
                                            errorMessage = null
                                        },
                                        label = { Text("Username *") },
                                        placeholder = { Text("e.g. sartaj_yarkhoon") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("phone_setup_username_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (fullName.isBlank()) {
                                                errorMessage = "Please enter your full name."
                                                return@Button
                                            }
                                            val finalUsername = username.ifBlank { "user_" + formattedPhone.takeLast(6) }
                                            isLoading = true
                                            errorMessage = null

                                            onVerifyOtp(
                                                formattedPhone,
                                                verificationId,
                                                otpCode,
                                                fullName.trim(),
                                                finalUsername
                                            ) { success, _, message ->
                                                isLoading = false
                                                if (success) {
                                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                    onSuccess()
                                                } else {
                                                    errorMessage = message
                                                }
                                            }
                                        },
                                        enabled = !isLoading && fullName.isNotBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("phone_complete_profile_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Creating Account...", fontSize = 14.sp)
                                        } else {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Complete Sign Up", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
