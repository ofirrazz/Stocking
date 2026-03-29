package com.stocksocial.ui.portfolio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.R
import com.stocksocial.databinding.FragmentPortfolioBinding
import java.util.Locale

class PortfolioFragment : Fragment() {

    private var _binding: FragmentPortfolioBinding? = null
    private val binding get() = _binding!!

    private val adapter = HoldingsAdapter()

    private val mockHoldings = listOf(
        HoldingUi("AAPL", "Apple Inc.", 50, 150.0f, 172.50f, 15.0f),
        HoldingUi("TSLA", "Tesla Inc.", 25, 200.0f, 242.84f, 21.4f),
        HoldingUi("NVDA", "NVIDIA Corp.", 30, 400.0f, 495.22f, 23.8f),
        HoldingUi("MSFT", "Microsoft Corp.", 40, 320.0f, 378.91f, 18.4f)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPortfolioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.holdingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.holdingsRecyclerView.adapter = adapter
        adapter.submitList(mockHoldings)

        val totalValue = mockHoldings.sumOf { it.shares * it.currentPrice.toDouble() }
        val totalInvestment = mockHoldings.sumOf { it.shares * it.avgPrice.toDouble() }
        val totalGain = totalValue - totalInvestment
        val totalGainPercent = if (totalInvestment > 0) (totalGain / totalInvestment) * 100.0 else 0.0

        binding.totalValueText.text = String.format(Locale.US, "$%,.2f", totalValue)
        binding.totalGainText.text = String.format(
            Locale.US,
            "+$%,.2f (%.2f%%)",
            totalGain,
            totalGainPercent
        )

        binding.portfolioAddButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
