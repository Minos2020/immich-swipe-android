package com.example.immichswipe.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.immichswipe.core.AppTheme
import com.example.immichswipe.core.IconPosition
import com.example.immichswipe.core.PlaybackBehavior

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
            // SECTION APPARENCE
            SettingsSection(title = "Apparence", icon = Icons.Default.Palette) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Thème de l'application",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeButton(
                            text = "Système",
                            icon = Icons.Default.SettingsSuggest,
                            selected = uiState.themeMode == AppTheme.SYSTEM,
                            onClick = { viewModel.setThemeMode(AppTheme.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeButton(
                            text = "Clair",
                            icon = Icons.Default.LightMode,
                            selected = uiState.themeMode == AppTheme.LIGHT,
                            onClick = { viewModel.setThemeMode(AppTheme.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeButton(
                            text = "Sombre",
                            icon = Icons.Default.DarkMode,
                            selected = uiState.themeMode == AppTheme.DARK,
                            onClick = { viewModel.setThemeMode(AppTheme.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SECTION INTERACTION
            SettingsSection(title = "Interaction", icon = Icons.Default.TouchApp) {
                Column {
                    // Inversion du swipe
                    SettingsToggleItem(
                        title = "Inverser le sens du swipe",
                        subtitle = "A gauche pour garder, à droite pour supprimer",
                        checked = uiState.isSwipeInverted,
                        onCheckedChange = { viewModel.setSwipeInverted(it) },
                        icon = Icons.Default.SwapHoriz
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    // Position icône plein écran
                    IconPositionPicker(
                        title = "Position du bouton Plein Écran",
                        selectedPosition = uiState.fullscreenButtonPosition,
                        onPositionSelected = { viewModel.setFullscreenButtonPosition(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    // Position icône Immich
                    IconPositionPicker(
                        title = "Position du bouton Ouvrir dans Immich",
                        selectedPosition = uiState.immichButtonPosition,
                        onPositionSelected = { viewModel.setImmichButtonPosition(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    // Comportement vidéo
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Comportement vidéo",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Gérer l'interaction avec les autres sons de l'appareil lors de la lecture d'une vidéo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(Modifier.selectableGroup()) {
                            PlaybackOption(
                                text = "Couper tous les autres sons",
                                selected = uiState.playbackBehavior == PlaybackBehavior.PAUSE_OTHERS,
                                onClick = { viewModel.setPlaybackBehavior(PlaybackBehavior.PAUSE_OTHERS) }
                            )
                            PlaybackOption(
                                text = "Jouer par dessus les autres sons",
                                selected = uiState.playbackBehavior == PlaybackBehavior.IGNORE,
                                onClick = { viewModel.setPlaybackBehavior(PlaybackBehavior.IGNORE) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SECTION COMPTE
            SettingsSection(title = "Compte", icon = Icons.Default.Person) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(text = uiState.userName, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Connecté au serveur Immich", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Se déconnecter")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
}

@Composable
fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun IconPositionPicker(
    title: String,
    selectedPosition: IconPosition,
    onPositionSelected: (IconPosition) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CornerButton(
                    text = "Haut-Gauche",
                    selected = selectedPosition == IconPosition.TOP_LEFT,
                    onClick = { onPositionSelected(IconPosition.TOP_LEFT) },
                    modifier = Modifier.weight(1f)
                )
                CornerButton(
                    text = "Haut-Droite",
                    selected = selectedPosition == IconPosition.TOP_RIGHT,
                    onClick = { onPositionSelected(IconPosition.TOP_RIGHT) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CornerButton(
                    text = "Bas-Gauche",
                    selected = selectedPosition == IconPosition.BOTTOM_LEFT,
                    onClick = { onPositionSelected(IconPosition.BOTTOM_LEFT) },
                    modifier = Modifier.weight(1f)
                )
                CornerButton(
                    text = "Bas-Droite",
                    selected = selectedPosition == IconPosition.BOTTOM_RIGHT,
                    onClick = { onPositionSelected(IconPosition.BOTTOM_RIGHT) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CornerButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun ThemeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun PlaybackOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // null car le clic est géré par la Row
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
