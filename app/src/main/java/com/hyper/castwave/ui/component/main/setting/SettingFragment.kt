package com.hyper.castwave.ui.component.main.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.hyper.castwave.R
import com.hyper.castwave.app.CastFeatureConstants
import com.hyper.castwave.databinding.CastFragmentSettingBinding
import com.hyper.castwave.ui.bases.BaseFragment
import com.hyper.castwave.ui.bases.ext.click
import com.hyper.castwave.ui.bases.ext.showCastRateDialog
import com.hyper.castwave.utils.Routes

class SettingFragment : BaseFragment<CastFragmentSettingBinding>() {

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_setting

    override fun initViews() {
        super.initViews()
        bindSettingOption(mBinding.layoutSettingLanguage, R.drawable.cast_settings_icon_language, R.string.cast_language)
        bindSettingOption(mBinding.layoutSettingPrivacy, R.drawable.cast_settings_icon_privacy, R.string.cast_privacy_policy)
        bindSettingOption(mBinding.layoutSettingRate, R.drawable.cast_settings_icon_rate, R.string.cast_rate_us)
        bindSettingOption(mBinding.layoutSettingShare, R.drawable.cast_settings_icon_share, R.string.cast_share)
    }

    private fun bindSettingOption(
        binding: com.hyper.castwave.databinding.CastItemSettingOptionBinding,
        iconRes: Int,
        labelRes: Int,
    ) {
        binding.imgSettingIcon.setImageResource(iconRes)
        binding.tvSettingLabel.setText(labelRes)
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.layoutSettingLanguage.root.click {
            Routes.startLanguageActivity(requireActivity(), Bundle().apply {
                putBoolean(CastFeatureConstants.KEY_SETTING, true)
            })
        }

        mBinding.layoutSettingPrivacy.root.click {
            val url = CastFeatureConstants.LINK_PRIVACY_POLICY
            if (url.isBlank()) return@click
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        mBinding.layoutSettingRate.root.click {
            showCastRateDialog(requireActivity(), isFinish = false)
        }

        mBinding.layoutSettingShare.root.click {
            val storeUrl = getString(
                R.string.cast_play_store_listing_url_template,
                requireActivity().packageName,
            )
            val message = getString(
                R.string.cast_share_app_text,
                getString(R.string.app_name),
                storeUrl,
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            startActivity(Intent.createChooser(sendIntent, null))
        }
    }
}
