package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.Post
import java.io.File

private val BrandBlue = Color(0xFF1877F2)
private val BrandBlueDark = Color(0xFF0D53B5)
private val EmeraldGreen = Color(0xFF10B981)
private val DarkSlate = Color(0xFF0F172A)
private val LightSurface = Color(0xFFF8FAFC)
private val AmberGold = Color(0xFFF59E0B)

/**
 * Web preview page generator and viewer simulating the web experience at https://yarkhoon.com/post/{postId}.
 * When a user does NOT have the Yarkhoon app installed, this web page displays the post details, media,
 * reactions preview, and a prominent banner to download the Yarkhoon app.
 */
object YarkhoonPostWebPreviewHelper {

    fun generateWebHtml(post: Post): String {
        val postUrl = "https://yarkhoon.com/post/${post.id}"
        val deepLinkScheme = "yarkhoon://post/${post.id}"
        val appDownloadUrl = "https://yarkhoon.com/download/yarkhoon-social.apk"

        val mediaHtml = if (post.mediaUrl.isNotBlank()) {
            """
            <div class="post-media">
                <img src="${post.mediaUrl}" alt="Post media" />
            </div>
            """.trimIndent()
        } else ""

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${post.authorName} on Yarkhoon: "${post.content.take(60)}"</title>
            <meta name="description" content="${post.content.replace("\"", "&quot;")}">
            <meta property="og:title" content="${post.authorName} on Yarkhoon Social">
            <meta property="og:description" content="${post.content.replace("\"", "&quot;")}">
            <meta property="og:url" content="$postUrl">
            <meta property="og:type" content="article">
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    background: #f0f2f5;
                    color: #1c1e21;
                    line-height: 1.5;
                }
                .app-banner {
                    background: linear-gradient(135deg, #1877F2, #0D53B5);
                    color: white;
                    padding: 12px 16px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    position: sticky;
                    top: 0;
                    z-index: 100;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
                }
                .app-banner-info {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                }
                .app-logo {
                    width: 36px;
                    height: 36px;
                    border-radius: 9px;
                    background: white;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: 900;
                    color: #1877F2;
                    font-size: 20px;
                }
                .app-title { font-weight: bold; font-size: 15px; }
                .app-sub { font-size: 11px; opacity: 0.85; }
                .btn-open-app {
                    background: white;
                    color: #1877F2;
                    font-weight: bold;
                    padding: 8px 16px;
                    border-radius: 20px;
                    text-decoration: none;
                    font-size: 13px;
                }
                .container {
                    max-width: 600px;
                    margin: 16px auto;
                    padding: 0 12px;
                }
                .card {
                    background: white;
                    border-radius: 12px;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                    overflow: hidden;
                    margin-bottom: 16px;
                }
                .card-header {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    padding: 16px;
                }
                .author-avatar {
                    width: 46px;
                    height: 46px;
                    border-radius: 50%;
                    object-fit: cover;
                    background: #e4e6eb;
                }
                .author-name {
                    font-weight: 700;
                    font-size: 16px;
                    color: #050505;
                }
                .author-meta {
                    font-size: 12px;
                    color: #65676b;
                }
                .post-body {
                    padding: 0 16px 16px 16px;
                    font-size: 15px;
                    color: #1c1e21;
                    white-space: pre-line;
                }
                .post-media img {
                    width: 100%;
                    max-height: 480px;
                    object-fit: cover;
                    display: block;
                }
                .card-stats {
                    padding: 12px 16px;
                    display: flex;
                    justify-content: space-between;
                    border-top: 1px solid #f0f2f5;
                    font-size: 13px;
                    color: #65676b;
                }
                .download-cta {
                    background: linear-gradient(135deg, #ffffff, #f0f7ff);
                    border: 1px solid #cce4ff;
                    border-radius: 12px;
                    padding: 20px;
                    text-align: center;
                    margin-top: 20px;
                }
                .download-title {
                    font-size: 18px;
                    font-weight: 800;
                    color: #0F172A;
                    margin-bottom: 6px;
                }
                .download-desc {
                    font-size: 13px;
                    color: #475569;
                    margin-bottom: 16px;
                }
                .btn-download {
                    display: inline-block;
                    background: #1877F2;
                    color: white;
                    font-weight: 700;
                    padding: 12px 24px;
                    border-radius: 24px;
                    text-decoration: none;
                    font-size: 15px;
                    box-shadow: 0 4px 12px rgba(24, 119, 242, 0.35);
                }
                .footer {
                    text-align: center;
                    font-size: 11px;
                    color: #8a8d91;
                    padding: 24px 0;
                }
            </style>
        </head>
        <body>
            <div class="app-banner">
                <div class="app-banner-info">
                    <div class="app-logo">Y</div>
                    <div>
                        <div class="app-title">Yarkhoon.com</div>
                        <div class="app-sub">Chitral's Community Network</div>
                    </div>
                </div>
                <a href="$deepLinkScheme" class="btn-open-app">Open in App</a>
            </div>

            <div class="container">
                <div class="card">
                    <div class="card-header">
                        <img class="author-avatar" src="${post.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" }}" alt="${post.authorName}" />
                        <div>
                            <div class="author-name">${post.authorName}</div>
                            <div class="author-meta">Shared on Yarkhoon • Chitral, Pakistan</div>
                        </div>
                    </div>
                    <div class="post-body">
                        ${post.content}
                    </div>
                    $mediaHtml
                    <div class="card-stats">
                        <span>❤️ 👍 ${post.likesCount} Reactions</span>
                        <span>💬 ${post.commentsCount} Comments</span>
                    </div>
                </div>

                <div class="download-cta">
                    <div class="download-title">Join the conversation on Yarkhoon</div>
                    <div class="download-desc">React, comment, and connect with people across Chitral, Yarkhoon Valley, and the global diaspora.</div>
                    <a href="$appDownloadUrl" class="btn-download">📱 Download Yarkhoon App</a>
                </div>

                <div class="footer">
                    &copy; 2026 Yarkhoon.com • All Rights Reserved<br>
                    Official Community & Social Platform for Chitral & Beyond
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun exportHtmlToLocalFile(context: Context, post: Post): File {
        val html = generateWebHtml(post)
        val file = File(context.cacheDir, "post_preview_${post.id}.html")
        file.writeText(html)
        return file
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPostPreviewDialog(
    post: Post,
    onDismiss: () -> Unit,
    onOpenInApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isSimulatingDownload by remember { mutableStateOf(false) }

    val postUrl = remember(post) { "https://yarkhoon.com/post/${post.id}" }

    fun openInBrowser() {
        try {
            val file = YarkhoonPostWebPreviewHelper.exportHtmlToLocalFile(context, post)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open Web Preview in Browser"))
        } catch (e: Exception) {
            // Fallback to post URL
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(postUrl))
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not launch external browser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun triggerSimulatedDownload() {
        isSimulatingDownload = true
        Toast.makeText(context, "Downloading Yarkhoon Android App (APK)...", Toast.LENGTH_LONG).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Web Preview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "yarkhoon.com/post/${post.id}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = { openInBrowser() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open in Browser", fontSize = 12.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = Color(0xFFF1F5F9)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .testTag("web_post_preview_screen")
            ) {
                // Browser URL bar simulator
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Secure", tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            postUrl,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Sticky Web Banner: "Yarkhoon Web • Open in App / Download"
                Surface(
                    color = BrandBlue,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Y", fontWeight = FontWeight.Black, color = BrandBlue, fontSize = 20.sp)
                            }
                            Column {
                                Text("Yarkhoon.com", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Chitral Social & Community", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }

                        Button(
                            onClick = {
                                if (onOpenInApp != null) {
                                    onOpenInApp()
                                } else {
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = BrandBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Open in App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Main Web Page Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Post Card on Web Preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = post.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" },
                                    contentDescription = post.authorName,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            post.authorName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = DarkSlate
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Verified",
                                            tint = BrandBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        "Shared via Yarkhoon • Chitral, Pakistan",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Content text
                            Text(
                                text = post.content,
                                fontSize = 14.sp,
                                color = DarkSlate,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )

                            // Media image if present
                            if (post.mediaUrl.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                AsyncImage(
                                    model = post.mediaUrl,
                                    contentDescription = "Post image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp, max = 360.dp)
                                )
                            }

                            Divider(modifier = Modifier.padding(top = 12.dp), color = Color(0xFFF1F5F9))

                            // Reactions Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("❤️", fontSize = 14.sp)
                                    Text("👍", fontSize = 14.sp)
                                    Text(
                                        "${post.likesCount} Reactions",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    "${post.commentsCount} Comments",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Download Yarkhoon Call to Action Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFFBAE6FD)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDark))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.GetApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Experience Yarkhoon on Android",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = DarkSlate,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Download the official app to like, comment, connect with friends, and discover groups across Chitral & Yarkhoon Valley.",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = { triggerSimulatedDownload() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isSimulatingDownload) "Downloading Yarkhoon APK..." else "Download Yarkhoon for Android",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                Text(
                                    "Verified APK • Safe & Direct Community Download",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Web Footer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "yarkhoon.com • The Chitral Community Network",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            "App Links deep-link verified with autoVerify=true",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
