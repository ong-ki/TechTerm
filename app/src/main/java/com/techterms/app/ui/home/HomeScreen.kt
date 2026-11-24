package com.techterms.app.ui.home

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techterms.app.R
import com.techterms.app.data.db.Term
import com.techterms.app.notification.helper.NotificationHelper
import com.techterms.app.ui.detail.highlightCodeSnippet
import com.techterms.app.ui.theme.CodeMonoFontFamily
import com.techterms.app.ui.theme.EnglishSansFontFamily
import com.techterms.app.ui.theme.NotoSansMyanmarFontFamily
import com.techterms.app.ui.theme.TechTermsThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CompactCardShape = RoundedCornerShape(16.dp)
private val CompactMyanmarStyle = androidx.compose.ui.text.TextStyle(
    fontFamily = NotoSansMyanmarFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 21.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTermClick: (Term) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationEnabled(true)
        } else {
            viewModel.setNotificationEnabled(false)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.home_permission_denied)
                )
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SlimTopHeader(
                title = stringResource(R.string.app_name),
                dateLabel = todayLabel
            )

            // Section 1: Term of the Day
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(text = stringResource(R.string.home_term_of_the_day))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                } else {
                    var visible by remember(uiState.termOfTheDay?.id) { mutableStateOf(false) }
                    LaunchedEffect(uiState.termOfTheDay?.id) {
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 250)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 250),
                                initialOffsetY = { it / 12 }
                            )
                    ) {
                        CompactTermOfTheDayCard(
                            term = uiState.termOfTheDay,
                            onClick = onTermClick
                        )
                    }
                }
            }

            // Section 2: Developer snippet (hidden when no snippet exists)
            val snippetTerm = uiState.snippetTerm
            if (!uiState.isLoading && snippetTerm != null && !snippetTerm.codeSnippet.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(text = stringResource(R.string.home_section_developer_snippet))
                    CompactDeveloperSnippetCard(
                        term = snippetTerm,
                        showTermTitle = snippetTerm.id != uiState.termOfTheDay?.id,
                        onClick = { onTermClick(snippetTerm) }
                    )
                }
            }

            // Section 3: Notification
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(text = stringResource(R.string.home_section_notification))
                SlimNotificationCard(
                    enabled = uiState.notificationsEnabled,
                    hour = uiState.notificationHour,
                    minute = uiState.notificationMinute,
                    devTermsOnly = uiState.devTermsOnly,
                    onToggle = { checked ->
                        if (!checked) {
                            viewModel.setNotificationEnabled(false)
                        } else if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NotificationHelper.hasNotificationPermission(context)
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setNotificationEnabled(true)
                        }
                    },
                    onTimeClick = { showTimePicker = true },
                    onDevTermsOnlyToggle = viewModel::setDevTermsOnly
                )
            }
        }
    }

    if (showTimePicker) {
        NotificationTimePickerDialog(
            initialHour = uiState.notificationHour,
            initialMinute = uiState.notificationMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                showTimePicker = false
                viewModel.setNotificationTime(hour, minute)
            }
        )
    }
}

@Composable
private fun SlimTopHeader(
    title: String,
    dateLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.ENGLISH),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp)
    )
}

@Composable
private fun CompactTermOfTheDayCard(
    term: Term?,
    onClick: (Term) -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val zeroElevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

    if (term == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CompactCardShape,
            colors = cardColors,
            border = cardBorder,
            elevation = zeroElevation
        ) {
            Text(
                text = stringResource(R.string.home_empty_terms),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
            )
        }
        return
    }

    Card(
        onClick = { onClick(term) },
        modifier = Modifier.fillMaxWidth(),
        shape = CompactCardShape,
        colors = cardColors,
        border = cardBorder,
        elevation = zeroElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = term.term,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = EnglishSansFontFamily,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                CompactOutlinePill(
                    label = term.category,
                    accent = true
                )
                CompactOutlinePill(
                    label = term.difficulty,
                    accent = false
                )
            }

            Text(
                text = term.meaningMy,
                style = CompactMyanmarStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = term.meaningEn,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactOutlinePill(
    label: String,
    accent: Boolean
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (accent) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (accent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 104.dp)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CompactDeveloperSnippetCard(
    term: Term,
    showTermTitle: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = TechTermsThemeTokens.extendedColors
    val baseTextColor = MaterialTheme.colorScheme.onSurface
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember(term.id) { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500L)
            copied = false
        }
    }

    val fullSnippet = term.codeSnippet.orEmpty()
    val previewSnippet = remember(fullSnippet) {
        fullSnippet.lines().take(4).joinToString("\n")
    }
    val highlighted = remember(previewSnippet, extendedColors, baseTextColor) {
        highlightCodeSnippet(
            code = previewSnippet,
            colors = extendedColors,
            baseColor = baseTextColor
        )
    }
    val languageLabel = term.codeLanguage?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.detail_default_code_lang)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CompactCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showTermTitle) {
                Text(
                    text = term.term,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            term.usageNote?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = note,
                    style = CompactMyanmarStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = extendedColors.codeSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = languageLabel.lowercase(Locale.ENGLISH),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = CodeMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(fullSnippet))
                                    copied = true
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.detail_copied_toast),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Rounded.Check else Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(
                                    if (copied) R.string.detail_copied_cd else R.string.detail_copy_code_cd
                                ),
                                tint = if (copied) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = highlighted,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = CodeMonoFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            ),
                            maxLines = 4,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlimNotificationCard(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    devTermsOnly: Boolean,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
    onDevTermsOnlyToggle: (Boolean) -> Unit
) {
    val formattedTime = remember(hour, minute) {
        LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CompactCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 60.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (enabled) {
                        Icons.Outlined.Notifications
                    } else {
                        Icons.Outlined.NotificationsOff
                    },
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_daily_notification),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (enabled) {
                            formattedTime
                        } else {
                            stringResource(R.string.home_notification_time_off, formattedTime)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onTimeClick)
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDevTermsOnlyToggle(!devTermsOnly) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = devTermsOnly,
                    onCheckedChange = onDevTermsOnlyToggle,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
                Text(
                    text = stringResource(R.string.home_dev_terms_only),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CompactCardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dialog_time_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirm(timePickerState.hour, timePickerState.minute)
                        },
                        modifier = Modifier.sizeIn(minHeight = 48.dp)
                    ) {
                        Text(text = stringResource(R.string.action_ok))
                    }
                }
            }
        }
    }
}
