package com.stocksocial.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.stocksocial.R
import com.stocksocial.databinding.FragmentPortfolioBinding
import com.stocksocial.ui.adapters.PortfolioHoldingsAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.utils.focusAndShowKeyboard
import com.stocksocial.viewmodel.PortfolioViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class PortfolioFragment : Fragment() {
    private val viewModel: PortfolioViewModel by viewModels { appViewModelFactory }
    private val adapter = PortfolioHoldingsAdapter()
    private var _binding: FragmentPortfolioBinding? = null
    private val binding get() = _binding!!
    private val currency = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPortfolioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val showUp = findNavController().previousBackStackEntry?.destination?.id == R.id.profileFragment
        binding.portfolioToolbar.navigationIcon =
            if (showUp) ContextCompat.getDrawable(requireContext(), R.drawable.ic_nav_back) else null
        binding.portfolioToolbar.setNavigationOnClickListener {
            if (showUp) findNavController().navigateUp()
        }

        binding.holdingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.holdingsRecyclerView.adapter = adapter
        binding.addHoldingButton.setOnClickListener { showAddHoldingDialog() }

        viewModel.holdingsStateLive.observe(viewLifecycleOwner) { state ->
            val list = state.data.orEmpty()
            adapter.submitList(list)
            val invested = list.sumOf { it.investedValue }
            val value = list.sumOf { it.currentValue }
            val pnl = value - invested
            val pnlPercent = if (invested == 0.0) 0.0 else (pnl / invested) * 100.0
            binding.totalValueText.text = currency.format(value)
            val moneyStr = "${if (pnl >= 0) "+" else "-"}${currency.format(abs(pnl))}"
            val pctStr = String.format(Locale.US, "%+.2f%%", pnlPercent)
            binding.gainLossText.text = "$moneyStr ($pctStr)"
            val ctx = requireContext()
            val green = ContextCompat.getColor(ctx, R.color.success_green)
            val red = ContextCompat.getColor(ctx, R.color.destructive_red)
            if (pnl >= 0) {
                binding.gainLossText.setTextColor(green)
                binding.gainTrendIcon.setImageResource(android.R.drawable.arrow_up_float)
                binding.gainTrendIcon.imageTintList = android.content.res.ColorStateList.valueOf(green)
            } else {
                binding.gainLossText.setTextColor(red)
                binding.gainTrendIcon.setImageResource(android.R.drawable.arrow_down_float)
                binding.gainTrendIcon.imageTintList = android.content.res.ColorStateList.valueOf(red)
            }
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.upsertStateLive.observe(viewLifecycleOwner) { state ->
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                viewModel.consumeUpsertState()
            } else if (state.data != null) {
                Toast.makeText(requireContext(), R.string.holding_saved, Toast.LENGTH_SHORT).show()
                viewModel.consumeUpsertState()
            }
        }

        viewModel.loadHoldings()
    }

    private fun showAddHoldingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_holding, null)
        val symbolInput = dialogView.findViewById<TextInputEditText>(R.id.symbolInput)
        val sharesInput = dialogView.findViewById<TextInputEditText>(R.id.sharesInput)
        val buyPriceInput = dialogView.findViewById<TextInputEditText>(R.id.buyPriceInput)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_holding)
            .setView(dialogView)
            .setPositiveButton(R.string.save_holding) { _, _ ->
                val symbol = symbolInput.text?.toString().orEmpty()
                val shares = sharesInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                val buyPrice = buyPriceInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.addOrUpdateHolding(symbol, shares, buyPrice)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener { symbolInput.focusAndShowKeyboard() }
                dialog.show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
