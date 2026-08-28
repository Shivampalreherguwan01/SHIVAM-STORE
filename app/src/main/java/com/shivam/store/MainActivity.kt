package com.shivam.store

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

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
    PHONE,
    OTP,
    PROFILE,
    STORE
}

@Composable
fun ShivamStoreApp() {

    val context = LocalContext.current
    val activity = context as Activity

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var page by remember {
        mutableStateOf(
            if (auth.currentUser != null) {
                AuthPage.STORE
            } else {
                AuthPage.PHONE
            }
        )
    }

    var phone by remember {
        mutableStateOf("")
    }

    var otp by remember {
        mutableStateOf("")
    }

    var verificationId by remember {
        mutableStateOf<String?>(null)
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

                    name = document.getString("name") ?: ""
                    age = document.getLong("age")?.toString() ?: ""

                    page = AuthPage.STORE

                } else {

                    page = AuthPage.PROFILE
                }
            }
            .addOnFailureListener {

                loading = false
                message = "Profile check failed. Please try again."
            }
    }

    LaunchedEffect(auth.currentUser?.uid) {

        if (auth.currentUser != null) {
            checkUserProfile()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0F14)
    ) {

        when (page) {

            AuthPage.PHONE -> {

                PhoneScreen(
                    phone = phone,
                    onPhoneChange = {
                        phone = it
                        message = ""
                    },
                    loading = loading,
                    message = message,
                    onSendOtp = {

                        val cleanPhone = phone.trim()

                        if (cleanPhone.length < 10) {
                            message = "Enter a valid mobile number."
                            return@PhoneScreen
                        }

                        val formattedPhone =
                            if (cleanPhone.startsWith("+")) {
                                cleanPhone
                            } else {
                                "+91$cleanPhone"
                            }

                        loading = true
                        message = ""

                        val callbacks =
                            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                                override fun onVerificationCompleted(
                                    credential: PhoneAuthCredential
                                ) {
                                    auth.signInWithCredential(credential)
                                        .addOnSuccessListener {
                                            loading = false
                                            checkUserProfile()
                                        }
                                        .addOnFailureListener {
                                            loading = false
                                            message =
                                                it.message
                                                    ?: "Verification failed."
                                        }
                                }

                                override fun onVerificationFailed(
                                    e: FirebaseException
                                ) {
                                    loading = false
                                    message =
                                        e.message
                                            ?: "OTP could not be sent."
                                }

                                override fun onCodeSent(
                                    id: String,
                                    token: PhoneAuthProvider.ForceResendingToken
                                ) {
                                    verificationId = id
                                    loading = false
                                    page = AuthPage.OTP
                                    message = "OTP sent successfully."
                                }
                            }

                        val options =
                            PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber(formattedPhone)
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(activity)
                                .setCallbacks(callbacks)
                                .build()

                        PhoneAuthProvider.verifyPhoneNumber(options)
                    }
                )
            }

            AuthPage.OTP -> {

                OtpScreen(
                    otp = otp,
                    onOtpChange = {
                        otp = it
                        message = ""
                    },
                    loading = loading,
                    message = message,
                    onVerify = {

                        val id = verificationId

                        if (id == null) {
                            message = "Please request OTP again."
                            return@OtpScreen
                        }

                        if (otp.length < 6) {
                            message = "Enter the 6-digit OTP."
                            return@OtpScreen
                        }

                        loading = true
                        message = ""

                        val credential =
                            PhoneAuthProvider.getCredential(
                                id,
                                otp
                            )

                        auth.signInWithCredential(credential)
                            .addOnSuccessListener {

                                loading = false
                                otp = ""

                                checkUserProfile()
                            }
                            .addOnFailureListener {

                                loading = false
                                message =
                                    it.message
                                        ?: "Invalid OTP."
                            }
                    },
                    onBack = {
                        otp = ""
                        verificationId = null
                        page = AuthPage.PHONE
                        message = ""
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
                            page = AuthPage.PHONE
                            return@ProfileScreen
                        }

                        val cleanName = name.trim()
                        val ageNumber = age.toIntOrNull()

                        if (cleanName.length < 2) {
                            message = "Enter your name."
                            return@ProfileScreen
                        }

                        if (ageNumber == null || ageNumber < 1 || ageNumber > 120) {
                            message = "Enter a valid age."
                            return@ProfileScreen
                        }

                        loading = true
                        message = ""

                        val profile = hashMapOf(
                            "uid" to user.uid,
                            "name" to cleanName,
                            "age" to ageNumber,
                            "phone" to (user.phoneNumber ?: ""),
                            "createdAt" to System.currentTimeMillis()
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
                    userName = name,
                    onLogout = {

                        auth.signOut()

                        phone = ""
                        otp = ""
                        verificationId = null
                        name = ""
                        age = ""
                        message = ""

                        page = AuthPage.PHONE
                    }
                )
            }
        }
    }
}

@Composable
private fun PhoneScreen(
    phone: String,
    onPhoneChange: (String) -> Unit,
    loading: Boolean,
    message: String,
    onSendOtp: () -> Unit
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Login / Signup",
            color = Color(0xFF7C9CFF),
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Mobile Number")
            },
            placeholder = {
                Text("Enter 10 digit number")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSendOtp,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (loading) "Sending OTP..." else "SEND OTP"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (message.isNotBlank()) {
            Text(
                text = message,
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "By continuing, you agree to use SHIVAM STORE responsibly.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun OtpScreen(
    otp: String,
    onOtpChange: (String) -> Unit,
    loading: Boolean,
    message: String,
    onVerify: () -> Unit,
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Verify OTP",
            color = Color.White,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Enter the 6-digit OTP sent to your mobile.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = {
                if (it.length <= 6 && it.all(Char::isDigit)) {
                    onOtpChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("OTP")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onVerify,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (loading) "VERIFYING..." else "VERIFY OTP"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onBack,
            enabled = !loading
        ) {
            Text("CHANGE NUMBER")
        }

        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "First time here? Tell us about yourself.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

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

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = age,
            onValueChange = {
                if (it.length <= 3 && it.all(Char::isDigit)) {
                    onAgeChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Age")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onSave,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (loading) "SAVING..." else "CONTINUE"
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

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
    onLogout: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(30.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your account is active.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Apps published by developers will appear here.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LOG OUT")
        }
    }
}
