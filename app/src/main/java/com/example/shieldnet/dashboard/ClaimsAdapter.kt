package com.example.shieldnet.dashboard

import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shieldnet.R
import com.example.shieldnet.data.model.ClaimResponse
import java.util.Locale


class ClaimsAdapter : ListAdapter<ClaimResponse, ClaimsAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEvent:  TextView = view.findViewById(R.id.tv_event_type)
        val tvAmount: TextView = view.findViewById(R.id.tv_claim_amount)
        val tvStatus: TextView = view.findViewById(R.id.tv_claim_status)
        val tvDate:   TextView = view.findViewById(R.id.tv_claim_date)

        fun bind(claim: ClaimResponse) {
            tvEvent.text = claim.eventType
                .replace("_", " ")
                .replaceFirstChar { it.uppercase(Locale.ROOT) }

            tvAmount.text = when (claim.status.lowercase(Locale.ROOT)) {
                "paid"     -> "₹${claim.approvedAmount} paid"
                "approved" -> "₹${claim.approvedAmount} approved"
                "pending"  -> "₹${claim.estimatedLoss} (processing)"
                else       -> "Rejected"
            }

            tvStatus.text = claim.status.uppercase(Locale.ROOT)
            tvStatus.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    when (claim.status.lowercase(Locale.ROOT)) {
                        "paid"     -> R.color.status_paid
                        "approved" -> R.color.status_approved
                        "pending"  -> R.color.status_pending
                        else       -> R.color.status_rejected
                    }
                )
            )
            tvDate.text = claim.createdAt.take(10)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_claim, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ClaimResponse>() {
            override fun areItemsTheSame(a: ClaimResponse, b: ClaimResponse) = a.id == b.id
            override fun areContentsTheSame(a: ClaimResponse, b: ClaimResponse) = a == b
        }
    }
}