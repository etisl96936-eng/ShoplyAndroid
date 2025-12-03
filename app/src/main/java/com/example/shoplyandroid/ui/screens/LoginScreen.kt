package com.example.shoplyandroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shoplyandroid.ui.components.ShoplyMainButton

@Composable
fun LoginScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShoplyMainButton(
            text = "התחברי",
            onClick = {
                // TODO: כאן תבוא לוגיקת ההתחברות / ניווט
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ShoplyMainButton(
            text = "הרשמה",
            onClick = {
                // TODO: ניווט למסך הרשמה
            }
        )
    }
}
