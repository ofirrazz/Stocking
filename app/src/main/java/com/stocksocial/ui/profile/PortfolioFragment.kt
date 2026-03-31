package com.stocksocial.ui.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.stocksocial.R
import com.stocksocial.databinding.FragmentPortfolioBinding
import com.stocksocial.model.SymbolSearchHit
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

    private var addHoldingSymbolView: MaterialAutoCompleteTextView? = null
    private var lastSymbolHints: List<SymbolSearchHit> = emptyList()
    private var selectedSymbolHit: SymbolSearchHit? = null
    private var suppressSymbolTextCallback = false
    private var pendingAddHoldingDialog: AlertDialog? = null

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

        viewModel.symbolHintsLive.observe(viewLifecycleOwner) { hints ->
            lastSymbolHints = hints
            val input = addHoldingSymbolView ?: return@observe
            val ctx = requireContext()
            val labels = hints.map { "${it.symbol} · ${it.description}" }
            input.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, labels))
            if (hints.isNotEmpty() && input.hasFocus()) {
                input.showDropDown()
            }
        }

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
                pendingAddHoldingDialog?.takeIf { it.isShowing }?.dismiss()
                pendingAddHoldingDialog = null
                viewModel.consumeUpsertState()
            }
        }

        viewModel.loadHoldings()
    }

    private fun showAddHoldingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_holding, null)
        val symbolInput = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.symbolInput)
        val buyPriceInput = dialogView.findViewById<TextInputEditText>(R.id.buyPriceInput)
        selectedSymbolHit = null
        addHoldingSymbolView = symbolInput

        symbolInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (suppressSymbolTextCallback) return
                selectedSymbolHit = null
                viewModel.onPortfolioSymbolQuery(s?.toString().orEmpty())
            }
        })

        symbolInput.setOnItemClickListener { _, _, position, _ ->
            val hit = lastSymbolHints.getOrNull(position) ?: return@setOnItemClickListener
            selectedSymbolHit = hit
            suppressSymbolTextCallback = true
            symbolInput.setText(hit.symbol)
            symbolInput.setSelection(hit.symbol.length)
            suppressSymbolTextCallback = false
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_holding)
            .setView(dialogView)
            .setPositiveButton(R.string.save_holding, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        pendingAddHoldingDialog = dialog

        dialog.setOnShowListener {
            symbolInput.focusAndShowKeyboard()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val rawSym = symbolInput.text?.toString().orEmpty()
                val typedSymbol = normalizeSymbolInput(rawSym)
                val matched = selectedSymbolHit
                    ?: lastSymbolHints.firstOrNull { it.symbol.equals(typedSymbol, ignoreCase = true) }
                    ?: lastSymbolHints.firstOrNull { it.symbol.startsWith(typedSymbol, ignoreCase = true) }
                val symbol = matched?.symbol ?: typedSymbol
                val priceText = buyPriceInput.text?.toString().orEmpty().replace(",", ".")
                val buyPrice = priceText.toDoubleOrNull() ?: 0.0
                if (symbol.isBlank()) {
                    Toast.makeText(requireContext(), R.string.enter_stock_symbol, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (buyPrice <= 0.0) {
                    Toast.makeText(requireContext(), R.string.portfolio_invalid_buy_price, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val nameFromHit = matched?.description
                viewModel.addOrUpdateHolding(symbol, buyPrice, nameFromHit)
            }
        }

        dialog.setOnDismissListener {
            addHoldingSymbolView = null
            if (pendingAddHoldingDialog === dialog) pendingAddHoldingDialog = null
            viewModel.clearSymbolHints()
        }

        dialog.show()
    }

    private fun normalizeSymbolInput(raw: String): String {
        val trimmed = raw.trim().uppercase(Locale.US).removePrefix("$")
        val first = trimmed.substringBefore("·").substringBefore("—").trim()
        return first.substringBefore(" ").trim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addHoldingSymbolView = null
        _binding = null
    }
}
