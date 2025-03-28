package com.github.kdejaeger.valuefromjsonwidget

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.github.kdejaeger.valuefromjsonwidget.FetchDataWorker.PREFS_NAME
import com.github.kdejaeger.valuefromjsonwidget.ui.theme.ValueFromJsonWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ValueFromJsonWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ConfigWidget(
                        modifier = Modifier.padding(innerPadding),
                        context = applicationContext
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigWidget(modifier: Modifier = Modifier, context: Context) {
    // Get saved URL and JSON key from SharedPreferences
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedApiUrl = sharedPreferences.getString("apiUrl", "https://api.airgradient.com/public/api/v1/world/locations/154803/measures/current") ?: ""
    val savedJsonKey = sharedPreferences.getString("json_key", "pm02") ?: ""
    val savedClickUrl = sharedPreferences.getString("clickUrl", "https://app.airgradient.com") ?: ""

    val apiUrlState = remember { mutableStateOf(savedApiUrl) }
    val jsonKeyState = remember { mutableStateOf(savedJsonKey) }
    val clickUrlState = remember { mutableStateOf(savedClickUrl) }

    Column(modifier = modifier.padding(16.dp)) {

        Text("Api URL:")
        BasicTextField(
            value = apiUrlState.value,
            onValueChange = { apiUrlState.value = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("JSON Key:")
        BasicTextField(
            value = jsonKeyState.value,
            onValueChange = { jsonKeyState.value = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Click URL:")
        BasicTextField(
            value = clickUrlState.value,
            onValueChange = { clickUrlState.value = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to trigger data fetch
        Button(
            onClick = {
                // Save the URL and JSON Key in SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("apiUrl", apiUrlState.value)
                editor.putString("json_key", jsonKeyState.value)
                editor.putString("clickUrl", clickUrlState.value)
                editor.apply()

                // Trigger the worker with the updated URL and JSON Key
                triggerFetchDataWorker(apiUrlState.value, jsonKeyState.value, clickUrlState.value)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save settings")
        }
    }
}

// Function to trigger the FetchDataWorker with the provided URL and JSON Key
fun triggerFetchDataWorker(apiUrl: String, jsonKey: String, clickUrl: String) {
    val inputData = Data.Builder()
        .putString("apiUrl", apiUrl)
        .putString("json_key", jsonKey)
        .putString("clickUrl", clickUrl)
        .build()

    val fetchDataRequest = OneTimeWorkRequest.Builder(FetchDataWorker::class.java)
        .setInputData(inputData)
        .build()

    WorkManager.getInstance().enqueue(fetchDataRequest)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ValueFromJsonWidgetTheme {
        ConfigWidget(context = LocalContext.current)
    }
}
