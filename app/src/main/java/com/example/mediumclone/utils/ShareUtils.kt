package com.example.mediumclone.utils

import android.content.Context
import android.content.Intent
import com.example.mediumclone.data.model.Article

object ShareUtils {
    fun shareArticle(context: Context, article: Article) {
        val shareText = buildString {
            append(article.title)
            append("\n\n")
            append("By ${article.authorName}")
            append("\n\n")
            append("Read the full article here:\n")
            append("https://yazan-medium-clone.web.app/article/${article.id}")
            append("\n\n")
            append("Download the app for the best experience!")
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, article.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
    }
}
