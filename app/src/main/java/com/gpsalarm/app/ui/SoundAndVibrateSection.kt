package com.gpsalarm.app.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gpsalarm.app.ui.theme.arcadeColors
import com.gpsalarm.app.ui.theme.arcadeType

@Composable
fun SoundAndVibrateSection(
    soundUri: Uri?,
    onSoundUriChange: (Uri?) -> Unit,
    vibrate: Boolean,
    onVibrateChange: (Boolean) -> Unit,
) {
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            onSoundUriChange(result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI))
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (soundUri == null) "Default alarm sound" else "Custom sound selected",
                style = arcadeType.body,
                color = arcadeColors.textPrimary,
            )
            OutlinedButton(onClick = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(
                        RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    )
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundUri)
                }
                ringtonePickerLauncher.launch(intent)
            }) {
                Text("Choose")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Vibrate", style = arcadeType.body, color = arcadeColors.textPrimary)
            Switch(checked = vibrate, onCheckedChange = onVibrateChange)
        }
    }
}
