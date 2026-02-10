package top.xiaojiang233.nekomusic.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginQrKeyResponse(
    val data: LoginQrKeyData,
    val code: Int
)

@Serializable
data class LoginQrKeyData(
    val code: Int,
    val unikey: String
)

@Serializable
data class LoginQrCreateResponse(
    val data: LoginQrCreateData,
    val code: Int
)

@Serializable
data class LoginQrCreateData(
    val qrurl: String,
    val qrimg: String // Base64 image
)

@Serializable
data class LoginQrCheckResponse(
    val code: Int,
    val message: String,
    val cookie: String? = null
)

@Serializable
data class LoginStatusResponse(
    val data: LoginStatusData
)

@Serializable
data class LoginStatusData(
    val code: Int,
    val account: Account?,
    val profile: Profile?
)

@Serializable
data class Account(
    val id: Long,
    val userName: String,
    val type: Int,
    val status: Int,
    val createTime: Long,
    val vipType: Int
)

@Serializable
data class Profile(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String,
    val backgroundUrl: String?,
    val signature: String?
)

