package com.twitchalarm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
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

        BootReceiver.scheduleWork(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_check_now -> {
                forceCheckNow()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun forceCheckNow() {
        Toast.makeText(this, "🔄 Проверка запущена...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            val streamers = withContext(Dispatchers.IO) {
                db.streamerDao().getAll()
            }
            
            if (streamers.isEmpty()) {
                Toast.makeText(this@MainActivity, "📭 Нет стримеров для проверки", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val logins = streamers.filter { it.notifyEnabled }.map { it.login }
            if (logins.isEmpty()) {
                Toast.makeText(this@MainActivity, "⚠️ Все стримеры отключены", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            Toast.makeText(this@MainActivity, "🔍 Проверяем  стримеров...", Toast.LENGTH_SHORT).show()
            
            val results = withContext(Dispatchers.IO) {
                TwitchApi.checkStreams(logins)
            }
            
            var foundLive = false
            results.forEach { info ->
                val prev = withContext(Dispatchers.IO) {
                    db.streamerDao().getByLogin(info.login)
                } ?: return@forEach
                
                withContext(Dispatchers.IO) {
                    db.streamerDao().updateLiveStatus(
                        login       = info.login,
                        isLive      = info.isLive,
                        title       = info.title,
                        viewers     = info.viewerCount,
                        game        = info.gameName,
                        displayName = info.displayName
                    )
                }
                
                if (info.isLive && prev.notifyEnabled) {
                    foundLive = true
                    showAlarmForStreamer(
                        displayName = info.displayName,
                        title = info.title,
                        game = info.gameName,
                        viewers = info.viewerCount
                    )
                }
            }
            
            val msg = if (foundLive) {
                "🔴 Найден стример в эфире!"
            } else {
                "✅ Все стримеры офлайн"
            }
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun showAlarmForStreamer(displayName: String, title: String, game: String, viewers: Int) {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_STREAMER, displayName)
            putExtra(AlarmActivity.EXTRA_TITLE, title)
            putExtra(AlarmActivity.EXTRA_GAME, game)
            putExtra(AlarmActivity.EXTRA_VIEWERS, viewers)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
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

        binding.etNickname.setOnEditorActionListener { _, _, _ ->
            binding.btnAdd.performClick()
            true
        }
    }

    private fun addStreamer(login: String) {
        binding.btnAdd.isEnabled = false
        binding.progressAdd.visibility = View.VISIBLE

        lifecycleScope.launch {
            val existing = withContext(Dispatchers.IO) {
                db.streamerDao().getByLogin(login)
            }
            if (existing != null) {
                Toast.makeText(this@MainActivity, "Стример уже в списке", Toast.LENGTH_SHORT).show()
                resetAddButton()
                return@launch
            }

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

            val msg = if (info.isLive) {
                "🔴  уже в эфире!"
            } else {
                "✅  добавлен"
            }
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
            .setMessage(" будет удалён из списка.")
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
