package com.shivam.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val rating: String
)

@Composable
fun ShivamStoreApp() {

    var searchText by remember { mutableStateOf("") }

    val apps = listOf(
        StoreApp(
            "NIYANTRAK",
            "Remote control and safety app",
            "4.8"
        ),
        StoreApp(
            "MOHINI",
            "AI powered video creation",
            "4.7"
        ),
        StoreApp(
            "SAARTHI AI",
            "Your personal AI assistant",
            "4.9"
        )
    )

    val categories = listOf(
        "🤖 AI",
        "🛠 Tools",
        "📚 Education",
        "🎨 Creativity",
        "🔒 Security",
        "🎮 Games"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0F14)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "SHIVAM",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "STORE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C9CFF)
                    )
                }

                Text(
                    text = "⋮",
                    fontSize = 30.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search apps...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Featured
            Text(
                text = "Featured",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF182132)
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "NIYANTRAK",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Remote control & safety",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3157D5)
                    ) {
                        Text(
                            text = "VIEW APP",
                            modifier = Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 10.dp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Categories
            Text(
                text = "Categories",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {

                items(categories) { category ->

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF171D27)
                    ) {

                        Text(
                            text = category,
                            modifier = Modifier.padding(
                                horizontal = 15.dp,
                                vertical = 11.dp
                            ),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Popular Apps
            Text(
                text = "Popular Apps",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            apps.forEach { app ->

                AppCard(app)

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom spacing for future navigation
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun AppCard(app: StoreApp) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151A23)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        color = Color(0xFF26334D),
                        shape = RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = app.name.take(1),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = app.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = app.description,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFC107)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = app.rating,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "↓",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
