package com.hyper.castwave.ui.component.iptv.guide.stream

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastFragmentGuideStreamBinding
import com.hyper.castwave.ui.bases.BaseFragment

class GuideStreamFragment : BaseFragment<CastFragmentGuideStreamBinding>() {

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_guide_stream

    override fun initViews() {
        super.initViews()
        setupSteps()
        setupSearchButton()
        setupSearchButtonText()
    }

    private fun setupSteps() {
        highlightText(
            mBinding.tvStep1,
            getString(R.string.cast_guide_stream_new_step1_text),
            getString(R.string.cast_guide_stream_new_step1_highlight),
        )
        highlightText(
            mBinding.tvStep2,
            getString(R.string.cast_guide_stream_new_step2_text),
            getString(R.string.cast_guide_stream_new_step2_highlight),
        )
        highlightText(
            mBinding.tvStep3,
            getString(R.string.cast_guide_stream_new_step3_text),
            listOf(
                getString(R.string.cast_guide_stream_new_step3_highlight1),
                getString(R.string.cast_guide_stream_new_step3_highlight2),
            ),
        )
        highlightText(
            mBinding.tvStep4,
            getString(R.string.cast_guide_stream_new_step4_text),
            getString(R.string.cast_guide_stream_new_step4_highlight),
        )
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

    private fun setupSearchButton() {
        mBinding.btnSearch.setOnClickListener {
            val query = "Public IPTV Stream URL"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")))
        }
    }

    private fun setupSearchButtonText() {
        mBinding.tvSearchText.text = getString(R.string.cast_guide_stream_new_search_placeholder)
    }
}
