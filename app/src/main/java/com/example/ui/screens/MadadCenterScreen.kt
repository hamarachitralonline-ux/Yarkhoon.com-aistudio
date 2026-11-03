package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.YarkhoonUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MadadCenterScreen(
    uiState: YarkhoonUiState,
    currentUser: User,
    onDismiss: () -> Unit,
    onSelectCategory: (MadadCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onJobCityFilterChange: (String) -> Unit,
    onBloodGroupFilterChange: (String) -> Unit,
    onBloodCityFilterChange: (String) -> Unit,
    onAidCategoryFilterChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onContactUser: (User) -> Unit,
    onSaveJobSeekerProfile: (qualifications: String, skills: List<String>, experienceYears: Int, preferredRoles: List<String>, preferredCities: List<String>, summary: String, contactPreference: String, isPhonePublic: Boolean, phoneNumber: String?) -> Unit,
    onPublishJobVacancy: (title: String, companyName: String, city: String, location: String, jobType: String, salaryRange: String?, requiredSkills: List<String>, description: String, applicationInstructions: String, contactEmail: String?, contactPhone: String?) -> Unit,
    onRegisterBloodDonor: (bloodGroup: String, city: String, area: String, isAvailable: Boolean, lastDonationDate: String?, contactPreference: String, isPhonePublic: Boolean, phone: String?) -> Unit,
    onCreateBloodRequest: (patientName: String, bloodGroup: String, hospitalName: String, city: String, requiredUnits: Int, urgency: String, contactPhone: String, notes: String) -> Unit,
    onCreateCommunityAidRequest: (beneficiaryName: String, category: String, title: String, description: String, city: String, location: String, assistanceType: String, targetAmount: Double?, contactMethod: String) -> Unit,
    onMarkBloodRequestFulfilled: (String) -> Unit,
    onMarkCommunityAidFulfilled: (String) -> Unit,
    onCloseJobVacancy: (String) -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onDismiss)

    var showJobSeekerDialog by remember { mutableStateOf(false) }
    var showPostJobVacancyDialog by remember { mutableStateOf(false) }
    var showRegisterDonorDialog by remember { mutableStateOf(false) }
    var showRequestBloodDialog by remember { mutableStateOf(false) }
    var showRequestAidDialog by remember { mutableStateOf(false) }
    var jobTab by remember { mutableIntStateOf(0) }
    var bloodTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("madad_center_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = FbBlueLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VolunteerActivism,
                                    contentDescription = null,
                                    tint = FbBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Madad Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FbTextPrimary
                            )
                            Text(
                                text = "Community Relief & Mutual Aid",
                                style = MaterialTheme.typography.labelSmall,
                                color = FbTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("madad_center_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FbTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("madad_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = FbBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FbBackground)
        ) {
            // Category Selector Chips
            MadadCategoryHeader(
                activeCategory = uiState.activeMadadCategory,
                onCategorySelected = onSelectCategory
            )

            // Content Area depending on Active Category
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState.activeMadadCategory) {
                    MadadCategory.JOBS -> {
                        JobSupportSection(
                            jobTab = jobTab,
                            onJobTabChange = { jobTab = it },
                            searchQuery = uiState.madadJobSkillQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            cityFilter = uiState.madadJobCityFilter,
                            onCityFilterChange = onJobCityFilterChange,
                            vacancies = uiState.jobVacancies,
                            seekers = uiState.jobSeekerProfiles,
                            currentUserId = currentUser.id,
                            onContactUser = onContactUser,
                            onOpenRegisterProfile = { showJobSeekerDialog = true },
                            onOpenPostVacancy = { showPostJobVacancyDialog = true },
                            onCloseVacancy = onCloseJobVacancy
                        )
                    }
                    MadadCategory.BLOOD_DONATION -> {
                        BloodDonationSection(
                            bloodTab = bloodTab,
                            onBloodTabChange = { bloodTab = it },
                            selectedBloodGroup = uiState.madadBloodGroupFilter,
                            onBloodGroupChange = onBloodGroupFilterChange,
                            cityFilter = uiState.madadBloodCityFilter,
                            onCityFilterChange = onBloodCityFilterChange,
                            requests = uiState.bloodRequests,
                            donors = uiState.bloodDonors,
                            currentUserId = currentUser.id,
                            onContactUser = onContactUser,
                            onOpenRegisterDonor = { showRegisterDonorDialog = true },
                            onOpenRequestBlood = { showRequestBloodDialog = true },
                            onMarkFulfilled = onMarkBloodRequestFulfilled
                        )
                    }
                    MadadCategory.COMMUNITY_AID -> {
                        CommunityAidSection(
                            requests = uiState.communityAidRequests,
                            selectedAidCategory = uiState.madadAidCategoryFilter,
                            onAidCategoryChange = onAidCategoryFilterChange,
                            currentUserId = currentUser.id,
                            onContactUser = onContactUser,
                            onOpenRequestAid = { showRequestAidDialog = true },
                            onMarkFulfilled = onMarkCommunityAidFulfilled
                        )
                    }
                    MadadCategory.LEGAL_EMERGENCY -> {
                        LegalEmergencySection(
                            topics = uiState.legalGuidanceTopics,
                            emergencyContacts = uiState.emergencyContacts,
                            onDial = { phone ->
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot dial: $phone", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs for Forms
    if (showJobSeekerDialog) {
        JobSeekerDialog(
            currentUser = currentUser,
            onDismiss = { showJobSeekerDialog = false },
            onSubmit = { qual, skills, exp, roles, cities, sum, pref, isPub, phone ->
                onSaveJobSeekerProfile(qual, skills, exp, roles, cities, sum, pref, isPub, phone)
                showJobSeekerDialog = false
            }
        )
    }

    if (showPostJobVacancyDialog) {
        PostJobVacancyDialog(
            onDismiss = { showPostJobVacancyDialog = false },
            onSubmit = { title, comp, city, loc, role, salary, skills, desc, inst, email, phone ->
                onPublishJobVacancy(title, comp, city, loc, role, salary, skills, desc, inst, email, phone)
                showPostJobVacancyDialog = false
            }
        )
    }

    if (showRegisterDonorDialog) {
        RegisterBloodDonorDialog(
            currentUser = currentUser,
            onDismiss = { showRegisterDonorDialog = false },
            onSubmit = { bloodGroup, city, area, available, lastDate, pref, isPub, phone ->
                onRegisterBloodDonor(bloodGroup, city, area, available, lastDate, pref, isPub, phone)
                showRegisterDonorDialog = false
            }
        )
    }

    if (showRequestBloodDialog) {
        RequestBloodDialog(
            onDismiss = { showRequestBloodDialog = false },
            onSubmit = { name, group, hosp, city, units, urg, phone, notes ->
                onCreateBloodRequest(name, group, hosp, city, units, urg, phone, notes)
                showRequestBloodDialog = false
            }
        )
    }

    if (showRequestAidDialog) {
        RequestCommunityAidDialog(
            onDismiss = { showRequestAidDialog = false },
            onSubmit = { ben, cat, title, desc, city, loc, assist, amount, contactMethod ->
                onCreateCommunityAidRequest(ben, cat, title, desc, city, loc, assist, amount, contactMethod)
                showRequestAidDialog = false
            }
        )
    }
}

// ==========================================
// 1. Category Switcher Bar
// ==========================================
@Composable
private fun MadadCategoryHeader(
    activeCategory: MadadCategory,
    onCategorySelected: (MadadCategory) -> Unit
) {
    Surface(
        color = FbSurface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MadadCategory.entries.forEach { category ->
                val isSelected = category == activeCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    label = {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        val icon = when (category) {
                            MadadCategory.JOBS -> Icons.Default.Work
                            MadadCategory.BLOOD_DONATION -> Icons.Default.Bloodtype
                            MadadCategory.COMMUNITY_AID -> Icons.Default.VolunteerActivism
                            MadadCategory.LEGAL_EMERGENCY -> Icons.Default.Shield
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("madad_category_${category.name.lowercase()}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FbBlue,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = FbBackground,
                        labelColor = FbTextPrimary
                    )
                )
            }
        }
    }
}

// ==========================================
// 2. Job Support Section
// ==========================================
@Composable
private fun JobSupportSection(
    jobTab: Int,
    onJobTabChange: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    cityFilter: String,
    onCityFilterChange: (String) -> Unit,
    vacancies: List<JobVacancy>,
    seekers: List<JobSeekerProfile>,
    currentUserId: String,
    onContactUser: (User) -> Unit,
    onOpenRegisterProfile: () -> Unit,
    onOpenPostVacancy: () -> Unit,
    onCloseVacancy: (String) -> Unit
) {
    val cities = listOf("All Cities", "Chitral", "Islamabad", "Peshawar", "Lahore", "Karachi", "Gilgit")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = jobTab,
            containerColor = FbSurface,
            contentColor = FbBlue
        ) {
            Tab(
                selected = jobTab == 0,
                onClick = { onJobTabChange(0) },
                text = { Text("Job Vacancies (${vacancies.size})", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("job_tab_vacancies")
            )
            Tab(
                selected = jobTab == 1,
                onClick = { onJobTabChange(1) },
                text = { Text("Job Seekers (${seekers.size})", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("job_tab_seekers")
            )
        }

        Surface(color = FbSurface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search title, skills, or qualification...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FbBackground,
                        unfocusedContainerColor = FbBackground
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cities.forEach { city ->
                        val isSelected = cityFilter.equals(city, ignoreCase = true)
                        SuggestionChip(
                            onClick = { onCityFilterChange(city) },
                            label = { Text(city, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) FbBlueLight else FbBackground,
                                labelColor = if (isSelected) FbBlue else FbTextSecondary
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) FbBlue else Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = FbBlueLight),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = FbBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Structured profiles and vacancies are active and searchable. Direct in-app messaging connects talent.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FbBlueHover
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenPostVacancy,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_post_job_vacancy"),
                colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Post Vacancy", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = onOpenRegisterProfile,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_register_job_seeker"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Job Seeker Profile", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (jobTab == 0) {
            if (vacancies.isEmpty()) {
                EmptyStateCard(
                    message = "No job vacancies found matching current filters.",
                    actionText = "Post the First Vacancy",
                    onAction = onOpenPostVacancy
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vacancies, key = { it.id }) { vacancy ->
                        JobVacancyCard(
                            vacancy = vacancy,
                            isAuthor = vacancy.employerId == currentUserId,
                            onContactEmployer = {
                                val empUser = User(
                                    id = vacancy.employerId,
                                    name = vacancy.employerName,
                                    handle = vacancy.employerHandle
                                )
                                onContactUser(empUser)
                            },
                            onCloseVacancy = { onCloseVacancy(vacancy.id) }
                        )
                    }
                }
            }
        } else {
            if (seekers.isEmpty()) {
                EmptyStateCard(
                    message = "No job seekers found matching current filters.",
                    actionText = "Register Your Profile",
                    onAction = onOpenRegisterProfile
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(seekers, key = { it.id }) { seeker ->
                        JobSeekerCard(
                            seeker = seeker,
                            isSelf = seeker.userId == currentUserId,
                            onConnect = {
                                val user = User(
                                    id = seeker.userId,
                                    name = seeker.name,
                                    handle = seeker.handle
                                )
                                onContactUser(user)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JobVacancyCard(
    vacancy: JobVacancy,
    isAuthor: Boolean,
    onContactEmployer: () -> Unit,
    onCloseVacancy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("job_vacancy_card_${vacancy.id}"),
        colors = CardDefaults.cardColors(containerColor = FbSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vacancy.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FbTextPrimary
                    )
                    Text(
                        text = "${vacancy.companyName} • ${vacancy.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FbTextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (vacancy.status == "active") FbBlueLight else FbBackground
                ) {
                    Text(
                        text = vacancy.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (vacancy.status == "active") FbBlue else FbTextSecondary
                    )
                }
            }

            vacancy.salaryRange?.let { salary ->
                if (salary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Compensation: $salary",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SocialRepost
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = vacancy.description,
                style = MaterialTheme.typography.bodyMedium,
                color = FbTextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (vacancy.requiredSkills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vacancy.requiredSkills.forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FbBackground
                        ) {
                            Text(
                                text = skill,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = FbTextSecondary
                            )
                        }
                    }
                }
            }

            if (vacancy.applicationInstructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Instructions: ${vacancy.applicationInstructions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FbTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAuthor && vacancy.status == "active") {
                    TextButton(onClick = onCloseVacancy) {
                        Text("Mark Closed", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!isAuthor && vacancy.status == "active") {
                    Button(
                        onClick = onContactEmployer,
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_contact_employer_${vacancy.id}")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contact Employer", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun JobSeekerCard(
    seeker: JobSeekerProfile,
    isSelf: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("job_seeker_card_${seeker.id}"),
        colors = CardDefaults.cardColors(containerColor = FbSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = seeker.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FbTextPrimary
                    )
                    Text(
                        text = seeker.preferredRoles.firstOrNull() ?: "Professional",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = FbBlue
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (seeker.isActivelySeeking) FbBlueLight else FbBackground
                ) {
                    Text(
                        text = if (seeker.isActivelySeeking) "Available Now" else "Passive",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (seeker.isActivelySeeking) FbBlue else FbTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Qualification: ${seeker.qualifications} • Experience: ${seeker.experienceYears} yrs",
                style = MaterialTheme.typography.bodySmall,
                color = FbTextSecondary
            )

            if (seeker.preferredCities.isNotEmpty()) {
                Text(
                    text = "Preferred Cities: ${seeker.preferredCities.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FbTextSecondary
                )
            }

            if (seeker.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = seeker.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FbTextPrimary
                )
            }

            if (seeker.skills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    seeker.skills.forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FbBackground
                        ) {
                            Text(
                                text = skill,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = FbTextSecondary
                            )
                        }
                    }
                }
            }

            if (!isSelf) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_connect_seeker_${seeker.id}")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect (Chat)", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Blood Donation Section
// ==========================================
@Composable
private fun BloodDonationSection(
    bloodTab: Int,
    onBloodTabChange: (Int) -> Unit,
    selectedBloodGroup: String,
    onBloodGroupChange: (String) -> Unit,
    cityFilter: String,
    onCityFilterChange: (String) -> Unit,
    requests: List<BloodRequest>,
    donors: List<BloodDonor>,
    currentUserId: String,
    onContactUser: (User) -> Unit,
    onOpenRegisterDonor: () -> Unit,
    onOpenRequestBlood: () -> Unit,
    onMarkFulfilled: (String) -> Unit
) {
    val bloodGroups = listOf("All Groups", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = bloodTab,
            containerColor = FbSurface,
            contentColor = SocialHeart
        ) {
            Tab(
                selected = bloodTab == 0,
                onClick = { onBloodTabChange(0) },
                text = { Text("Urgent Requests (${requests.size})", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("blood_tab_requests")
            )
            Tab(
                selected = bloodTab == 1,
                onClick = { onBloodTabChange(1) },
                text = { Text("Available Donors (${donors.size})", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("blood_tab_donors")
            )
        }

        Surface(color = FbSurface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Filter by Blood Group:",
                    style = MaterialTheme.typography.labelSmall,
                    color = FbTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bloodGroups.forEach { group ->
                        val isSelected = selectedBloodGroup.equals(group, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onBloodGroupChange(group) },
                            label = { Text(group, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SocialHeart,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenRequestBlood,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_request_blood"),
                colors = ButtonDefaults.buttonColors(containerColor = SocialHeart),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Request Blood", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = onOpenRegisterDonor,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_register_donor"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = SocialHeart)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Donor", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (bloodTab == 0) {
            if (requests.isEmpty()) {
                EmptyStateCard(
                    message = "No active blood requests found for this filter.",
                    actionText = "Post an Urgent Request",
                    onAction = onOpenRequestBlood
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(requests, key = { it.id }) { req ->
                        BloodRequestCard(
                            request = req,
                            isAuthor = req.requesterId == currentUserId,
                            onContact = {
                                val user = User(id = req.requesterId, name = req.requesterName, handle = req.requesterHandle)
                                onContactUser(user)
                            },
                            onMarkFulfilled = { onMarkFulfilled(req.id) }
                        )
                    }
                }
            }
        } else {
            if (donors.isEmpty()) {
                EmptyStateCard(
                    message = "No registered donors found for this blood group.",
                    actionText = "Become a Life Saver",
                    onAction = onOpenRegisterDonor
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(donors, key = { it.id }) { donor ->
                        BloodDonorCard(
                            donor = donor,
                            isSelf = donor.userId == currentUserId,
                            onContact = {
                                val user = User(id = donor.userId, name = donor.name, handle = donor.handle)
                                onContactUser(user)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BloodRequestCard(
    request: BloodRequest,
    isAuthor: Boolean,
    onContact: () -> Unit,
    onMarkFulfilled: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blood_request_card_${request.id}"),
        colors = CardDefaults.cardColors(containerColor = FbSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SocialHeart.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.bloodGroup,
                                fontWeight = FontWeight.ExtraBold,
                                color = SocialHeart,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Patient: ${request.patientName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FbTextPrimary
                        )
                        Text(
                            text = "${request.hospitalName}, ${request.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FbTextSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (request.status == "active") SocialHeart.copy(alpha = 0.1f) else FbBackground
                ) {
                    Text(
                        text = if (request.status == "active") "URGENT" else "FULFILLED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (request.status == "active") SocialHeart else FbTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Required Units: ${request.requiredUnits} • Urgency: ${request.urgency}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = FbTextPrimary
            )

            if (request.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = request.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = FbTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAuthor && request.status == "active") {
                    TextButton(onClick = onMarkFulfilled) {
                        Text("Mark Fulfilled", color = SocialRepost)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!isAuthor && request.status == "active") {
                    Button(
                        onClick = onContact,
                        colors = ButtonDefaults.buttonColors(containerColor = SocialHeart),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_contact_requester_${request.id}")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contact in Chat", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BloodDonorCard(
    donor: BloodDonor,
    isSelf: Boolean,
    onContact: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blood_donor_card_${donor.id}"),
        colors = CardDefaults.cardColors(containerColor = FbSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = FbBlueLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = donor.bloodGroup,
                            fontWeight = FontWeight.ExtraBold,
                            color = FbBlue,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = donor.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FbTextPrimary
                    )
                    Text(
                        text = "Location: ${donor.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FbTextSecondary
                    )
                    Text(
                        text = if (donor.isAvailable) "Ready to Donate" else "Currently Unavailable",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (donor.isAvailable) SocialRepost else FbTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!isSelf && donor.isAvailable) {
                Button(
                    onClick = onContact,
                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_contact_donor_${donor.id}")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ==========================================
// 4. Community Aid Section
// ==========================================
@Composable
private fun CommunityAidSection(
    requests: List<CommunityAidRequest>,
    selectedAidCategory: String,
    onAidCategoryChange: (String) -> Unit,
    currentUserId: String,
    onContactUser: (User) -> Unit,
    onOpenRequestAid: () -> Unit,
    onMarkFulfilled: (String) -> Unit
) {
    val categories = listOf("All Categories", "Medical Treatment", "Education / Fees", "Emergency Relief", "Food & Ration", "Winter Clothing")

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = FbSurface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedAidCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onAidCategoryChange(cat) },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FbBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = FbSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Community Mutual Aid",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FbTextPrimary
                    )
                    Text(
                        text = "Transparent solidarity and direct assistance for Yarkhoon families.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FbTextSecondary
                    )
                }
                Button(
                    onClick = onOpenRequestAid,
                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_request_aid")
                ) {
                    Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Request Aid", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (requests.isEmpty()) {
            EmptyStateCard(
                message = "No community aid requests in this category.",
                actionText = "Submit a Request",
                onAction = onOpenRequestAid
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(requests, key = { it.id }) { req ->
                    CommunityAidCard(
                        request = req,
                        isAuthor = req.requesterId == currentUserId,
                        onOfferSupport = {
                            val user = User(id = req.requesterId, name = req.requesterName, handle = req.requesterHandle)
                            onContactUser(user)
                        },
                        onMarkFulfilled = { onMarkFulfilled(req.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityAidCard(
    request: CommunityAidRequest,
    isAuthor: Boolean,
    onOfferSupport: () -> Unit,
    onMarkFulfilled: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("community_aid_card_${request.id}"),
        colors = CardDefaults.cardColors(containerColor = FbSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = FbBlueLight
                ) {
                    Text(
                        text = request.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FbBlue
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (request.verificationStatus == "verified_by_moderator") SocialRepost.copy(alpha = 0.15f) else FbBackground
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (request.verificationStatus == "verified_by_moderator") Icons.Default.CheckCircle else Icons.Default.Pending,
                            contentDescription = null,
                            tint = if (request.verificationStatus == "verified_by_moderator") SocialRepost else FbTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (request.verificationStatus == "verified_by_moderator") "Community Verified" else "Under Review",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (request.verificationStatus == "verified_by_moderator") SocialRepost else FbTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = request.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FbTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = request.description,
                style = MaterialTheme.typography.bodyMedium,
                color = FbTextPrimary
            )

            if (request.targetAmount != null && request.targetAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Target Support: PKR ${"%,.0f".format(request.targetAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FbBlue
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Beneficiary: ${request.beneficiaryName} • Location: ${request.city}",
                style = MaterialTheme.typography.bodySmall,
                color = FbTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAuthor && request.status == "active") {
                    TextButton(onClick = onMarkFulfilled) {
                        Text("Mark Resolved", color = SocialRepost)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!isAuthor && request.status == "active") {
                    Button(
                        onClick = onOfferSupport,
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_offer_aid_${request.id}")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Offer Support (Chat)", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. Legal & Emergency Guidance Section
// ==========================================
@Composable
private fun LegalEmergencySection(
    topics: List<LegalGuidanceTopic>,
    emergencyContacts: List<EmergencyContact>,
    onDial: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FbBlueLight),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = FbBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Community Legal & Emergency Disclaimer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FbBlueHover
                    )
                    Text(
                        text = "Information provided in this section is for community education and rights awareness. It does not replace certified legal counsel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FbTextSecondary
                    )
                }
            }
        }

        Text(
            text = "24/7 Verified Emergency Helplines",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = FbTextPrimary
        )

        emergencyContacts.forEach { contact ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FbSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FbTextPrimary
                        )
                        Text(
                            text = "${contact.serviceType} • ${contact.dialNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FbTextSecondary
                        )
                        Text(
                            text = contact.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = FbTextSecondary
                        )
                    }

                    Button(
                        onClick = { onDial(contact.dialNumber) },
                        colors = ButtonDefaults.buttonColors(containerColor = SocialHeart),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("dial_${contact.dialNumber}")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Citizen Rights & Legal Guidance Topics",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = FbTextPrimary
        )

        topics.forEach { topic ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                colors = CardDefaults.cardColors(containerColor = FbSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = FbBlueLight,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = FbBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = topic.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FbTextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = FbTextSecondary
                        )
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = FbDivider)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = topic.fullGuide,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FbTextPrimary,
                            lineHeight = 20.sp
                        )
                        if (topic.importantCaution.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Important Caution: ${topic.importantCaution}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. Generic Empty State Card
// ==========================================
@Composable
private fun EmptyStateCard(
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = FbTextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FbTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

// ==========================================
// 7. Dialog Composables
// ==========================================

@Composable
fun JobSeekerDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (qualifications: String, skills: List<String>, experienceYears: Int, preferredRoles: List<String>, preferredCities: List<String>, summary: String, contactPreference: String, isPhonePublic: Boolean, phoneNumber: String?) -> Unit
) {
    var role by remember { mutableStateOf("") }
    var skillsInput by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }
    var experienceYears by remember { mutableStateOf("1") }
    var citiesInput by remember { mutableStateOf("Chitral, Islamabad") }
    var bio by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Job Seeker Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Your Name: ${currentUser.name} (${currentUser.handle})", fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Desired Job Role *") },
                    placeholder = { Text("e.g. Teacher, Accountant, Developer") },
                    modifier = Modifier.fillMaxWidth().testTag("job_role_input")
                )

                OutlinedTextField(
                    value = skillsInput,
                    onValueChange = { skillsInput = it },
                    label = { Text("Skills (Comma-separated) *") },
                    placeholder = { Text("e.g. Teaching, Accounting, Excel") },
                    modifier = Modifier.fillMaxWidth().testTag("job_skills_input")
                )

                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Highest Qualification *") },
                    placeholder = { Text("e.g. BS, MBA, FSc") },
                    modifier = Modifier.fillMaxWidth().testTag("job_qualification_input")
                )

                OutlinedTextField(
                    value = experienceYears,
                    onValueChange = { experienceYears = it },
                    label = { Text("Years of Experience") },
                    modifier = Modifier.fillMaxWidth().testTag("job_experience_input")
                )

                OutlinedTextField(
                    value = citiesInput,
                    onValueChange = { citiesInput = it },
                    label = { Text("Preferred Cities (Comma-separated)") },
                    placeholder = { Text("Chitral, Islamabad") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Professional Summary") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val skills = skillsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val roles = role.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val cities = citiesInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val exp = experienceYears.toIntOrNull() ?: 0
                    if (qualification.isNotBlank() && skills.isNotEmpty()) {
                        onSubmit(qualification, skills, exp, roles, cities, bio, "in_app_chat", false, null)
                    }
                },
                modifier = Modifier.testTag("submit_job_seeker_btn"),
                enabled = role.isNotBlank() && skillsInput.isNotBlank() && qualification.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PostJobVacancyDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, companyName: String, city: String, location: String, jobType: String, salaryRange: String?, requiredSkills: List<String>, description: String, applicationInstructions: String, contactEmail: String?, contactPhone: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Chitral") }
    var salaryRange by remember { mutableStateOf("") }
    var skillsInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Job Vacancy", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job Title *") },
                    placeholder = { Text("e.g. Science Teacher") },
                    modifier = Modifier.fillMaxWidth().testTag("job_title_input")
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Organization *") },
                    modifier = Modifier.fillMaxWidth().testTag("job_company_input")
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = salaryRange,
                    onValueChange = { salaryRange = it },
                    label = { Text("Salary Range (Optional)") },
                    placeholder = { Text("e.g. PKR 50,000 - 70,000") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = skillsInput,
                    onValueChange = { skillsInput = it },
                    label = { Text("Required Skills (Comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Job Description *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Application Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val skills = skillsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (title.isNotBlank() && company.isNotBlank() && description.isNotBlank()) {
                        onSubmit(title, company, city, city, "Full-time", salaryRange.ifBlank { null }, skills, description, instructions, null, null)
                    }
                },
                modifier = Modifier.testTag("submit_job_vacancy_btn"),
                enabled = title.isNotBlank() && company.isNotBlank() && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
            ) {
                Text("Publish Vacancy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RegisterBloodDonorDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (bloodGroup: String, city: String, area: String, isAvailable: Boolean, lastDonationDate: String?, contactPreference: String, isPhonePublic: Boolean, phone: String?) -> Unit
) {
    var bloodGroup by remember { mutableStateOf("O+") }
    var city by remember { mutableStateOf("Chitral") }
    var isAvailable by remember { mutableStateOf(true) }
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register as Blood Donor", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Donor: ${currentUser.name}", style = MaterialTheme.typography.bodyMedium)

                Text("Select Blood Group:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bloodGroups.forEach { group ->
                        FilterChip(
                            selected = bloodGroup == group,
                            onClick = { bloodGroup = group },
                            label = { Text(group) }
                        )
                    }
                }

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Current City / Town *") },
                    modifier = Modifier.fillMaxWidth().testTag("donor_city_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Available to donate:")
                    Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                }

                Text(
                    text = "Privacy: Your phone number is not exposed publicly. Community members contact you securely via In-App Chat.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FbTextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (city.isNotBlank()) onSubmit(bloodGroup, city, "", isAvailable, null, "in_app_chat", false, null)
                },
                modifier = Modifier.testTag("submit_register_donor_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = SocialHeart)
            ) {
                Text("Register Donor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RequestBloodDialog(
    onDismiss: () -> Unit,
    onSubmit: (patientName: String, bloodGroup: String, hospitalName: String, city: String, requiredUnits: Int, urgency: String, contactPhone: String, notes: String) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("B+") }
    var hospital by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Chitral") }
    var units by remember { mutableStateOf("1") }
    var contactPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Urgent Blood Request", fontWeight = FontWeight.Bold, color = SocialHeart) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("request_patient_name")
                )

                Text("Blood Group Needed:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bloodGroups.forEach { group ->
                        FilterChip(
                            selected = bloodGroup == group,
                            onClick = { bloodGroup = group },
                            label = { Text(group) }
                        )
                    }
                }

                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("Hospital / Medical Center *") },
                    placeholder = { Text("e.g. DHQ Hospital Chitral") },
                    modifier = Modifier.fillMaxWidth().testTag("request_hospital")
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it },
                    label = { Text("Pints / Units Needed") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Attendant Phone *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Special Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = units.toIntOrNull() ?: 1
                    if (patientName.isNotBlank() && hospital.isNotBlank() && contactPhone.isNotBlank()) {
                        onSubmit(patientName, bloodGroup, hospital, city, u, "URGENT", contactPhone, notes)
                    }
                },
                modifier = Modifier.testTag("submit_request_blood_btn"),
                enabled = patientName.isNotBlank() && hospital.isNotBlank() && contactPhone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SocialHeart)
            ) {
                Text("Broadcast Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RequestCommunityAidDialog(
    onDismiss: () -> Unit,
    onSubmit: (beneficiaryName: String, category: String, title: String, description: String, city: String, location: String, assistanceType: String, targetAmount: Double?, contactMethod: String) -> Unit
) {
    var beneficiaryName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Medical Treatment") }
    var description by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Yarkhoon") }
    val categories = listOf("Medical Treatment", "Education / Fees", "Emergency Relief", "Food & Ration", "Winter Clothing")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Community Aid", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = beneficiaryName,
                    onValueChange = { beneficiaryName = it },
                    label = { Text("Beneficiary / Family Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Request Title *") },
                    placeholder = { Text("e.g. Winter Clothing for Family") },
                    modifier = Modifier.fillMaxWidth().testTag("aid_title_input")
                )

                Text("Category:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe Need & Background *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("aid_desc_input")
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Estimated Support in PKR (Optional)") },
                    placeholder = { Text("e.g. 25000") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Village / Town *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = targetAmount.toDoubleOrNull()
                    if (title.isNotBlank() && description.isNotBlank() && beneficiaryName.isNotBlank()) {
                        onSubmit(beneficiaryName, category, title, description, city, city, "Financial", amount, "in_app_message")
                    }
                },
                modifier = Modifier.testTag("submit_aid_request_btn"),
                enabled = title.isNotBlank() && description.isNotBlank() && beneficiaryName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
