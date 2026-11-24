package com.techterms.app.ui.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techterms.app.R
import com.techterms.app.data.db.Term
import com.techterms.app.ui.theme.CodeMonoFontFamily
import com.techterms.app.ui.theme.EnglishSansFontFamily
import com.techterms.app.ui.theme.MyanmarBodyLarge
import com.techterms.app.ui.theme.MyanmarBodyMedium
import com.techterms.app.ui.theme.TechTermsThemeTokens
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TermDetailBottomSheet(
    term: Term,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copiedTerm by remember(term.id) { mutableStateOf(false) }

    LaunchedEffect(copiedTerm) {
        if (copiedTerm) {
            delay(1_500L)
            copiedTerm = false
        }
    }

    val isCodeLike = remember(term.term) { isCodeLikeTerm(term.term) }
    val extendedColors = TechTermsThemeTokens.extendedColors
    val difficultyDotColor = when (term.difficulty.lowercase(Locale.ENGLISH)) {
        "beginner" -> extendedColors.difficultyBeginner
        "intermediate" -> extendedColors.difficultyIntermediate
        "advanced" -> extendedColors.difficultyAdvanced
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outline
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(top = 4.dp, bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = term.term,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = if (isCodeLike) CodeMonoFontFamily else EnglishSansFontFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString("${term.term}\n${term.meaningMy}")
                        )
                        copiedTerm = true
                        Toast.makeText(
                            context,
                            context.getString(R.string.detail_copied_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = if (copiedTerm) Icons.Rounded.Check else Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(
                            if (copiedTerm) R.string.detail_copied_cd else R.string.detail_copy_term_cd
                        ),
                        tint = if (copiedTerm) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TermOutlinePill(label = term.category)
                TermOutlinePill(
                    label = term.difficulty,
                    dotColor = difficultyDotColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.detail_section_meaning_my),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = term.meaningMy,
                style = MyanmarBodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.detail_section_meaning_en),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = term.meaningEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.detail_section_example),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExampleQuoteBlock(example = term.example)

            val usageNote = term.usageNote?.takeIf { it.isNotBlank() }
            if (usageNote != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.detail_section_usage_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = usageNote,
                    style = MyanmarBodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val codeSnippet = term.codeSnippet?.takeIf { it.isNotBlank() }
            if (codeSnippet != null) {
                Spacer(modifier = Modifier.height(20.dp))
                CodeSnippetBlock(
                    code = codeSnippet,
                    language = term.codeLanguage?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.detail_default_code_lang)
                )
            }
        }
    }
}

@Composable
private fun CodeSnippetBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val extendedColors = TechTermsThemeTokens.extendedColors
    val baseTextColor = MaterialTheme.colorScheme.onSurface
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember(code) { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500L)
            copied = false
        }
    }

    val highlightedCode = remember(code, extendedColors, baseTextColor) {
        highlightCodeSnippet(
            code = code,
            colors = extendedColors,
            baseColor = baseTextColor
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = extendedColors.codeSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.lowercase(Locale.ENGLISH),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = CodeMonoFontFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(code))
                            copied = true
                            Toast.makeText(
                                context,
                                context.getString(R.string.detail_copied_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Rounded.Check else Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.detail_copy_code_cd),
                        tint = if (copied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(
                            if (copied) R.string.detail_copied_code_button else R.string.detail_copy_code_button
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = highlightedCode,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = CodeMonoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun TermOutlinePill(
    label: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color = dotColor, shape = CircleShape)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExampleQuoteBlock(
    example: String,
    modifier: Modifier = Modifier
) {
    val quoteSurface = TechTermsThemeTokens.extendedColors.quoteSurface

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = quoteSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
            Text(
                text = example,
                style = MyanmarBodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun isCodeLikeTerm(term: String): Boolean {
    val trimmed = term.trim()
    if (trimmed.startsWith(".") || trimmed.contains("/") ||
        (trimmed.contains("-") && trimmed.any { it.isUpperCase() })
    ) {
        return true
    }
    if (!trimmed.contains(" ")) {
        val uppercaseCount = trimmed.count { it.isUpperCase() }
        if (uppercaseCount >= 2) return true
        if (trimmed.all { it.isLowerCase() }) return true
        if (trimmed in setOf("Git", "Docker", "OAuth", "GraphQL", "NoSQL", "SQLite", "WebSocket", "Regex")) {
            return true
        }
    }
    return trimmed in setOf("REST API", "IP Address", "MAC Address", "Fetch API")
}
