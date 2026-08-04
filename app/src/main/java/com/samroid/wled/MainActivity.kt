package com.samroid.wled

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import com.samroid.wled.domain.model.TransportDevice
import com.samroid.wled.presentation.navigation.AppRoot
import com.samroid.wled.presentation.settings.AppViewModel
import com.samroid.wled.presentation.settings.Language
import com.samroid.wled.presentation.settings.ThemeMode
import com.samroid.wled.presentation.theme.WLEDTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
/**
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WLEDTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WLEDTheme {
        Greeting("Android")
    }
}
        **/

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.domain.model.TransportConnectionState
/**
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            // مجوزها داده شد
        }
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* نتیجه روشن شدن بلوتوث */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBtPermissions()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BluetoothScreen(
                        viewModel = viewModel,
                        onEnableBluetooth = { enableBluetooth() }
                    )
                }
            }
        }
    }

    private fun requestBtPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
            permissions += Manifest.permission.BLUETOOTH_SCAN
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
            permissions += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        val need = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty()) {
            permissionLauncher.launch(need.toTypedArray())
        }
    }

    private fun enableBluetooth() {
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtLauncher.launch(enableIntent)
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(
    viewModel: MainViewModel,
    onEnableBluetooth: () -> Unit
) {
    val connectionState by viewModel.transportConnectionState.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val lastResponse by viewModel.lastResponse.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // عنوان و وضعیت
        Text(
            text = "WLED Master – HC-05",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        StatusChip(connectionState)

        if (lastResponse != null) {
            Text(
                text = lastResponse!!,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // دکمه‌های کنترل
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
//            if (!viewModel.isBluetoothEnabled()) {
//                Button(onClick = onEnableBluetooth, modifier = Modifier.weight(1f)) {
//                    Text("روشن کردن بلوتوث")
//                }
//            } else {
                Button(
                    onClick = {
                        if (isScanning) viewModel.stopScan() else viewModel.startScan()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isScanning) "توقف اسکن" else "اسکن دستگاه‌ها")
                }
            //}

            if (connectionState == TransportConnectionState.CONNECTED) {
                Button(
                    onClick = { viewModel.sendPing() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("ارسال PING")
                }
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("قطع")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // لیست دستگاه‌ها
        Text("دستگاه‌های بلوتوث:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(devices, key = { it.address }) { device ->
                DeviceItem(
                    device = device,
                    transportConnectionState = connectionState,
                    onPair = { viewModel.pair(device) },
                    onConnect = { viewModel.connect(device) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // لاگ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("لاگ:", fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { viewModel.clearLog() }) {
                Text("پاک کردن", fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
        ) {
            items(log.reversed()) { line ->
                Text(
                    text = line,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}



@Composable
fun StatusChip(state: TransportConnectionState) {
    val (text, color) = when (state) {
        TransportConnectionState.DISCONNECTED -> "قطع" to Color.Gray
        TransportConnectionState.CONNECTING -> "در حال اتصال..." to Color(0xFFF9A825)
        TransportConnectionState.CONNECTED -> "متصل ✓" to Color(0xFF2E7D32)
        TransportConnectionState.ERROR -> "خطا" to Color(0xFFC62828)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "وضعیت: $text",
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceItem(
    device: BluetoothDevice,
    transportConnectionState: TransportConnectionState,
    onPair: () -> Unit,
    onConnect: () -> Unit
) {
    val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
    val busy = transportConnectionState == TransportConnectionState.CONNECTING

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(device.name ?: "نامشخص", fontWeight = FontWeight.Medium)
            Text(device.address, fontSize = 12.sp, color = Color.Gray)
            Text(
                text = if (isBonded) "جفت‌شده ✓" else "جفت‌نشده",
                fontSize = 11.sp,
                color = if (isBonded) Color(0xFF2E7D32) else Color.Gray
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isBonded) {
                    OutlinedButton(
                        onClick = onPair,
                        enabled = !busy
                    ) {
                        Text("جفت‌سازی")
                    }
                }
                Button(
                    onClick = onConnect,
                    enabled = !busy
                ) {
                    Text(if (isBonded) "اتصال" else "جفت و اتصال")
                }
            }
        }
    }
}
        **/



@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* نتیجه مجوزها */ }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* نتیجه روشن شدن بلوتوث */ }

    @SuppressLint("LocalContextConfigurationRead")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBtPermissions()

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()

            val uiState by appViewModel.uiState.collectAsStateWithLifecycle()


            val locale = when(uiState.language) {
                Language.PERSIAN -> "fa"
                Language.ENGLISH -> "en"
            }


            LaunchedEffect(locale) {

                val currentLocale =
                    resources.configuration.locales[0].language


                if (currentLocale != locale) {

                    val config = Configuration(resources.configuration)

                    config.setLocale(Locale(locale))

                    resources.updateConfiguration(
                        config,
                        resources.displayMetrics
                    )

                    recreate()
                }
            }


            CompositionLocalProvider(

                LocalLayoutDirection provides
                        if(uiState.language == Language.PERSIAN)
                            LayoutDirection.Rtl
                        else
                            LayoutDirection.Ltr

            ) {
                WLEDTheme(darkTheme = when (uiState.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                }) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppRoot(uiState)
                    }

                }


            }
        }
    }

    private fun requestBtPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
            permissions += Manifest.permission.BLUETOOTH_SCAN
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
            permissions += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        val need = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty()) {
            permissionLauncher.launch(need.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableBluetooth() {
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtLauncher.launch(enableIntent)
    }
}

@Composable
fun BluetoothScreen(
    viewModel: MainViewModel,
    onEnableBluetooth: () -> Unit
) {
    val connectionState by viewModel.transportConnectionState.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val lastResponse by viewModel.lastResponse.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "WLED Master",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        StatusChip(connectionState)

        if (!lastResponse.isNullOrBlank()) {
            Text(
                text = lastResponse.orEmpty(),
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onEnableBluetooth,
                modifier = Modifier.weight(1f)
            ) {
                Text("بلوتوث")
            }

            Button(
                onClick = {
                    if (isScanning) viewModel.stopScan() else viewModel.startScan()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isScanning) "توقف اسکن" else "اسکن")
            }
        }

        if (connectionState == TransportConnectionState.CONNECTED) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.sendPing() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("ارسال PING")
                }
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("قطع")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("دستگاه‌های بلوتوث:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(devices, key = { it.id }) { device ->
                DeviceItem(
                    device = device,
                    connectionState = connectionState,
                    onConnect = { viewModel.connect(device.id) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.log), fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { viewModel.clearLog() }) {
                Text(stringResource(R.string.delete), style = MaterialTheme.typography.bodySmall)
            }
        }

        LazyColumn(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
        ) {
            items(log.reversed()) { line ->
                Text(
                    text = line,
                    style =MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun StatusChip(state: TransportConnectionState) {
    val (text, color) = when (state) {
        TransportConnectionState.DISCONNECTED -> "قطع" to Color.Gray
        TransportConnectionState.CONNECTING -> "در حال اتصال..." to Color(0xFFF9A825)
        TransportConnectionState.CONNECTED -> "متصل ✓" to Color(0xFF2E7D32)
        TransportConnectionState.ERROR -> "خطا" to Color(0xFFC62828)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "وضعیت: $text",
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun DeviceItem(
    device: TransportDevice,
    connectionState: TransportConnectionState,
    onConnect: () -> Unit
) {
    val busy = connectionState == TransportConnectionState.CONNECTING

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(device.name, fontWeight = FontWeight.Medium)
            Text(device.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onConnect,
                enabled = !busy
            ) {
                Text(stringResource(R.string.connect))
            }
        }
    }
}