package com.stocksocial.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.R
import com.stocksocial.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val adapter = NotificationsAdapter()

    private val mockItems = mutableListOf(
        NotificationUi(
            "1",
            NotificationUiType.STOCK_ALERT,
            "Price target hit",
            "AAPL reached your target of $175.",
            "10 min ago",
            false
        ),
        NotificationUi(
            "2",
            NotificationUiType.LIKE,
            "New like",
            "Alex liked your post about TSLA.",
            "1 hour ago",
            false
        ),
        NotificationUi(
            "3",
            NotificationUiType.COMMENT,
            "New comment",
            "Sam replied to your analysis.",
            "2 hours ago",
            false
        ),
        NotificationUi(
            "4",
            NotificationUiType.FOLLOW,
            "New follower",
            "Jordan started following you.",
            "3 hours ago",
            false
        ),
        NotificationUi(
            "5",
            NotificationUiType.PRICE_ALERT,
            "Large price move",
            "NVDA moved +8.5% today.",
            "5 hours ago",
            true
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
        adapter.submitList(mockItems.toList())

        binding.markAllReadButton.setOnClickListener {
            for (i in mockItems.indices) {
                mockItems[i] = mockItems[i].copy(isRead = true)
            }
            adapter.submitList(mockItems.toList())
            Toast.makeText(requireContext(), R.string.mark_all_read, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
