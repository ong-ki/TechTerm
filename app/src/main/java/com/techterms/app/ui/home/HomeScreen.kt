package com.techterms.app.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techterms.app.R
import com.techterms.app.data.db.Term
import com.techterms.app.ui.detail.highlightCodeSnippet
import com.techterms.app.ui.theme.CodeMonoFontFamily
import com.techterms.app.ui.theme.EnglishSansFontFamily
import com.techterms.app.ui.theme.NotoSansMyanmarFontFamily
import com.techterms.app.ui.theme.TechTermsThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    onBrowse: (BrowseRequest) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val isScrolledDown by remember { derivedStateOf { scrollState.value > 0 } }

    BackHandler(enabled = isScrolledDown) {
        coroutineScope.launch { scrollState.animateScrollTo(0) }
    }

    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SlimTopHeader(
                title = stringResource(R.string.app_name),
                dateLabel = todayLabel,
                onSearchClick = { onBrowse(BrowseRequest()) }
            )

            // Section 0: Category wave graph
            CategoryWaveSection(stats = stats, onBrowse = onBrowse)

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
                            seedFailed = uiState.seedFailed,
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
        }
    }
}

@Composable
private fun SlimTopHeader(
    title: String,
    dateLabel: String,
    onSearchClick: () -> Unit
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.home_search_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
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
    seedFailed: Boolean = false,
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
                text = stringResource(
                    if (seedFailed) R.string.home_seed_error else R.string.home_empty_terms
                ),
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
                        fontFamily = TechTermsThemeTokens.termFontFamily,
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
                    lineHeight = 20.sp
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
