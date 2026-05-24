package com.example.brainbrawl.config

enum class RoomFlowType(val firebaseValue: String) {
    MATCHMAKING(GameConstants.ORIGEM_MATCHMAKING),
    INVITE(GameConstants.ORIGEM_CONVITE),
    PRIVATE(GameConstants.ORIGEM_MANUAL);

    val isMatchmaking: Boolean
        get() = this == MATCHMAKING

    val isInviteOrPrivate: Boolean
        get() = this == INVITE || this == PRIVATE

    companion object {
        fun fromOrigin(origin: String): RoomFlowType {
            return when (origin) {
                GameConstants.ORIGEM_MATCHMAKING -> MATCHMAKING
                GameConstants.ORIGEM_CONVITE -> INVITE
                else -> PRIVATE
            }
        }
    }
}
