package com.ethoel.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class AssignmentAdapter(private val dataSet: Array<String>): RecyclerView.Adapter<AssignmentAdapter.ViewHolder>() {

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.assignment_row, parent, false))
    }

    override fun onBindViewHolder(holder: AssignmentAdapter.ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}