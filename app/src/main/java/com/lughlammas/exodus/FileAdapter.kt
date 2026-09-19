package com.lughlammas.exodus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class FileAdapter(
    private val onClick: (FileEntry) -> Unit,
    private val onLongClick: (FileEntry, View) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    private val items = mutableListOf<FileEntry>()
    private val dateFormat: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())

    fun submit(list: List<FileEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val name: TextView = itemView.findViewById(R.id.name)
        private val meta: TextView = itemView.findViewById(R.id.meta)

        fun bind(entry: FileEntry) {
            name.text = entry.name
            icon.setImageResource(if (entry.isDirectory) R.drawable.ic_folder else R.drawable.ic_file)
            val sizePart = if (entry.isDirectory) {
                itemView.context.getString(R.string.folder)
            } else {
                formatSize(entry.length)
            }
            val datePart = dateFormat.format(Date(entry.lastModified))
            meta.text = "$sizePart · $datePart"
            itemView.setOnClickListener { onClick(entry) }
            itemView.setOnLongClickListener {
                onLongClick(entry, itemView)
                true
            }
        }
    }

    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format(Locale.US, "%.2f GB", gb)
        }
    }
}
