package com.example.umbilotemplefrontend.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.BankAccount

/**
 * Adapter for displaying bank accounts in admin panel
 */
class BankAccountsAdapter(
    private var bankAccounts: List<BankAccount>,
    private val onEdit: (BankAccount) -> Unit,
    private val onDelete: (BankAccount) -> Unit
) : RecyclerView.Adapter<BankAccountsAdapter.BankAccountViewHolder>() {

    class BankAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBankName: TextView = itemView.findViewById(R.id.tvBankName)
        val tvAccountNumber: TextView = itemView.findViewById(R.id.tvAccountNumber)
        val tvBranchCode: TextView = itemView.findViewById(R.id.tvBranchCode)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankAccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bank_admin, parent, false)
        return BankAccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: BankAccountViewHolder, position: Int) {
        val account = bankAccounts[position]
        
        holder.tvBankName.text = account.bankName
        holder.tvAccountNumber.text = "Account: ${account.accountNumber}"
        holder.tvBranchCode.text = "Branch: ${account.branchCode}"
        
        holder.btnEdit.setOnClickListener {
            onEdit(account)
        }
        
        holder.btnDelete.setOnClickListener {
            onDelete(account)
        }
    }

    override fun getItemCount(): Int = bankAccounts.size

    /**
     * Update the list of bank accounts and refresh the view
     */
    fun updateBankAccounts(newAccounts: List<BankAccount>) {
        bankAccounts = newAccounts
        notifyDataSetChanged()
    }
}

