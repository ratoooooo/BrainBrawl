package com.example.brainbrawl.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvatarUtilsTest {
    @Test
    fun indicePorNomeAvatar_aceitaFormatosGuardados() {
        assertEquals(0, AvatarUtils.indicePorNomeAvatar("avatar_1_playstore"))
        assertEquals(4, AvatarUtils.indicePorNomeAvatar("@drawable/avatar_5_playstore.png"))
        assertEquals(11, AvatarUtils.indicePorNomeAvatar("avatar_12"))
    }

    @Test
    fun indicePorNomeAvatar_rejeitaValoresForaDaGrelha() {
        assertNull(AvatarUtils.indicePorNomeAvatar(""))
        assertNull(AvatarUtils.indicePorNomeAvatar("avatar_0_playstore"))
        assertNull(AvatarUtils.indicePorNomeAvatar("avatar_14_playstore"))
    }
}
