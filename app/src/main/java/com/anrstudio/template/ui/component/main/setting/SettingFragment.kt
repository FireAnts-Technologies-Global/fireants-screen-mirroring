package com.anrstudio.template.ui.component.main.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.anrstudio.template.R
import com.anrstudio.template.app.CastFeatureConstants
import com.anrstudio.template.databinding.CastFragmentSettingBinding
import com.anrstudio.template.ui.bases.BaseFragment
import com.anrstudio.template.ui.bases.ext.click
import com.anrstudio.template.ui.bases.ext.showCastRateDialog
import com.anrstudio.template.utils.Routes

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
        binding: com.anrstudio.template.databinding.CastItemSettingOptionBinding,
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
