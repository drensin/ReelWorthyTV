package com.reelworthy.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile VideoDao _videoDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `videos` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, `channelTitle` TEXT NOT NULL, `publishedAt` TEXT NOT NULL, `duration` TEXT, `isWatched` INTEGER NOT NULL, `addedDate` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `thumbnailUrl` TEXT, `itemCount` INTEGER NOT NULL, `lastSyncTime` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ea59ed2d78cd204fce149ad697ad1d2d')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `videos`");
        db.execSQL("DROP TABLE IF EXISTS `playlists`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsVideos = new HashMap<String, TableInfo.Column>(9);
        _columnsVideos.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("thumbnailUrl", new TableInfo.Column("thumbnailUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("channelTitle", new TableInfo.Column("channelTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("publishedAt", new TableInfo.Column("publishedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("duration", new TableInfo.Column("duration", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("isWatched", new TableInfo.Column("isWatched", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("addedDate", new TableInfo.Column("addedDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVideos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVideos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVideos = new TableInfo("videos", _columnsVideos, _foreignKeysVideos, _indicesVideos);
        final TableInfo _existingVideos = TableInfo.read(db, "videos");
        if (!_infoVideos.equals(_existingVideos)) {
          return new RoomOpenHelper.ValidationResult(false, "videos(com.reelworthy.data.VideoEntity).\n"
                  + " Expected:\n" + _infoVideos + "\n"
                  + " Found:\n" + _existingVideos);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaylists = new HashMap<String, TableInfo.Column>(6);
        _columnsPlaylists.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("thumbnailUrl", new TableInfo.Column("thumbnailUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("itemCount", new TableInfo.Column("itemCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("lastSyncTime", new TableInfo.Column("lastSyncTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylists = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylists = new TableInfo("playlists", _columnsPlaylists, _foreignKeysPlaylists, _indicesPlaylists);
        final TableInfo _existingPlaylists = TableInfo.read(db, "playlists");
        if (!_infoPlaylists.equals(_existingPlaylists)) {
          return new RoomOpenHelper.ValidationResult(false, "playlists(com.reelworthy.data.PlaylistEntity).\n"
                  + " Expected:\n" + _infoPlaylists + "\n"
                  + " Found:\n" + _existingPlaylists);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "ea59ed2d78cd204fce149ad697ad1d2d", "b0189b055cae63029c97e1285afa959f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "videos","playlists");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `videos`");
      _db.execSQL("DELETE FROM `playlists`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(VideoDao.class, VideoDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public VideoDao videoDao() {
    if (_videoDao != null) {
      return _videoDao;
    } else {
      synchronized(this) {
        if(_videoDao == null) {
          _videoDao = new VideoDao_Impl(this);
        }
        return _videoDao;
      }
    }
  }
}
