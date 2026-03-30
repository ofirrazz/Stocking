package com.stocksocial.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.R
import com.stocksocial.databinding.FragmentNotificationsBinding
import com.stocksocial.model.InAppNotificationUi
import com.stocksocial.ui.adapters.NotificationsAdapter

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val notifications = mutableListOf<InAppNotificationUi>()
    private lateinit var listAdapter: NotificationsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.notificationsToolbar.navigationIcon = null
        if (notifications.isEmpty()) {
            notifications.addAll(sampleNotifications())
        }
        listAdapter = NotificationsAdapter(
            onMarkRead = { id ->
                val idx = notifications.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    val cur = notifications[idx]
                    notifications[idx] = cur.copy(unread = false)
                    listAdapter.updateItem(notifications[idx])
                    refreshSubtitle()
                }
            },
            onViewStock = { symbol ->
                val dir = NotificationsFragmentDirections.actionNotificationsFragmentToStockDetailsFragment(symbol)
                findNavController().navigate(dir)
            }
        )
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = listAdapter
        listAdapter.submitList(notifications.toList())
        refreshSubtitle()
    }

    private fun refreshSubtitle() {
        val unread = notifications.count { it.unread }
        binding.notificationsToolbar.subtitle = getString(R.string.notifications_unread_count, unread)
    }

    private fun sampleNotifications(): List<InAppNotificationUi> = listOf(
        InAppNotificationUi(
            id = "1",
            headline = "Sarah Chen",
            body = "liked your post about \$AAPL and \$MSFT",
            timeLabel = "5m",
            unread = true,
            viewPositive = null,
            stockSymbolForView = null,
            isSocialStyle = true
        ),
        InAppNotificationUi(
            id = "2",
            headline = "\$NVDA",
            body = "is up 5.2% today",
            timeLabel = "1h",
            unread = true,
            viewPositive = true,
            stockSymbolForView = "NVDA",
            isSocialStyle = false
        ),
        InAppNotificationUi(
            id = "3",
            headline = "\$TSLA",
            body = "is down 2.1% since open",
            timeLabel = "2h",
            unread = true,
            viewPositive = false,
            stockSymbolForView = "TSLA",
            isSocialStyle = false
        ),
        InAppNotificationUi(
            id = "4",
            headline = "Alex Rivera",
            body = "commented on your \$GOOGL thread",
            timeLabel = "3h",
            unread = false,
            viewPositive = null,
            stockSymbolForView = null,
            isSocialStyle = true
        )
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
