package top.xiaojiang233.nekomusic.model

import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    val code: Int,
    val comments: List<Comment>,
    val hotComments: List<Comment> = emptyList(),
    val total: Int = 0,
    val more: Boolean = false
)

@Serializable
data class Comment(
    val user: CommentUser,
    val content: String,
    val time: Long,
    val likedCount: Int = 0,
    val commentId: Long
)

@Serializable
data class CommentUser(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null
)

