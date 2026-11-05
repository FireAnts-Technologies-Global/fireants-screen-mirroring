package com.anrstudio.template.ui.component.iptv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import com.anrstudio.template.app.CastFeatureConstants
import com.anrstudio.template.data.iptv.model.TVChannel
import com.anrstudio.template.ui.component.iptv.addchannel.AddChannelActivity
import com.anrstudio.template.ui.component.iptv.channel.all.AllChannelActivity
import com.anrstudio.template.ui.component.iptv.guide.GuideActivity
import com.anrstudio.template.ui.component.iptv.player.PlayListActivity
import com.anrstudio.template.ui.component.iptv.player.PlayListActivity.Companion.EXTRA_TV_CHANEL
import com.anrstudio.template.utils.Routes

object IptvRoutes {

    fun startAddChannelActivity(fromActivity: Activity, sourceType: String) {
        Intent(fromActivity, AddChannelActivity::class.java).apply {
            putExtra(AddChannelActivity.EXTRA_SOURCE_TYPE, sourceType)
            putExtra(CastFeatureConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }
    }

    fun startGuideActivity(
        fromActivity: Activity,
        isShowAdsWhenBack: Boolean = false,
        sourceType: String = "URL",
    ) {
        Intent(fromActivity, GuideActivity::class.java).apply {
            putExtra(AddChannelActivity.EXTRA_SOURCE_TYPE, sourceType)
            putExtra(CastFeatureConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            putExtra(IptvAppConstants.EXTRA_SHOW_ADS_WHEN_BACK, isShowAdsWhenBack)
            fromActivity.startActivity(this)
        }
    }

    fun startAllChannelActivity(
        fromActivity: Activity,
        categoryName: String,
        groupId: String,
        groupType: String = IptvAppConstants.GROUP_TYPE_CATEGORY,
    ) {
        Intent(fromActivity, AllChannelActivity::class.java).apply {
            putExtra(AllChannelActivity.EXTRA_CATEGORY_NAME, categoryName)
            putExtra(AllChannelActivity.EXTRA_GROUP_ID, groupId)
            putExtra(AllChannelActivity.EXTRA_GROUP_TYPE, groupType)
            putExtra(CastFeatureConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startPlaylistActivity(
        fromActivity: Activity,
        tvChanel: TVChannel,
        groupType: String = IptvAppConstants.GROUP_TYPE_CATEGORY,
        filterType: String = "",
        isShowAdsWhenBack: Boolean = false,
    ) {
        Intent(fromActivity, PlayListActivity::class.java).apply {
            putExtra(EXTRA_TV_CHANEL, tvChanel)
            putExtra(PlayListActivity.EXTRA_GROUP_TYPE, groupType)
            putExtra(PlayListActivity.EXTRA_FILTER_TYPE, filterType)
            putExtra(IptvAppConstants.EXTRA_SHOW_ADS_WHEN_BACK, isShowAdsWhenBack)
            putExtra(CastFeatureConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }
    }

    fun startAllChannelActivityForFavorite(fromActivity: Activity) {
        Intent(fromActivity, AllChannelActivity::class.java).apply {
            putExtra(AllChannelActivity.EXTRA_FILTER_TYPE, IptvAppConstants.FILTER_TYPE_FAVORITE)
            putExtra(CastFeatureConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }
    }

    fun startAllChannelActivityForRecent(fromActivity: Activity) {
        Intent(fromActivity, AllChannelActivity::class.java).apply {
            putExtra(AllChannelActivity.EXTRA_FILTER_TYPE, IptvAppConstants.FILTER_TYPE_RECENT)
            putExtra(CastFeatureConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }
    }

    fun addTrackingMoveScreen(fromActivity: String, toActivity: String) {
        Routes.addTrackingMoveScreen(fromActivity, toActivity)
    }
}
