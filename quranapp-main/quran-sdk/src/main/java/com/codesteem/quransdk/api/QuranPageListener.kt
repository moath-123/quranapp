package com.codesteem.quransdk.api

import com.codesteem.quransdk.api.model.Ayah

/**
 * Callbacks for ayah interactions on [QuranPageView].
 * The SDK does not show menus or popups — the client app handles all UI actions.
 */
interface QuranPageListener {

    fun onAyahTapped(ayah: Ayah)

    fun onAyahLongPressed(ayah: Ayah, screenX: Float, screenY: Float)
}
