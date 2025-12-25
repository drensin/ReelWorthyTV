package com.reelworthy.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
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
public final class VideoDao_Impl implements VideoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VideoEntity> __insertionAdapterOfVideoEntity;

  private final EntityInsertionAdapter<PlaylistEntity> __insertionAdapterOfPlaylistEntity;

  public VideoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVideoEntity = new EntityInsertionAdapter<VideoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `videos` (`id`,`title`,`description`,`thumbnailUrl`,`channelTitle`,`publishedAt`,`duration`,`isWatched`,`addedDate`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VideoEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getThumbnailUrl());
        statement.bindString(5, entity.getChannelTitle());
        statement.bindString(6, entity.getPublishedAt());
        if (entity.getDuration() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getDuration());
        }
        final int _tmp = entity.isWatched() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getAddedDate());
      }
    };
    this.__insertionAdapterOfPlaylistEntity = new EntityInsertionAdapter<PlaylistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playlists` (`id`,`title`,`description`,`thumbnailUrl`,`itemCount`,`lastSyncTime`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        if (entity.getThumbnailUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getThumbnailUrl());
        }
        statement.bindLong(5, entity.getItemCount());
        statement.bindLong(6, entity.getLastSyncTime());
      }
    };
  }

  @Override
  public Object insertVideos(final List<VideoEntity> videos,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVideoEntity.insert(videos);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPlaylists(final List<PlaylistEntity> playlists,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlaylistEntity.insert(playlists);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VideoEntity>> getAllVideos() {
    final String _sql = "SELECT * FROM videos ORDER BY addedDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"videos"}, new Callable<List<VideoEntity>>() {
      @Override
      @NonNull
      public List<VideoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfChannelTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "channelTitle");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfAddedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "addedDate");
          final List<VideoEntity> _result = new ArrayList<VideoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VideoEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpThumbnailUrl;
            _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            final String _tmpChannelTitle;
            _tmpChannelTitle = _cursor.getString(_cursorIndexOfChannelTitle);
            final String _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getString(_cursorIndexOfPublishedAt);
            final String _tmpDuration;
            if (_cursor.isNull(_cursorIndexOfDuration)) {
              _tmpDuration = null;
            } else {
              _tmpDuration = _cursor.getString(_cursorIndexOfDuration);
            }
            final boolean _tmpIsWatched;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp != 0;
            final long _tmpAddedDate;
            _tmpAddedDate = _cursor.getLong(_cursorIndexOfAddedDate);
            _item = new VideoEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpThumbnailUrl,_tmpChannelTitle,_tmpPublishedAt,_tmpDuration,_tmpIsWatched,_tmpAddedDate);
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
  public Object getAllVideosList(final Continuation<? super List<VideoEntity>> $completion) {
    final String _sql = "SELECT * FROM videos ORDER BY addedDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VideoEntity>>() {
      @Override
      @NonNull
      public List<VideoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfChannelTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "channelTitle");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfIsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "isWatched");
          final int _cursorIndexOfAddedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "addedDate");
          final List<VideoEntity> _result = new ArrayList<VideoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VideoEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpThumbnailUrl;
            _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            final String _tmpChannelTitle;
            _tmpChannelTitle = _cursor.getString(_cursorIndexOfChannelTitle);
            final String _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getString(_cursorIndexOfPublishedAt);
            final String _tmpDuration;
            if (_cursor.isNull(_cursorIndexOfDuration)) {
              _tmpDuration = null;
            } else {
              _tmpDuration = _cursor.getString(_cursorIndexOfDuration);
            }
            final boolean _tmpIsWatched;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWatched);
            _tmpIsWatched = _tmp != 0;
            final long _tmpAddedDate;
            _tmpAddedDate = _cursor.getLong(_cursorIndexOfAddedDate);
            _item = new VideoEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpThumbnailUrl,_tmpChannelTitle,_tmpPublishedAt,_tmpDuration,_tmpIsWatched,_tmpAddedDate);
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
  public Flow<List<PlaylistEntity>> getAllPlaylists() {
    final String _sql = "SELECT * FROM playlists";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playlists"}, new Callable<List<PlaylistEntity>>() {
      @Override
      @NonNull
      public List<PlaylistEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfItemCount = CursorUtil.getColumnIndexOrThrow(_cursor, "itemCount");
          final int _cursorIndexOfLastSyncTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncTime");
          final List<PlaylistEntity> _result = new ArrayList<PlaylistEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlaylistEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final int _tmpItemCount;
            _tmpItemCount = _cursor.getInt(_cursorIndexOfItemCount);
            final long _tmpLastSyncTime;
            _tmpLastSyncTime = _cursor.getLong(_cursorIndexOfLastSyncTime);
            _item = new PlaylistEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpThumbnailUrl,_tmpItemCount,_tmpLastSyncTime);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
