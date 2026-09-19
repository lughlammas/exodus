package com.lughlammas.exodus

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var pathText: TextView
    private lateinit var fileList: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var fabZip: FloatingActionButton
    private lateinit var adapter: FileAdapter

    private var currentDir: File = Environment.getExternalStorageDirectory()
    private var selectedForZip: File? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshList() }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshList() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        pathText = findViewById(R.id.pathText)
        fileList = findViewById(R.id.fileList)
        emptyText = findViewById(R.id.emptyText)
        fabZip = findViewById(R.id.fabZip)

        setSupportActionBar(toolbar)

        adapter = FileAdapter(
            onClick = { entry ->
                if (entry.isDirectory) {
                    navigateTo(entry.file)
                } else {
                    openFile(entry.file)
                }
            },
            onLongClick = { entry, view -> showItemMenu(entry, view) }
        )
        fileList.layoutManager = LinearLayoutManager(this)
        fileList.adapter = adapter

        fabZip.setOnClickListener {
            val target = selectedForZip?.takeIf { it.isDirectory } ?: currentDir
            showZipDialog(target)
        }

        ensureStorageAccess()
        setupBackNavigation()
        navigateTo(preferredStartDir())
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun preferredStartDir(): File {
        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return when {
            dl.exists() -> dl
            Environment.getExternalStorageDirectory().exists() -> Environment.getExternalStorageDirectory()
            else -> filesDir
        }
    }

    private fun ensureStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.need_permission)
                    .setPositiveButton(R.string.action_permission) { _, _ -> openManageAllFilesSettings() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        } else {
            val need = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                need += Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (Build.VERSION.SDK_INT <= 28 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                need += Manifest.permission.WRITE_EXTERNAL_STORAGE
            }
            if (need.isNotEmpty()) {
                permissionLauncher.launch(need.toTypedArray())
            }
        }
    }

    private fun openManageAllFilesSettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            manageStorageLauncher.launch(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            manageStorageLauncher.launch(intent)
        }
    }

    private fun navigateTo(dir: File) {
        val canonical = try {
            dir.canonicalFile
        } catch (_: Exception) {
            dir
        }
        if (!canonical.exists() || !canonical.isDirectory) {
            toast(getString(R.string.error_open))
            return
        }
        currentDir = canonical
        selectedForZip = canonical
        refreshList()
    }

    private fun refreshList() {
        pathText.text = currentDir.absolutePath
        val listed = currentDir.listFiles()?.toList().orEmpty()
        val entries = listed
            .map { FileEntry(it) }
            .sortedWith(compareBy<FileEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
        adapter.submit(entries)
        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showItemMenu(entry: FileEntry, anchor: View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_item, popup.menu)
        popup.menu.findItem(R.id.action_zip).isVisible = entry.isDirectory
        popup.menu.findItem(R.id.action_share).isVisible = !entry.isDirectory || entry.file.extension.equals("zip", true)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open -> {
                    if (entry.isDirectory) navigateTo(entry.file) else openFile(entry.file)
                    true
                }
                R.id.action_zip -> {
                    showZipDialog(entry.file)
                    true
                }
                R.id.action_share -> {
                    shareFile(entry.file)
                    true
                }
                R.id.action_copy_path -> {
                    copyPath(entry.file.absolutePath)
                    true
                }
                R.id.action_rename -> {
                    promptRename(entry.file)
                    true
                }
                R.id.action_delete -> {
                    confirmDelete(entry.file)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showZipDialog(folder: File) {
        if (!folder.isDirectory) {
            toast(getString(R.string.select_folder_first))
            return
        }
        val options = arrayOf(
            getString(R.string.zip_dest_parent),
            getString(R.string.zip_dest_download)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.zip_title)
            .setItems(options) { _, which ->
                val destDir = when (which) {
                    0 -> folder.parentFile ?: folder
                    else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
                destDir.mkdirs()
                val zipName = folder.name + ".zip"
                val destZip = uniqueZipFile(destDir, zipName)
                runZip(folder, destZip)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun uniqueZipFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val base = name.removeSuffix(".zip")
        var i = 2
        while (true) {
            candidate = File(dir, "${base}_$i.zip")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    private fun runZip(source: File, dest: File) {
        val progress = AlertDialog.Builder(this)
            .setTitle(R.string.zip_title)
            .setMessage(R.string.zipping)
            .setCancelable(false)
            .create()
        progress.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { ZipUtils.zipFolder(source, dest); dest }
            }
            progress.dismiss()
            result.onSuccess { zip ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.zip_title)
                    .setMessage(getString(R.string.zip_done, zip.absolutePath))
                    .setPositiveButton(R.string.action_share) { _, _ -> shareFile(zip) }
                    .setNegativeButton(R.string.ok) { _, _ -> refreshList() }
                    .show()
                refreshList()
            }.onFailure { e ->
                toast(getString(R.string.zip_fail, e.message ?: e.toString()))
            }
        }
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mime = mimeFor(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(contentResolver, file.name, uri)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
        } catch (e: Exception) {
            toast(e.message ?: getString(R.string.error_open))
        }
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeFor(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.action_open)))
        } catch (e: Exception) {
            toast(e.message ?: getString(R.string.error_open))
        }
    }

    private fun mimeFor(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    private fun copyPath(path: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("path", path))
        toast(getString(R.string.path_copied))
    }

    private fun promptMkdir() {
        val input = EditText(this).apply {
            hint = getString(R.string.name_hint)
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.mkdir_title)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val dir = File(currentDir, name)
                if (dir.mkdirs()) refreshList() else toast(getString(R.string.error_open))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun promptRename(file: File) {
        val input = EditText(this).apply {
            setText(file.name)
            setSelection(file.name.length)
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_title)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty() || name == file.name) return@setPositiveButton
                val dest = File(file.parentFile, name)
                if (file.renameTo(dest)) refreshList() else toast(getString(R.string.error_open))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete, file.name))
            .setPositiveButton(R.string.yes) { _, _ ->
                val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
                if (ok) refreshList() else toast(getString(R.string.error_open))
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_up -> {
                currentDir.parentFile?.let { navigateTo(it) }
                true
            }
            R.id.action_refresh -> {
                refreshList()
                true
            }
            R.id.action_mkdir -> {
                promptMkdir()
                true
            }
            R.id.action_zip -> {
                showZipDialog(currentDir)
                true
            }
            R.id.action_copy_path -> {
                copyPath(currentDir.absolutePath)
                true
            }
            R.id.action_goto_download -> {
                navigateTo(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                true
            }
            R.id.action_goto_documents -> {
                navigateTo(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
                true
            }
            R.id.action_goto_storage -> {
                navigateTo(Environment.getExternalStorageDirectory())
                true
            }
            R.id.action_permission -> {
                ensureStorageAccess()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) openManageAllFilesSettings()
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val parent = currentDir.parentFile
                val root = Environment.getExternalStorageDirectory()
                if (parent != null && currentDir.absolutePath != root.absolutePath &&
                    currentDir.absolutePath.startsWith(root.absolutePath)
                ) {
                    navigateTo(parent)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
