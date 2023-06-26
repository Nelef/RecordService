package com.example.recordmodule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.recordmodule.ui.theme.RecordModuleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecordModuleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RecordButtons()
                }
            }
        }
    }
}

@Composable
fun RecordButtons() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { /*TODO*/ }) {
            Text(text = "녹취 서비스 시작")
        }
        Button(onClick = { /*TODO*/ }) {
            Text(text = "녹취 서비스 종료")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RecordModuleTheme {
        RecordButtons()
    }
}