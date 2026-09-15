package com.codesteem.quransdk

import android.content.Context
import com.codesteem.quransdk.api.QuranApi
import com.codesteem.quransdk.internal.QuranApiImpl
import com.codesteem.quransdk.ui.QuranPageFragment
import com.codesteem.quransdk.ui.QuranPageView

/**
 * Entry point for the Quran SDK.
 */
object QuranSdk {

    /**
     * Creates a SDK API instance. Call [com.codesteem.quransdk.api.QuranApi.initialize]
     * before using data or page APIs.
     */
    fun create(context: Context): QuranApi {
        return QuranApiImpl(context.applicationContext)
    }

    /**
     * Creates an embeddable page view. Must be used inside a LifecycleOwner (Activity/Fragment).
     */
    fun createPageView(context: Context): QuranPageView = QuranPageView(context)

    /**
     * Creates a Fragment hosting the mushaf page reader.
     */
    fun createPageFragment(): QuranPageFragment = QuranPageFragment.newInstance()
}
