package com.stocksocial.ui.stocks

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.button.MaterialButton
import com.stocksocial.R
import com.stocksocial.databinding.FragmentStockDetailsBinding
import com.stocksocial.model.AnalystRecommendation
import com.stocksocial.model.PriceChartSeries
import com.stocksocial.model.Stock
import com.stocksocial.ui.adapters.StockPostsAdapter
import com.stocksocial.ui.adapters.StockUiFormatter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.StockDetailsViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class StockDetailsFragment : Fragment() {

    private val args: StockDetailsFragmentArgs by navArgs()
    private val viewModel: StockDetailsViewModel by viewModels { appViewModelFactory }
    private val postsAdapter = StockPostsAdapter(
        onPostClick = { post ->
            val dir = StockDetailsFragmentDirections.actionStockDetailsFragmentToPostDetailsFragment(post.id)
            findNavController().navigate(dir)
        }
    )
    private var _binding: FragmentStockDetailsBinding? = null
    private val binding get() = _binding!!
    private val currency = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStockDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.postsRecyclerView.adapter = postsAdapter

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.favoriteButton.setOnClickListener {
            viewModel.toggleFavorite(args.symbol.uppercase(Locale.US))
        }
        binding.addPortfolioButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.stock_add_portfolio_toast, Toast.LENGTH_SHORT).show()
        }

        binding.tabOverviewButton.setOnClickListener { selectTab(0) }
        binding.tabInstitutionalButton.setOnClickListener { selectTab(1) }
        binding.tabAnalystsButton.setOnClickListener { selectTab(2) }
        selectTab(0)

        val sym = args.symbol.uppercase(Locale.US)
        binding.toolbarTickerText.text = "\$$sym"
        binding.toolbarNameText.text = StockDetailsRepositorySymbolNames.nameFor(sym)

        viewModel.quoteStateLive.observe(viewLifecycleOwner) { state ->
            state.data?.let { stock ->
                bindQuote(stock)
                bindDerived(stock, viewModel.recommendationsStateLive.value?.data?.firstOrNull())
            }
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.recommendationsStateLive.observe(viewLifecycleOwner) { state ->
            val latest = state.data?.firstOrNull()
            binding.recommendationsText.text = formatRecommendations(latest)
            viewModel.quoteStateLive.value?.data?.let { stock ->
                bindDerived(stock, latest)
            }
        }

        viewModel.postsStateLive.observe(viewLifecycleOwner) { state ->
            postsAdapter.submitList(state.data.orEmpty())
        }

        viewModel.chartStateLive.observe(viewLifecycleOwner) { state ->
            val series = state.data ?: PriceChartSeries()
            binding.priceChart.setTimedPoints(series.points)
        }

        viewModel.favoriteStateLive.observe(viewLifecycleOwner) { fav ->
            when (fav) {
                true -> {
                    binding.favoriteButton.setIconResource(android.R.drawable.star_big_on)
                    binding.favoriteButton.iconTint = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.primary_gold)
                    )
                }
                false -> {
                    binding.favoriteButton.setIconResource(android.R.drawable.star_big_off)
                    binding.favoriteButton.iconTint = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.text_primary)
                    )
                }
                null -> Unit
            }
        }
        viewModel.favoriteErrorLive.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                viewModel.consumeFavoriteError()
            }
        }

        viewModel.load(args.symbol)
    }

    private fun bindQuote(stock: Stock) {
        binding.toolbarNameText.text = stock.name
        binding.livePriceText.text = currency.format(stock.price)
        val dAbs = stock.dayChangeAbs
        val pct = stock.dailyChangePercent
        if (dAbs != null) {
            val moneySign = if (dAbs >= 0) "+" else "−"
            val pctSign = if (pct >= 0) "+" else ""
            binding.changeText.text =
                "$moneySign${currency.format(abs(dAbs))} ($pctSign${String.format(Locale.US, "%.2f", pct)}%)"
        } else {
            binding.changeText.text = StockUiFormatter.formatChangePercent(pct)
        }
        binding.changeText.setTextColor(StockUiFormatter.resolveChangeColor(requireContext(), pct))
        binding.statOpenText.text = stock.open?.let { currency.format(it) } ?: "—"
        binding.statHighText.text = stock.high?.let { currency.format(it) } ?: "—"
        binding.statLowText.text = stock.low?.let { currency.format(it) } ?: "—"
        binding.statVolumeText.text = formatVolume(stock.volume)
    }

    private fun bindDerived(stock: Stock, rec: AnalystRecommendation?) {
        val d = StockDetailsUiHelper.buildDerived(stock, rec)
        binding.ratingScoreText.text = String.format(Locale.US, "%.1f", d.overallScore)
        binding.ratingLabelText.setText(d.ratingLabelRes)
        binding.metricPeText.text = d.peText
        binding.metricMcapText.text = d.marketCapText
        binding.metricDividendText.text = d.dividendText
        binding.metricEpsText.text = d.epsText
        binding.sentimentProgress.progress = d.sentimentPercent
        binding.sentimentPercentText.text = getString(R.string.stock_sentiment_percent, d.sentimentPercent)
        val rows = d.scoreRows
        if (rows.size >= 4) {
            applyScoreRow(binding.scoreLabel1, binding.scoreBar1, binding.scoreValue1, binding.scoreIcon1, rows[0])
            applyScoreRow(binding.scoreLabel2, binding.scoreBar2, binding.scoreValue2, binding.scoreIcon2, rows[1])
            applyScoreRow(binding.scoreLabel3, binding.scoreBar3, binding.scoreValue3, binding.scoreIcon3, rows[2])
            applyScoreRow(binding.scoreLabel4, binding.scoreBar4, binding.scoreValue4, binding.scoreIcon4, rows[3])
        }
    }

    private fun applyScoreRow(
        label: TextView,
        bar: ProgressBar,
        value: TextView,
        icon: ImageView,
        row: ScoreRowUi
    ) {
        val ctx = requireContext()
        label.setText(row.labelRes)
        bar.progress = row.progress
        value.text = String.format(Locale.US, "%.1f", row.score)
        val c = ContextCompat.getColor(ctx, if (row.iconTintIsGreen) R.color.success_green else R.color.primary_gold)
        value.setTextColor(c)
        icon.imageTintList = ColorStateList.valueOf(c)
    }

    private fun selectTab(index: Int) {
        binding.sectionOverview.visibility = if (index == 0) View.VISIBLE else View.GONE
        binding.sectionInstitutional.visibility = if (index == 1) View.VISIBLE else View.GONE
        binding.sectionAnalysts.visibility = if (index == 2) View.VISIBLE else View.GONE
        styleTab(binding.tabOverviewButton, index == 0)
        styleTab(binding.tabInstitutionalButton, index == 1)
        styleTab(binding.tabAnalystsButton, index == 2)
    }

    private fun styleTab(btn: MaterialButton, selected: Boolean) {
        val ctx = requireContext()
        val gold = ContextCompat.getColor(ctx, R.color.primary_gold)
        val black = ContextCompat.getColor(ctx, R.color.black)
        val surface = ContextCompat.getColor(ctx, R.color.surface_card)
        val muted = ContextCompat.getColor(ctx, R.color.text_muted)
        val border = ContextCompat.getColor(ctx, R.color.border)
        if (selected) {
            btn.backgroundTintList = ColorStateList.valueOf(gold)
            btn.setTextColor(black)
            btn.iconTint = ColorStateList.valueOf(black)
            btn.strokeWidth = 0
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(surface)
            btn.setTextColor(muted)
            btn.iconTint = ColorStateList.valueOf(muted)
            btn.strokeWidth = (resources.displayMetrics.density * 1).toInt()
            btn.strokeColor = ColorStateList.valueOf(border)
        }
    }

    private fun formatVolume(v: Long?): String {
        if (v == null || v <= 0L) return "—"
        return when {
            v >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", v / 1_000_000_000.0)
            v >= 1_000_000 -> String.format(Locale.US, "%.1fM", v / 1_000_000.0)
            v >= 1_000 -> String.format(Locale.US, "%.1fK", v / 1_000.0)
            else -> v.toString()
        }
    }

    private fun formatRecommendations(latest: AnalystRecommendation?): String {
        if (latest == null) return getString(R.string.stock_no_recommendations)
        return buildString {
            append("Period: ${latest.period}\n")
            append("Strong Buy: ${latest.strongBuy} | Buy: ${latest.buy}\n")
            append("Hold: ${latest.hold} | Sell: ${latest.sell} | Strong Sell: ${latest.strongSell}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Display names aligned with [com.stocksocial.repository.StockDetailsRepository] */
private object StockDetailsRepositorySymbolNames {
    private val names = mapOf(
        "SPY" to "S&P 500 ETF",
        "DIA" to "Dow Jones ETF",
        "QQQ" to "Nasdaq 100 ETF",
        "NVDA" to "NVIDIA",
        "AMD" to "Advanced Micro Devices",
        "MSFT" to "Microsoft",
        "AAPL" to "Apple Inc.",
        "TSLA" to "Tesla",
        "GOOGL" to "Alphabet Inc.",
        "META" to "Meta Platforms",
        "AMZN" to "Amazon"
    )

    fun nameFor(symbol: String): String = names[symbol.uppercase(Locale.US)] ?: symbol
}
