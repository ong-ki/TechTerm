package com.techterms.app.ui.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.techterms.app.ui.theme.TechTermsExtendedColors

private val COMMON_KEYWORDS = setOf(
    // Kotlin / JS / Python
    "fun", "val", "var", "class", "interface", "object", "data", "override", "suspend",
    "return", "if", "else", "when", "while", "for", "in", "import", "private", "const",
    "let", "async", "await", "export", "default", "throw", "new", "def", "print", "true", "false", "null",
    // SQL
    "SELECT", "FROM", "WHERE", "INNER", "LEFT", "JOIN", "ON", "ORDER", "BY", "ASC", "DESC",
    "GROUP", "CREATE", "TABLE", "INDEX", "ALTER", "ADD", "COLUMN", "UPDATE", "SET",
    "BEGIN", "TRANSACTION", "COMMIT", "AND", "OR", "NOT", "NULL", "PRIMARY", "KEY", "REFERENCES", "EXPLAIN", "QUERY", "PLAN",
    // HTTP / DevOps / CLI
    "GET", "POST", "PUT", "PATCH", "DELETE", "HTTP/1.1",
    "FROM", "WORKDIR", "COPY", "RUN", "CMD",
    "git", "docker", "kubectl", "sudo", "curl", "grep", "chmod", "ssh", "tar", "sed", "export", "resource", "syntax", "message", "query"
)

private val TOKEN_REGEX = Regex(
    """("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\b\d+(?:\.\d+)?\b|\b[A-Za-z_][A-Za-z0-9_/.-]*\b)"""
)

fun highlightCodeSnippet(
    code: String,
    colors: TechTermsExtendedColors,
    baseColor: Color
): AnnotatedString = buildAnnotatedString {
    val lines = code.lines()
    lines.forEachIndexed { index, line ->
        val commentStart = findLineCommentStart(line)
        val codePart = if (commentStart >= 0) line.substring(0, commentStart) else line
        val commentPart = if (commentStart >= 0) line.substring(commentStart) else ""

        if (codePart.isNotEmpty()) {
            var cursor = 0
            for (match in TOKEN_REGEX.findAll(codePart)) {
                val range = match.range
                if (range.first > cursor) {
                    pushStyle(SpanStyle(color = baseColor))
                    append(codePart.substring(cursor, range.first))
                    pop()
                }
                val token = match.value
                val style = when {
                    (token.startsWith("\"") && token.endsWith("\"")) ||
                        (token.startsWith("'") && token.endsWith("'")) ->
                        SpanStyle(color = colors.codeString)

                    token.firstOrNull()?.isDigit() == true ->
                        SpanStyle(color = colors.codeNumber)

                    token in COMMON_KEYWORDS ->
                        SpanStyle(color = colors.codeKeyword, fontWeight = FontWeight.Medium)

                    else ->
                        SpanStyle(color = baseColor)
                }
                pushStyle(style)
                append(token)
                pop()
                cursor = range.last + 1
            }
            if (cursor < codePart.length) {
                pushStyle(SpanStyle(color = baseColor))
                append(codePart.substring(cursor))
                pop()
            }
        }

        if (commentPart.isNotEmpty()) {
            pushStyle(SpanStyle(color = colors.codeComment, fontStyle = FontStyle.Italic))
            append(commentPart)
            pop()
        }

        if (index < lines.lastIndex) {
            append('\n')
        }
    }
}

private fun findLineCommentStart(line: String): Int {
    var inDouble = false
    var inSingle = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        val prev = if (i > 0) line[i - 1] else '\u0000'
        when {
            c == '"' && !inSingle && prev != '\\' -> inDouble = !inDouble
            c == '\'' && !inDouble && prev != '\\' -> inSingle = !inSingle
            !inDouble && !inSingle -> {
                if (c == '#' && (i == 0 || line[i - 1].isWhitespace())) return i
                if (c == '/' && i + 1 < line.length && line[i + 1] == '/' &&
                    (i == 0 || line[i - 1].isWhitespace())
                ) {
                    return i
                }
                if (c == '-' && i + 1 < line.length && line[i + 1] == '-' &&
                    (i == 0 || line[i - 1].isWhitespace()) &&
                    (i + 2 >= line.length || line[i + 2].isWhitespace())
                ) {
                    return i
                }
            }
        }
        i++
    }
    return -1
}
