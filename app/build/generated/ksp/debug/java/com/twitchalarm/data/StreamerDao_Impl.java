package com.twitchalarm.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StreamerDao_Impl implements StreamerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Streamer> __insertionAdapterOfStreamer;

  private final EntityDeletionOrUpdateAdapter<Streamer> __deletionAdapterOfStreamer;

  private final EntityDeletionOrUpdateAdapter<Streamer> __updateAdapterOfStreamer;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLiveStatus;

  public StreamerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStreamer = new EntityInsertionAdapter<Streamer>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `streamers` (`login`,`displayName`,`isLive`,`streamTitle`,`viewerCount`,`gameName`,`notifyEnabled`,`addedAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Streamer entity) {
        statement.bindString(1, entity.getLogin());
        statement.bindString(2, entity.getDisplayName());
        final int _tmp = entity.isLive() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, entity.getStreamTitle());
        statement.bindLong(5, entity.getViewerCount());
        statement.bindString(6, entity.getGameName());
        final int _tmp_1 = entity.getNotifyEnabled() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindLong(8, entity.getAddedAt());
      }
    };
    this.__deletionAdapterOfStreamer = new EntityDeletionOrUpdateAdapter<Streamer>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `streamers` WHERE `login` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Streamer entity) {
        statement.bindString(1, entity.getLogin());
      }
    };
    this.__updateAdapterOfStreamer = new EntityDeletionOrUpdateAdapter<Streamer>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `streamers` SET `login` = ?,`displayName` = ?,`isLive` = ?,`streamTitle` = ?,`viewerCount` = ?,`gameName` = ?,`notifyEnabled` = ?,`addedAt` = ? WHERE `login` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Streamer entity) {
        statement.bindString(1, entity.getLogin());
        statement.bindString(2, entity.getDisplayName());
        final int _tmp = entity.isLive() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, entity.getStreamTitle());
        statement.bindLong(5, entity.getViewerCount());
        statement.bindString(6, entity.getGameName());
        final int _tmp_1 = entity.getNotifyEnabled() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindLong(8, entity.getAddedAt());
        statement.bindString(9, entity.getLogin());
      }
    };
    this.__preparedStmtOfUpdateLiveStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE streamers\n"
                + "        SET isLive = ?,\n"
                + "            streamTitle = ?,\n"
                + "            viewerCount = ?,\n"
                + "            gameName = ?,\n"
                + "            displayName = ?\n"
                + "        WHERE login = ?\n"
                + "    ";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Streamer streamer, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStreamer.insert(streamer);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Streamer streamer, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfStreamer.handle(streamer);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Streamer streamer, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfStreamer.handle(streamer);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLiveStatus(final String login, final boolean isLive, final String title,
      final int viewers, final String game, final String displayName,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLiveStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isLive ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, title);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, viewers);
        _argIndex = 4;
        _stmt.bindString(_argIndex, game);
        _argIndex = 5;
        _stmt.bindString(_argIndex, displayName);
        _argIndex = 6;
        _stmt.bindString(_argIndex, login);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateLiveStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Streamer>> getAllFlow() {
    final String _sql = "SELECT * FROM streamers ORDER BY addedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"streamers"}, new Callable<List<Streamer>>() {
      @Override
      @NonNull
      public List<Streamer> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLogin = CursorUtil.getColumnIndexOrThrow(_cursor, "login");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfIsLive = CursorUtil.getColumnIndexOrThrow(_cursor, "isLive");
          final int _cursorIndexOfStreamTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "streamTitle");
          final int _cursorIndexOfViewerCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewerCount");
          final int _cursorIndexOfGameName = CursorUtil.getColumnIndexOrThrow(_cursor, "gameName");
          final int _cursorIndexOfNotifyEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyEnabled");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<Streamer> _result = new ArrayList<Streamer>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Streamer _item;
            final String _tmpLogin;
            _tmpLogin = _cursor.getString(_cursorIndexOfLogin);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpIsLive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLive);
            _tmpIsLive = _tmp != 0;
            final String _tmpStreamTitle;
            _tmpStreamTitle = _cursor.getString(_cursorIndexOfStreamTitle);
            final int _tmpViewerCount;
            _tmpViewerCount = _cursor.getInt(_cursorIndexOfViewerCount);
            final String _tmpGameName;
            _tmpGameName = _cursor.getString(_cursorIndexOfGameName);
            final boolean _tmpNotifyEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifyEnabled);
            _tmpNotifyEnabled = _tmp_1 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new Streamer(_tmpLogin,_tmpDisplayName,_tmpIsLive,_tmpStreamTitle,_tmpViewerCount,_tmpGameName,_tmpNotifyEnabled,_tmpAddedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAll(final Continuation<? super List<Streamer>> $completion) {
    final String _sql = "SELECT * FROM streamers";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Streamer>>() {
      @Override
      @NonNull
      public List<Streamer> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLogin = CursorUtil.getColumnIndexOrThrow(_cursor, "login");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfIsLive = CursorUtil.getColumnIndexOrThrow(_cursor, "isLive");
          final int _cursorIndexOfStreamTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "streamTitle");
          final int _cursorIndexOfViewerCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewerCount");
          final int _cursorIndexOfGameName = CursorUtil.getColumnIndexOrThrow(_cursor, "gameName");
          final int _cursorIndexOfNotifyEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyEnabled");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<Streamer> _result = new ArrayList<Streamer>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Streamer _item;
            final String _tmpLogin;
            _tmpLogin = _cursor.getString(_cursorIndexOfLogin);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpIsLive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLive);
            _tmpIsLive = _tmp != 0;
            final String _tmpStreamTitle;
            _tmpStreamTitle = _cursor.getString(_cursorIndexOfStreamTitle);
            final int _tmpViewerCount;
            _tmpViewerCount = _cursor.getInt(_cursorIndexOfViewerCount);
            final String _tmpGameName;
            _tmpGameName = _cursor.getString(_cursorIndexOfGameName);
            final boolean _tmpNotifyEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifyEnabled);
            _tmpNotifyEnabled = _tmp_1 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new Streamer(_tmpLogin,_tmpDisplayName,_tmpIsLive,_tmpStreamTitle,_tmpViewerCount,_tmpGameName,_tmpNotifyEnabled,_tmpAddedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByLogin(final String login, final Continuation<? super Streamer> $completion) {
    final String _sql = "SELECT * FROM streamers WHERE login = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, login);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Streamer>() {
      @Override
      @Nullable
      public Streamer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLogin = CursorUtil.getColumnIndexOrThrow(_cursor, "login");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfIsLive = CursorUtil.getColumnIndexOrThrow(_cursor, "isLive");
          final int _cursorIndexOfStreamTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "streamTitle");
          final int _cursorIndexOfViewerCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewerCount");
          final int _cursorIndexOfGameName = CursorUtil.getColumnIndexOrThrow(_cursor, "gameName");
          final int _cursorIndexOfNotifyEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyEnabled");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final Streamer _result;
          if (_cursor.moveToFirst()) {
            final String _tmpLogin;
            _tmpLogin = _cursor.getString(_cursorIndexOfLogin);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpIsLive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLive);
            _tmpIsLive = _tmp != 0;
            final String _tmpStreamTitle;
            _tmpStreamTitle = _cursor.getString(_cursorIndexOfStreamTitle);
            final int _tmpViewerCount;
            _tmpViewerCount = _cursor.getInt(_cursorIndexOfViewerCount);
            final String _tmpGameName;
            _tmpGameName = _cursor.getString(_cursorIndexOfGameName);
            final boolean _tmpNotifyEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifyEnabled);
            _tmpNotifyEnabled = _tmp_1 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _result = new Streamer(_tmpLogin,_tmpDisplayName,_tmpIsLive,_tmpStreamTitle,_tmpViewerCount,_tmpGameName,_tmpNotifyEnabled,_tmpAddedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
