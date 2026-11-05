package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.model.UserProfile
import com.example.data.repository.FleetRepository
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.driver.DriverHomeScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyBackground
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
        val repository = FleetRepository(db)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                AppRootContent(repository = repository)
            }
        }
    }
}

@Composable
fun AppRootContent(repository: FleetRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isProfileLoading by remember { mutableStateOf(false) }

    // Admin kullanıcısı kendi aracının şoför işlemlerini yaparken şoför ekranına geçebilir
    var adminInDriverMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(currentUser?.uid) {
        val user = currentUser
        if (user != null) {
            isProfileLoading = true
            // 3 filo aracının ilk kurulumunu ve başlangıç verilerini garantiye al
            repository.bootstrapInitialData()
            val profile = repository.syncOrCreateUserProfile(
                userId = user.uid,
                email = user.email ?: "",
                name = user.displayName ?: "Kullanıcı"
            )
            userProfile = profile
            isProfileLoading = false
        } else {
            userProfile = null
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else if (isProfileLoading || userProfile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = CyanAccent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
        }
    } else {
        val profile = userProfile!!
        val isAdmin = profile.role == "ADMIN" || profile.email.equals("sh_dilek@hotmail.com", ignoreCase = true)

        if (isAdmin && !adminInDriverMode) {
            AdminDashboardScreen(
                userProfile = profile,
                repository = repository,
                onSignOut = {
                    signOutUser(context, credentialManager, scope) {
                        currentUser = null
                        userProfile = null
                    }
                },
                onSwitchToDriverMode = {
                    adminInDriverMode = true
                }
            )
        } else {
            // Şoför Ekranı (veya Admin şoför modundayken)
            if (isAdmin) {
                BackHandler {
                    adminInDriverMode = false
                }
            }

            DriverHomeScreen(
                userProfile = profile,
                repository = repository,
                onSignOut = {
                    signOutUser(context, credentialManager, scope) {
                        currentUser = null
                        userProfile = null
                    }
                },
                onNavigateToAdmin = if (isAdmin) {
                    { adminInDriverMode = false }
                } else null
            )
        }
    }
}
