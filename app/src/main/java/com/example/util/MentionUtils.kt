package com.example.util

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.FbBlue

object MentionUtils {
    // Regex for mentions (@username) - supports alphanumeric and underscores
    private val MENTION_REGEX = Regex("""@([a-zA-Z0-9_]+)""")

    // Regex for hashtags (#hashtag)
    private val HASHTAG_REGEX = Regex("""#([a-zA-Z0-9_\u0600-\u06FF]+)""")

    /**
     * Extracts list of mentioned usernames without the '@' prefix.
     */
    fun extractMentions(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return MENTION_REGEX.findAll(text)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .toList()
    }

    /**
     * Checks if a given username is mentioned in the text.
     */
    fun containsMention(text: String, username: String): Boolean {
        val cleanUsername = username.removePrefix("@").lowercase()
        return extractMentions(text).contains(cleanUsername)
    }

    /**
     * Builds an AnnotatedString with styling and clickable annotations
     * for both @mentions and #hashtags.
     */
    fun buildInteractiveText(
        text: String,
        mentionColor: Color = FbBlue,
        hashtagColor: Color = FbBlue,
        defaultColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            append(text)

            // Highlight and annotate mentions
            MENTION_REGEX.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val username = matchResult.groupValues[1]

                addStyle(
                    style = SpanStyle(
                        color = mentionColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start = start,
                    end = end
                )
                addStringAnnotation(
                    tag = "MENTION",
                    annotation = username,
                    start = start,
                    end = end
                )
            }

            // Highlight and annotate hashtags
            HASHTAG_REGEX.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val tag = matchResult.groupValues[1]

                addStyle(
                    style = SpanStyle(
                        color = hashtagColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = start,
                    end = end
                )
                addStringAnnotation(
                    tag = "HASHTAG",
                    annotation = tag,
                    start = start,
                    end = end
                )
            }
        }
    }
}

/**
 * Composable that displays text with clickable @mentions and #hashtags.
 */
@Composable
fun InteractivePostText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    mentionColor: Color = FbBlue,
    hashtagColor: Color = FbBlue,
    onMentionClick: ((String) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null
) {
    val defaultColor = MaterialTheme.colorScheme.onSurface
    val annotatedString = MentionUtils.buildInteractiveText(
        text = text,
        mentionColor = mentionColor,
        hashtagColor = hashtagColor,
        defaultColor = defaultColor
    )

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = defaultColor),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick?.invoke(annotation.item)
                    return@ClickableText
                }

            annotatedString.getStringAnnotations(tag = "HASHTAG", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onHashtagClick?.invoke(annotation.item)
                    return@ClickableText
                }
        }
    )
}
