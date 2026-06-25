package com.twitchalarm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.twitchalarm.R
import com.twitchalarm.api.TwitchApi
import com.twitchalarm.data.AppDatabase
import com.twitchalarm.data.Streamer
import com.twitchalarm.databinding.ActivityMainBinding
import com.twitchalarm.work.BootReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StreamerAdapter
    private lateinit var db: AppDatabase

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) showNotifPermDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        setupAddButton()
        setupSwipeToDelete()
        observeStreamers()
        requestNotificationPermission()

        // Запускаем фоновую проверку
        BootReceiver.scheduleWork(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = ""
    }

    private fun setupRecyclerView() {
        adapter = StreamerAdapter(
            onToggle = { streamer, enabled ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.streamerDao().update(streamer.copy(notifyEnabled = enabled))
                }
            },
            onDelete = { streamer -> confirmDelete(streamer) },
            onTestAlarm = { streamer -> testAlarm(streamer) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupAddButton() {
        binding.btnAdd.setOnClickListener {
            val input = binding.etNickname.text?.toString()?.trim()?.lowercase() ?: ""
            if (input.isEmpty()) {
                binding.etNickname.error = "Введи ник стримера"
                return@setOnClickListener
            }
            addStreamer(input)
        }

        // Добавление по Enter
        binding.etNickname.setOnEditorActionListener { _, _, _ ->
            binding.btnAdd.performClick()
            true
        }
    }

    private fun addStreamer(login: String) {
        binding.btnAdd.isEnabled = false
        binding.progressAdd.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Проверяем, не добавлен ли уже
            val existing = withContext(Dispatchers.IO) {
                db.streamerDao().getByLogin(login)
            }
            if (existing != null) {
                Toast.makeText(this@MainActivity, "Стример уже в списке", Toast.LENGTH_SHORT).show()
                resetAddButton()
                return@launch
            }

            // Проверяем, существует ли стример на Twitch
            val info = withContext(Dispatchers.IO) { TwitchApi.checkStream(login) }

            if (info == null) {
                Toast.makeText(this@MainActivity, "Ошибка сети. Проверь интернет.", Toast.LENGTH_SHORT).show()
                resetAddButton()
                return@launch
            }

            withContext(Dispatchers.IO) {
                db.streamerDao().insert(
                    Streamer(
                        login       = login,
                        displayName = info.displayName.ifEmpty { login },
                        isLive      = info.isLive,
                        streamTitle = info.title,
                        viewerCount = info.viewerCount,
                        gameName    = info.gameName
                    )
                )
            }

            binding.etNickname.setText("")

            val msg = if (info.isLive) "✅ ${info.displayName} добавлен — сейчас в эфире!"
                      else "✅ ${info.displayName.ifEmpty { login }} добавлен"
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            resetAddButton()
        }
    }

    private fun resetAddButton() {
        binding.btnAdd.isEnabled = true
        binding.progressAdd.visibility = View.GONE
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val streamer = adapter.currentList[vh.adapterPosition]
                confirmDelete(streamer)
                adapter.notifyItemChanged(vh.adapterPosition)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)
    }

    private fun confirmDelete(streamer: Streamer) {
        AlertDialog.Builder(this)
            .setTitle("Удалить стримера?")
            .setMessage("${streamer.displayName.ifEmpty { streamer.login }} будет удалён из списка.")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.streamerDao().delete(streamer)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun testAlarm(streamer: Streamer) {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_STREAMER, streamer.displayName.ifEmpty { streamer.login })
            putExtra(AlarmActivity.EXTRA_TITLE,    "Тест будильника")
            putExtra(AlarmActivity.EXTRA_GAME,     "Minecraft")
            putExtra(AlarmActivity.EXTRA_VIEWERS,  12345)
        }
        startActivity(intent)
    }

    private fun observeStreamers() {
        lifecycleScope.launch {
            db.streamerDao().getAllFlow().collect { streamers ->
                adapter.submitList(streamers)
                binding.tvEmpty.visibility = if (streamers.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showNotifPermDialog() {
        AlertDialog.Builder(this)
            .setTitle("Нужно разрешение на уведомления")
            .setMessage("Без уведомлений будильник не сработает. Открыть настройки?")
            .setPositiveButton("Настройки") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("Пропустить", null)
            .show()
    }
}
