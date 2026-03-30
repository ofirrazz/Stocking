package com.stocksocial.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.stocksocial.R
import com.stocksocial.databinding.FragmentUserProfileBinding
import com.stocksocial.model.PublicUserProfile
import com.stocksocial.ui.adapters.PortfolioHoldingsAdapter
import com.stocksocial.ui.adapters.StockPostsAdapter
import com.stocksocial.ui.adapters.WatchlistAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.UserProfileViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class UserProfileFragment : Fragment() {

    private val args: UserProfileFragmentArgs by navArgs()
    private val viewModel: UserProfileViewModel by viewModels { appViewModelFactory }
    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val postsAdapter by lazy {
        StockPostsAdapter(
            handleUsernameOverride = args.username,
            onPostClick = { post ->
                val dir = UserProfileFragmentDirections.actionUserProfileFragmentToPostDetailsFragment(post.id)
                findNavController().navigate(dir)
            }
        )
    }
    private val portfolioAdapter = PortfolioHoldingsAdapter { symbol ->
        val dir = UserProfileFragmentDirections.actionUserProfileFragmentToStockDetailsFragment(symbol)
        findNavController().navigate(dir)
    }
    private val watchlistAdapter = WatchlistAdapter { stock ->
        val dir = UserProfileFragmentDirections.actionUserProfileFragmentToStockDetailsFragment(stock.symbol)
        findNavController().navigate(dir)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.userProfileToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.userProfileToolbar.title = ""

        binding.postsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.postsRecyclerView.adapter = postsAdapter
        binding.portfolioRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.portfolioRecyclerView.adapter = portfolioAdapter
        binding.watchlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.watchlistRecyclerView.adapter = watchlistAdapter

        binding.tabPostsButton.setOnClickListener { selectTab(0) }
        binding.tabPortfolioButton.setOnClickListener { selectTab(1) }
        binding.tabWatchlistButton.setOnClickListener { selectTab(2) }
        binding.portfolioViewButton.setOnClickListener { selectTab(1) }

        viewModel.profileStateLive.observe(viewLifecycleOwner) { state ->
            state.errorMessage?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
            state.data?.let { bindProfile(it) }
        }
        viewModel.postsStateLive.observe(viewLifecycleOwner) { state ->
            postsAdapter.submitList(state.data.orEmpty())
        }
        viewModel.portfolioStateLive.observe(viewLifecycleOwner) { state ->
            val list = state.data.orEmpty()
            portfolioAdapter.submitList(list)
            val pct = viewModel.portfolioPerformancePercent(list)
            val sign = if (pct >= 0) "+" else ""
            binding.portfolioPerfPercentText.text = "$sign${String.format(Locale.US, "%.1f", pct)}%"
            val ctx = requireContext()
            val green = ContextCompat.getColor(ctx, R.color.success_green)
            val red = ContextCompat.getColor(ctx, R.color.destructive_red)
            if (pct >= 0) {
                binding.portfolioPerfPercentText.setTextColor(green)
                binding.perfTrendIcon.setImageResource(android.R.drawable.arrow_up_float)
                binding.perfTrendIcon.imageTintList = android.content.res.ColorStateList.valueOf(green)
            } else {
                binding.portfolioPerfPercentText.setTextColor(red)
                binding.perfTrendIcon.setImageResource(android.R.drawable.arrow_down_float)
                binding.perfTrendIcon.imageTintList = android.content.res.ColorStateList.valueOf(red)
            }
        }
        viewModel.watchlistStateLive.observe(viewLifecycleOwner) { state ->
            val stocks = state.data.orEmpty().map { it.stock }
            watchlistAdapter.submitList(stocks)
        }

        viewModel.load(args.username)
        selectTab(0)
    }

    private fun bindProfile(p: PublicUserProfile) {
        binding.toolbarTitleText.text = p.displayName
        binding.displayNameText.text = p.displayName
        binding.handleText.text = "@${p.username}"
        if (p.bio.isNotBlank()) {
            binding.bioText.visibility = View.VISIBLE
            binding.bioText.text = p.bio
        } else {
            binding.bioText.visibility = View.GONE
        }
        binding.postsStatText.text = String.format(Locale.US, "%,d", p.postsCount)
        binding.followersStatText.text = String.format(Locale.US, "%,d", p.followersCount)
        binding.likesStatText.text = String.format(Locale.US, "%,d", p.totalLikesReceived)
        binding.toolbarSubtitleText.text =
            getString(R.string.user_profile_toolbar_subtitle, p.postsCount, p.totalLikesReceived)

        val photo = p.avatarUrl
        if (!photo.isNullOrBlank()) {
            Glide.with(binding.avatarImage).load(photo).circleCrop().into(binding.avatarImage)
        } else {
            binding.avatarImage.setImageResource(android.R.drawable.ic_menu_myplaces)
        }
        val banner = p.bannerUrl
        if (!banner.isNullOrBlank()) {
            Glide.with(binding.bannerImage).load(banner).centerCrop().into(binding.bannerImage)
        } else {
            binding.bannerImage.setImageResource(R.drawable.bg_profile_banner)
        }

        if (!p.location.isNullOrBlank()) {
            binding.locationRow.visibility = View.VISIBLE
            binding.locationText.text = p.location
        } else {
            binding.locationRow.visibility = View.GONE
        }

        val joined = p.joinedTimestamp
        if (joined != null && joined > 0L) {
            binding.joinedRow.visibility = View.VISIBLE
            val df = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
            binding.joinedText.text = getString(R.string.user_profile_joined_fmt, df.format(Date(joined)))
        } else {
            binding.joinedRow.visibility = View.GONE
        }

        if (!p.website.isNullOrBlank()) {
            binding.websiteRow.visibility = View.VISIBLE
            binding.websiteText.text = p.website
            binding.websiteRow.setOnClickListener {
                val url = p.website!!.let { if (it.startsWith("http")) it else "https://$it" }
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        } else {
            binding.websiteRow.visibility = View.GONE
            binding.websiteRow.setOnClickListener(null)
        }
    }

    private fun selectTab(index: Int) {
        binding.postsRecyclerView.visibility = if (index == 0) View.VISIBLE else View.GONE
        binding.portfolioRecyclerView.visibility = if (index == 1) View.VISIBLE else View.GONE
        binding.watchlistRecyclerView.visibility = if (index == 2) View.VISIBLE else View.GONE
        binding.watchlistHintText.visibility = if (index == 2) View.VISIBLE else View.GONE
        styleTab(binding.tabPostsButton, index == 0)
        styleTab(binding.tabPortfolioButton, index == 1)
        styleTab(binding.tabWatchlistButton, index == 2)
    }

    private fun styleTab(btn: MaterialButton, selected: Boolean) {
        val ctx = requireContext()
        val gold = ContextCompat.getColor(ctx, R.color.primary_gold)
        val black = ContextCompat.getColor(ctx, R.color.black)
        val surface = ContextCompat.getColor(ctx, R.color.surface_card)
        val muted = ContextCompat.getColor(ctx, R.color.text_muted)
        val border = ContextCompat.getColor(ctx, R.color.border)
        if (selected) {
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(gold)
            btn.setTextColor(black)
            btn.strokeWidth = 0
        } else {
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(surface)
            btn.setTextColor(muted)
            btn.strokeWidth = (resources.displayMetrics.density * 1).toInt()
            btn.strokeColor = android.content.res.ColorStateList.valueOf(border)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
