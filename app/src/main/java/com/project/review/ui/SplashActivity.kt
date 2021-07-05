package com.project.review.ui

import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.daimajia.androidanimations.library.Techniques
import com.project.review.MainActivity
import com.project.review.R
import com.viksaa.sssplash.lib.activity.AwesomeSplash
import com.viksaa.sssplash.lib.cnst.Flags
import com.viksaa.sssplash.lib.model.ConfigSplash

/**
 * entry point of the application
 * contains the initial animation before MainActivity starts
 *
 * @see MainActivity
 */
class SplashActivity : AwesomeSplash() {
    override fun initSplash(configSplash: ConfigSplash?) {
        configSplash?.backgroundColor =
            R.color.primaryColor
        configSplash?.animCircularRevealDuration = 800
        configSplash?.revealFlagX = (Flags.REVEAL_RIGHT)
        configSplash?.revealFlagY = (Flags.REVEAL_BOTTOM)

        //Customize Logo
        configSplash?.logoSplash = (R.mipmap.ic_launcher)
        configSplash?.animLogoSplashDuration = (100)
        configSplash?.animLogoSplashTechnique = (Techniques.Bounce)

        //Customize Title
        configSplash?.titleSplash = getString(R.string.app_name)
        configSplash?.titleTextColor = R.color.secondaryTextColor
        configSplash?.titleTextSize = (30f)
        configSplash?.animTitleDuration = (200)
        configSplash?.animTitleTechnique = (Techniques.DropOut)
        configSplash?.titleFont = "fonts/light.ttf"
    }

    override fun animationsFinished() {
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 50)

    }


}