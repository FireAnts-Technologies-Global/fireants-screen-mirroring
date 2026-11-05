package com.anrstudio.template.ui.component.iptv.guide.file

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastFragmentGuideFileBinding
import com.anrstudio.template.ui.bases.BaseFragment

class GuideFileFragment : BaseFragment<CastFragmentGuideFileBinding>() {

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_guide_file

    override fun initViews() {
        super.initViews()
        setupSteps()
    }

    private fun setupSteps() {
        highlightText(
            mBinding.tvStep1,
            getString(R.string.cast_guide_file_new_step1_text),
            getString(R.string.cast_guide_file_new_step1_highlight),
        )
        mBinding.tvStep2.text = getString(R.string.cast_guide_file_new_step2_text)
        highlightText(
            mBinding.tvStep3,
            getString(R.string.cast_guide_file_new_step3_text),
            listOf(
                getString(R.string.cast_guide_file_new_step3_highlight1),
                getString(R.string.cast_guide_file_new_step3_highlight2),
            ),
        )
        mBinding.tvStep4.text = getString(R.string.cast_guide_file_new_step4_text)
    }

    private fun highlightText(textView: android.widget.TextView, fullText: String, highlightText: String) {
        val spannable = SpannableString(fullText)
        val highlightColor = ContextCompat.getColor(requireContext(), R.color.cast_nav_item_selected_text)
        val startIndex = fullText.indexOf(highlightText)
        if (startIndex != -1) {
            spannable.setSpan(
                ForegroundColorSpan(highlightColor),
                startIndex,
                startIndex + highlightText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        textView.text = spannable
    }

    private fun highlightText(textView: android.widget.TextView, fullText: String, highlightTexts: List<String>) {
        val spannable = SpannableString(fullText)
        val highlightColor = ContextCompat.getColor(requireContext(), R.color.cast_nav_item_selected_text)
        highlightTexts.forEach { highlight ->
            val startIndex = fullText.indexOf(highlight)
            if (startIndex != -1) {
                spannable.setSpan(
                    ForegroundColorSpan(highlightColor),
                    startIndex,
                    startIndex + highlight.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        textView.text = spannable
    }
}
