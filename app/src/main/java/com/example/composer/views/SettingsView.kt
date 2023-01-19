package com.example.composer.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class SettingsView {
    companion object {
        @Composable
        fun SettingsContent() {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceEvenly) {
                Card(modifier = Modifier.padding(all = 10.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.padding(all = 10.dp)) {
                        Icon(
                            Icons.Filled.Email, contentDescription = null,
                            modifier = Modifier
                        )
                        //vertical space between image and text
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Options",
                            color = MaterialTheme.colors.secondaryVariant,
                            style = MaterialTheme.typography.subtitle2
                        )
                    }
                }
                Card(modifier = Modifier.padding(all = 10.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.padding(all = 10.dp)) {
                        Icon(
                            Icons.Filled.Email, contentDescription = null,
                            modifier = Modifier
                        )
                        //vertical space between image and text
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Options",
                            color = MaterialTheme.colors.secondaryVariant,
                            style = MaterialTheme.typography.subtitle2
                        )
                    }
                }
            }
        }
    }
}