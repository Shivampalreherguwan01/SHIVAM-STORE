package com.shivam.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ShivamStoreApp()
            }
        }
    }
}

data class StoreApp(
    val name: String,
    val description: String,
    val rating: Double,
    val installs: Long,
    val version: String,
    val category: String
)

private enum class AuthPage {
    GOOGLE,
    PROFILE,
    STORE
}

@Composable
fun ShivamStoreApp() {

    val context = LocalContext.current
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val credentialManager = remember {
        CredentialManager.create(context)
    }

    val scope = rememberCoroutineScope()

    var page by remember {
        mutableStateOf(
            if (auth.currentUser != null) {
                AuthPage.STORE
            } else {
                AuthPage.GOOGLE
            }
        )
    }

    var name by remember {
        mutableStateOf("")
    }

    var age by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    fun checkUserProfile() {

        val user = auth.currentUser ?: return

        loading = true

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                loading = false

                if (document.exists()) {

                    name = document.getString("name")
                        ?: user.displayName
                        ?: "User"

                    age = document.getLong("age")
                        ?.toString()
                        ?: ""

                    page = AuthPage.STORE

                } else {

                    name = user.displayName ?: ""
                    age = ""
                    page = AuthPage.PROFILE
                }
            }
            .addOnFailureListener {

                loading = false
                message = it.message
                    ?: "Profile check failed."
            }
    }

    LaunchedEffect(auth.currentUser?.uid) {

        if (auth.currentUser != null) {
            checkUserProfile()
        }
    }

    fun startGoogleLogin() {

        loading = true
        message = ""

        scope.launch {

            try {

                val googleIdOption =
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                            context.getString(
                                com.shivam.store.R.string.default_web_client_id
                            )
                        )
                        .setAutoSelectEnabled(false)
                        .build()

                val request =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(
                            googleIdOption
                        )
                        .build()

                val result =
                    credentialManager.getCredential(
                        context,
                        request
                    )

                val credential =
                    result.credential

                if (
                    credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {

                    val googleCredential =
                        try {

                            GoogleIdTokenCredential
                                .createFrom(
                                    credential.data
                                )

                        } catch (
                            e: GoogleIdTokenParsingException
                        ) {

                            loading = false
                            message =
                                "Google account data could not be read."
                            return@launch
                        }

                    val firebaseCredential =
                        GoogleAuthProvider
                            .getCredential(
                                googleCredential.idToken,
                                null
                            )

                    auth.signInWithCredential(
                        firebaseCredential
                    )
                        .addOnSuccessListener {

                            loading = false
                            checkUserProfile()
                        }
                        .addOnFailureListener {

                            loading = false
                            message =
                                it.message
                                    ?: "Google login failed."
                        }

                } else {

                    loading = false
                    message =
                        "Please select a Google account."
                }

            } catch (e: Exception) {

                loading = false

                message =
                    e.message
                        ?: "Google sign-in cancelled or failed."
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0F14)
    ) {

        when (page) {

            AuthPage.GOOGLE -> {

                GoogleLoginScreen(
                    loading = loading,
                    message = message,
                    onGoogleLogin = {
                        startGoogleLogin()
                    }
                )
            }

            AuthPage.PROFILE -> {

                ProfileScreen(
                    name = name,
                    age = age,
                    onNameChange = {
                        name = it
                        message = ""
                    },
                    onAgeChange = {
                        age = it
                        message = ""
                    },
                    loading = loading,
                    message = message,
                    onSave = {

                        val user = auth.currentUser

                        if (user == null) {
                            page = AuthPage.GOOGLE
                            return@ProfileScreen
                        }

                        val cleanName = name.trim()
                        val ageNumber = age.toIntOrNull()

                        if (cleanName.length < 2) {
                            message = "Enter your name."
                            return@ProfileScreen
                        }

                        if (
                            ageNumber == null ||
                            ageNumber < 1 ||
                            ageNumber > 120
                        ) {
                            message = "Enter a valid age."
                            return@ProfileScreen
                        }

                        loading = true
                        message = ""

                        val profile =
                            hashMapOf(
                                "uid" to user.uid,
                                "name" to cleanName,
                                "age" to ageNumber,
                                "email" to (
                                    user.email ?: ""
                                ),
                                "photoUrl" to (
                                    user.photoUrl
                                        ?.toString() ?: ""
                                ),
                                "provider" to "google",
                                "createdAt" to
                                    System.currentTimeMillis()
                            )

                        firestore.collection("users")
                            .document(user.uid)
                            .set(profile)
                            .addOnSuccessListener {

                                loading = false
                                page = AuthPage.STORE
                            }
                            .addOnFailureListener {

                                loading = false
                                message =
                                    it.message
                                        ?: "Could not save profile."
                            }
                    }
                )
            }

            AuthPage.STORE -> {

                StoreScreen(
                    userName = name.ifBlank {
                        auth.currentUser
                            ?.displayName
                            ?: "User"
                    },
                    email = auth.currentUser?.email ?: "",
                    onLogout = {

                        auth.signOut()

                        name = ""
                        age = ""
                        message = ""
                        loading = false

                        page = AuthPage.GOOGLE
                    }
                )
            }
        }
    }
}

@Composable
private fun GoogleLoginScreen(
    loading: Boolean,
    message: String,
    onGoogleLogin: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "SHIVAM STORE",
            color = Color.White,
            fontSize = 30.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Login / Signup",
            color = Color(0xFF7C9CFF),
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF151A23)
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Continue with Google",
                    color = Color.White,
                    fontSize = 20.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Use your Google account to login or create your SHIVAM STORE account.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Button(
                    onClick = onGoogleLogin,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Text(
                        if (loading)
                            "CONNECTING..."
                        else
                            "CONTINUE WITH GOOGLE"
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (message.isNotBlank()) {

            Text(
                text = message,
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text =
                "By continuing, you agree to use SHIVAM STORE responsibly.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ProfileScreen(
    name: String,
    age: String,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    loading: Boolean,
    message: String,
    onSave: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Create Profile",
            color = Color.White,
            fontSize = 28.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text =
                "Complete your SHIVAM STORE profile.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Name")
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        OutlinedTextField(
            value = age,
            onValueChange = {
                if (
                    it.length <= 3 &&
                    it.all(Char::isDigit)
                ) {
                    onAgeChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Age")
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = onSave,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {

            Text(
                if (loading)
                    "SAVING..."
                else
                    "CONTINUE"
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        if (message.isNotBlank()) {

            Text(
                text = message,
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StoreScreen(
    userName: String,
    email: String,
    onLogout: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "SHIVAM",
            color = Color.White,
            fontSize = 26.sp
        )

        Text(
            text = "STORE",
            color = Color(0xFF7C9CFF),
            fontSize = 15.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF151A23)
            )
        ) {

            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Text(
                    text = "Welcome, $userName",
                    color = Color.White,
                    fontSize = 22.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = email,
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Your account is active.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF151A23)
            )
        ) {

            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Text(
                    text = "Apps",
                    color = Color.White,
                    fontSize = 20.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Apps published by developers will appear here.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LOG OUT")
        }
    }
}
