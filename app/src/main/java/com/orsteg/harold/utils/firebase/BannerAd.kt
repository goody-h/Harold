package com.orsteg.harold.utils.firebase

import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView

/**
 * Created by goodhope on 5/4/18.
 */
object BannerAd {

    fun setListener(ad: AdView) {
        ad.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                ad.visibility = View.VISIBLE
            }
        }
    }
}