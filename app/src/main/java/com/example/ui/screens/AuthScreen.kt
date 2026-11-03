package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.AuthMode
import com.example.ui.components.CropAspectRatio
import com.example.ui.components.ImageCropperDialog
import com.example.ui.theme.FbBlue
import com.example.ui.theme.FbBlueLight

enum class OnboardingStep {
    NAME_AND_HANDLE,
    CONTACT_INFO,
    PASSWORD_AND_TYPE,
    PROFILE_AND_INTERESTS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    authMode: AuthMode,
    isLoading: Boolean,
    errorMessage: String?,
    onSwitchMode: (AuthMode) -> Unit,
    onSignIn: (identifier: String, pass: String) -> Unit,
    onCompleteOnboarding: (
        name: String,
        handle: String,
        email: String,
        phoneNumber: String,
        pass: String,
        bio: String,
        education: String,
        jobTitle: String,
        location: String,
        interests: List<String>,
        isBusiness: Boolean,
        category: String,
        avatarUri: String?
    ) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onAdminSignIn: (email: String, pass: String, rememberMe: Boolean) -> Unit = { e, p, _ -> onSignIn(e, p) },
    modifier: Modifier = Modifier
) {
    // Top-level Auth Flow (Login vs Onboarding vs Forgot Password vs Admin Login)
    var isShowingOnboarding by remember { mutableStateOf(authMode == AuthMode.SIGN_UP) }
    var currentStep by remember { mutableStateOf(OnboardingStep.NAME_AND_HANDLE) }

    // Step 1: Name & Handle
    var fullName by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }

    // Step 2: Contact Info
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    // Step 3: Security & Business
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isBusinessAccount by remember { mutableStateOf(false) }
    var businessCategory by remember { mutableStateOf("Retail") }

    // Step 4: Profile & Interests
    var avatarUri by remember { mutableStateOf<String?>(null) }
    var jobTitle by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Yarkhoon Valley") }
    var bio by remember { mutableStateOf("") }
    val selectedInterests = remember { mutableStateListOf<String>() }

    // Login Form State
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoginPasswordVisible by remember { mutableStateOf(false) }

    // Dedicated Admin Login Form State
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var isAdminPasswordVisible by remember { mutableStateOf(false) }

    // Forgot Password Form State
    var forgotPasswordEmail by remember { mutableStateOf("") }

    val availableInterests = listOf(
        "🏔️ Mountain Sports",
        "🏇 Shandur Polo",
        "🧶 Handicrafts & Shu",
        "💻 Tech & Freelancing",
        "🎵 Folk Music & Sitar",
        "🏕️ Tourism & Treks",
        "🍎 Agriculture & Walnuts",
        "📚 Education & Teaching",
        "⚡ Solar & Hydro Trades",
        "📸 Photography"
    )

    val scrollState = rememberScrollState()

    // Synchronize onboarding state with authMode and automatic collision fallback
    LaunchedEffect(authMode) {
        if (authMode == AuthMode.SIGN_IN) {
            isShowingOnboarding = false
            if (email.isNotBlank() && loginIdentifier.isBlank()) {
                loginIdentifier = email.trim()
            }
        } else if (authMode == AuthMode.SIGN_UP) {
            isShowingOnboarding = true
        }
    }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            if (errorMessage.contains("already exists", ignoreCase = true) ||
                errorMessage.contains("already in use", ignoreCase = true)
            ) {
                isShowingOnboarding = false
                if (email.isNotBlank() && loginIdentifier.isBlank()) {
                    loginIdentifier = email.trim()
                }
            }
            try {
                scrollState.animateScrollTo(0)
            } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error banner if any
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("auth_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (authMode == AuthMode.FORGOT_PASSWORD) {
                // ---------------- FORGOT PASSWORD SCREEN ----------------
                ForgotPasswordContent(
                    email = forgotPasswordEmail,
                    onEmailChange = { forgotPasswordEmail = it },
                    onSubmit = { onForgotPassword(forgotPasswordEmail) },
                    onBackToLogin = { onSwitchMode(AuthMode.SIGN_IN) }
                )
            } else if (authMode == AuthMode.ADMIN_LOGIN) {
                // ---------------- DEDICATED SECURE ADMIN LOGIN SCREEN ----------------
                AdminLoginContent(
                    email = adminEmail,
                    onEmailChange = { adminEmail = it },
                    password = adminPassword,
                    onPasswordChange = { adminPassword = it },
                    isPasswordVisible = isAdminPasswordVisible,
                    onTogglePasswordVisibility = { isAdminPasswordVisible = !isAdminPasswordVisible },
                    isLoading = isLoading,
                    onAdminLoginSubmit = { rememberMe -> onAdminSignIn(adminEmail, adminPassword, rememberMe) },
                    onForgotPasswordClick = {
                        forgotPasswordEmail = adminEmail
                        onSwitchMode(AuthMode.FORGOT_PASSWORD)
                    },
                    onBackToMemberLogin = { onSwitchMode(AuthMode.SIGN_IN) }
                )
            } else if (!isShowingOnboarding) {
                // ---------------- FACEBOOK-STYLE LOGIN SCREEN ----------------
                LoginContent(
                    identifier = loginIdentifier,
                    onIdentifierChange = { loginIdentifier = it },
                    password = loginPassword,
                    onPasswordChange = { loginPassword = it },
                    isPasswordVisible = isLoginPasswordVisible,
                    onTogglePasswordVisibility = { isLoginPasswordVisible = !isLoginPasswordVisible },
                    isLoading = isLoading,
                    onLoginSubmit = { onSignIn(loginIdentifier, loginPassword) },
                    onForgotPasswordClick = {
                        if (loginIdentifier.contains("@")) {
                            forgotPasswordEmail = loginIdentifier.trim()
                        }
                        onSwitchMode(AuthMode.FORGOT_PASSWORD)
                    },
                    onCreateNewAccountClick = {
                        isShowingOnboarding = true
                        currentStep = OnboardingStep.NAME_AND_HANDLE
                        onSwitchMode(AuthMode.SIGN_UP)
                    },
                    onAdminLoginClick = {
                        isShowingOnboarding = false
                        if (loginIdentifier.contains("@") && adminEmail.isBlank()) {
                            adminEmail = loginIdentifier.trim()
                        }
                        onSwitchMode(AuthMode.ADMIN_LOGIN)
                    }
                )
            } else {
                // ---------------- FACEBOOK-STYLE ONBOARDING WIZARD ----------------
                val progress = when (currentStep) {
                    OnboardingStep.NAME_AND_HANDLE -> 0.25f
                    OnboardingStep.CONTACT_INFO -> 0.50f
                    OnboardingStep.PASSWORD_AND_TYPE -> 0.75f
                    OnboardingStep.PROFILE_AND_INTERESTS -> 1.0f
                }

                // Header with Back button, Logo and Linear Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            when (currentStep) {
                                OnboardingStep.NAME_AND_HANDLE -> {
                                    isShowingOnboarding = false
                                    onSwitchMode(AuthMode.SIGN_IN)
                                }
                                OnboardingStep.CONTACT_INFO -> currentStep = OnboardingStep.NAME_AND_HANDLE
                                OnboardingStep.PASSWORD_AND_TYPE -> currentStep = OnboardingStep.CONTACT_INFO
                                OnboardingStep.PROFILE_AND_INTERESTS -> currentStep = OnboardingStep.PASSWORD_AND_TYPE
                            }
                        },
                        modifier = Modifier.testTag("onboarding_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Join YARKHOON.COM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FbBlue
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = FbBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboarding_steps"
                ) { step ->
                    when (step) {
                        OnboardingStep.NAME_AND_HANDLE -> {
                            StepNameAndHandle(
                                fullName = fullName,
                                onFullNameChange = { fullName = it },
                                handle = handle,
                                onHandleChange = { handle = it },
                                onNext = {
                                    if (fullName.isNotBlank() && handle.isNotBlank()) {
                                        currentStep = OnboardingStep.CONTACT_INFO
                                    }
                                }
                            )
                        }

                        OnboardingStep.CONTACT_INFO -> {
                            StepContactInfo(
                                email = email,
                                onEmailChange = { email = it },
                                phoneNumber = phoneNumber,
                                onPhoneNumberChange = { phoneNumber = it },
                                onNext = {
                                    if (email.isNotBlank() || phoneNumber.isNotBlank()) {
                                        currentStep = OnboardingStep.PASSWORD_AND_TYPE
                                    }
                                }
                            )
                        }

                        OnboardingStep.PASSWORD_AND_TYPE -> {
                            StepPasswordAndType(
                                password = password,
                                onPasswordChange = { password = it },
                                isPasswordVisible = isPasswordVisible,
                                onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                                isBusiness = isBusinessAccount,
                                onBusinessChange = { isBusinessAccount = it },
                                businessCategory = businessCategory,
                                onCategoryChange = { businessCategory = it },
                                onNext = {
                                    if (password.length >= 6) {
                                        currentStep = OnboardingStep.PROFILE_AND_INTERESTS
                                    }
                                }
                            )
                        }

                        OnboardingStep.PROFILE_AND_INTERESTS -> {
                            StepProfileAndInterests(
                                avatarUri = avatarUri,
                                onAvatarUriChange = { avatarUri = it },
                                jobTitle = jobTitle,
                                onJobTitleChange = { jobTitle = it },
                                education = education,
                                onEducationChange = { education = it },
                                location = location,
                                onLocationChange = { location = it },
                                bio = bio,
                                onBioChange = { bio = it },
                                availableInterests = availableInterests,
                                selectedInterests = selectedInterests,
                                onToggleInterest = { interest ->
                                    if (selectedInterests.contains(interest)) {
                                        selectedInterests.remove(interest)
                                    } else {
                                        selectedInterests.add(interest)
                                    }
                                },
                                isLoading = isLoading,
                                onComplete = {
                                    onCompleteOnboarding(
                                        fullName,
                                        handle,
                                        email,
                                        phoneNumber,
                                        password,
                                        bio,
                                        education,
                                        jobTitle,
                                        location,
                                        selectedInterests.toList(),
                                        isBusinessAccount,
                                        businessCategory,
                                        avatarUri
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Login Prompt
                TextButton(
                    onClick = {
                        if (email.isNotBlank() && loginIdentifier.isBlank()) {
                            loginIdentifier = email.trim()
                        }
                        isShowingOnboarding = false
                        onSwitchMode(AuthMode.SIGN_IN)
                    },
                    modifier = Modifier.testTag("already_have_account_btn")
                ) {
                    Text(
                        text = "Already have an account? Log In",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = FbBlue
                    )
                }
            }
        }
    }
}

// ==========================================
// ONBOARDING STEP 1: NAME AND USERNAME
// ==========================================
@Composable
private fun StepNameAndHandle(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    handle: String,
    onHandleChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "What's your name?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter the name you use in real life and pick a unique username for your profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full Name") },
            placeholder = { Text("e.g. Hamid Shah") },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_fullname_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = handle,
            onValueChange = onHandleChange,
            label = { Text("Username (@handle)") },
            placeholder = { Text("e.g. @hamid_yarkhoon") },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_handle_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_step1_next"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
            enabled = fullName.isNotBlank() && handle.isNotBlank()
        ) {
            Text(text = "Next", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// ONBOARDING STEP 2: CONTACT DETAILS
// ==========================================
@Composable
private fun StepContactInfo(
    email: String,
    onEmailChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Enter your contact info",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add your email and mobile phone number to connect with friends and secure your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            placeholder = { Text("name@example.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_email_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Mobile Phone Number") },
            placeholder = { Text("+92 345 1234567") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_phone_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        val isEmailValid = email.isNotBlank() && email.contains("@") && email.contains(".")

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_step2_next"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
            enabled = isEmailValid
        ) {
            Text(text = "Next", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// ONBOARDING STEP 3: PASSWORD & ACCOUNT PURPOSE
// ==========================================
@Composable
private fun StepPasswordAndType(
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isBusiness: Boolean,
    onBusinessChange: (Boolean) -> Unit,
    businessCategory: String,
    onCategoryChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val categories = listOf("Retail", "Hospitality", "Skilled Trades", "Crafts", "Education", "Services", "Tech & Freelance")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Create a password",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose a strong password with at least 6 characters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_password_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Account Type: Personal vs Business / Professional
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBusinessChange(!isBusiness) },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isBusiness) FbBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = CardDefaults.outlinedCardBorder(enabled = isBusiness)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isBusiness,
                    onCheckedChange = onBusinessChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Register as Business or Skilled Professional",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Display your trade, store, or craft in the YARKHOON.COM Business Hub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isBusiness) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "Select your trade or business category:", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OptInFlowCategoryChips(
                categories = categories,
                selected = businessCategory,
                onSelected = onCategoryChange
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_step3_next"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
            enabled = password.length >= 6
        ) {
            Text(text = "Next", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowCategoryChips(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelected(cat) },
                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FbBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

// ==========================================
// ONBOARDING STEP 4: PROFILE, EDUCATION, JOB & INTERESTS
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepProfileAndInterests(
    avatarUri: String?,
    onAvatarUriChange: (String?) -> Unit,
    jobTitle: String,
    onJobTitleChange: (String) -> Unit,
    education: String,
    onEducationChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    availableInterests: List<String>,
    selectedInterests: List<String>,
    onToggleInterest: (String) -> Unit,
    isLoading: Boolean = false,
    onComplete: () -> Unit
) {
    var cropTargetUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            cropTargetUri = uri
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Profile & Community Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select a profile photo from your device and tell peers about your background.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Profile Photo Picker Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .border(2.dp, FbBlue, CircleShape)
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Chosen Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Column {
                FilledTonalButton(
                    onClick = {
                        avatarPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = FbBlue.copy(alpha = 0.12f),
                        contentColor = FbBlue
                    ),
                    modifier = Modifier.testTag("onboarding_avatar_picker_btn")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (avatarUri != null) "Change Photo" else "Select Photo from Gallery",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (avatarUri != null) {
                    TextButton(
                        onClick = { onAvatarUriChange(null) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Remove photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = jobTitle,
            onValueChange = onJobTitleChange,
            label = { Text("Job / Profession / Work") },
            placeholder = { Text("e.g. Solar Technician, Teacher, Store Owner") },
            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_job_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = education,
            onValueChange = onEducationChange,
            label = { Text("Education / College / School") },
            placeholder = { Text("e.g. University of Chitral, Degree College Booni") },
            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_education_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Village / Location") },
            placeholder = { Text("e.g. Yarkhoon Lasht, Mastuj, Booni") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_location_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            label = { Text("Short Bio / Description") },
            placeholder = { Text("A few words introducing yourself to the community...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .testTag("onboarding_bio_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Pick Your Interests:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableInterests.forEach { interest ->
                val isSelected = selectedInterests.contains(interest)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleInterest(interest) },
                    label = { Text(interest, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FbBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_finish_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text(text = "Complete & Enter YARKHOON.COM", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal: Avatar Image Cropper & Aspect Ratio Adjuster
    cropTargetUri?.let { uriToCrop ->
        ImageCropperDialog(
            imageUri = uriToCrop,
            initialAspectRatio = CropAspectRatio.CIRCLE,
            allowedAspectRatios = listOf(
                CropAspectRatio.CIRCLE,
                CropAspectRatio.SQUARE,
                CropAspectRatio.FREE
            ),
            title = "Crop Profile Picture",
            onDismiss = { cropTargetUri = null },
            onCropSuccess = { croppedUri ->
                onAvatarUriChange(croppedUri.toString())
                cropTargetUri = null
            }
        )
    }
}

// ==========================================
// FACEBOOK-STYLE LOGIN CONTENT
// ==========================================
@Composable
private fun LoginContent(
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    onLoginSubmit: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateNewAccountClick: () -> Unit,
    onAdminLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Brand Emblem
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground_asset),
            contentDescription = "YARKHOON.COM",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(18.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "YARKHOON.COM",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = FbBlue,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "The Mountain Community Social Network",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = onIdentifierChange,
            label = { Text("Mobile number, email, or @handle") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signin_identifier_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signin_password_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLoginSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("signin_submit_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
            enabled = !isLoading && identifier.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text(text = "Log In", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onForgotPasswordClick) {
            Text(
                text = "Forgotten password?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = FbBlue
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = " OR ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Facebook style "Create new account" button
        OutlinedButton(
            onClick = onCreateNewAccountClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("start_create_account_btn"),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = FbBlue
            )
        ) {
            Text(
                text = "Create New YARKHOON.COM Account",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Discrete small Admin login button aligned to the right side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onAdminLoginClick,
                modifier = Modifier.testTag("open_admin_login_btn"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Admin",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Admin",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ==========================================
// DEDICATED SECURE ADMIN LOGIN CONTENT
// ==========================================
@Composable
private fun AdminLoginContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    onAdminLoginSubmit: (rememberMe: Boolean) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onBackToMemberLogin: () -> Unit
) {
    var rememberMe by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_login_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground_asset),
            contentDescription = "Admin Control Panel",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(18.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "YARKHOON ADMIN PORTAL",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Authorized Super Admin, Admin & Moderator Access Only",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = FbBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Security Notice: Public administrator registration is disabled. All login attempts and administrative actions are verified against backend roles and recorded in the audit log.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Administrator Email Address") },
            placeholder = { Text("admin@yarkhoon.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_email_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Administrator Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    modifier = Modifier.testTag("admin_toggle_password_btn")
                ) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_password_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Remember Me Toggle / Checkbox Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { rememberMe = !rememberMe }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                modifier = Modifier.testTag("admin_remember_me_checkbox"),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF1E3A8A),
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Remember Me",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Keep session active on this device (4-hour inactivity limit applies)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { onAdminLoginSubmit(rememberMe) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("admin_login_submit_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("admin_login_loading_indicator")
                )
            } else {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Secure Admin Sign In",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.testTag("admin_forgot_password_btn")
            ) {
                Text(
                    text = "Forgot Admin Password?",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FbBlue
                )
            }

            TextButton(
                onClick = onBackToMemberLogin,
                modifier = Modifier.testTag("back_to_member_login_btn")
            ) {
                Text(
                    text = "Back to Member Login",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// FORGOT PASSWORD CONTENT
// ==========================================
@Composable
private fun ForgotPasswordContent(
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Find Your Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your registered email address or phone number linked to your YARKHOON.COM account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Registered Email Address") },
            placeholder = { Text("user@example.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("forgot_email_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("send_password_reset_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
            enabled = email.isNotBlank() && email.contains("@") && email.contains(".")
        ) {
            Text(text = "Send Password Reset Email", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBackToLogin) {
            Text(text = "Cancel and Back to Login", color = FbBlue)
        }
    }
}
