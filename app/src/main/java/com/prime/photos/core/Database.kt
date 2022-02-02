package com.prime.photos.core

import android.content.Context
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.prime.photos.common.FileUtils
import kotlinx.coroutines.flow.Flow

private const val PHOTO_COLUMN_ID = "photo_id"

// data classes.
@Entity(tableName = "tbl_photos")
data class Photo(
    @JvmField @PrimaryKey @ColumnInfo(name = PHOTO_COLUMN_ID) val id: Long,
    @JvmField val title: String,
    @ColumnInfo(name = "date_added") @JvmField val dateAdded: Long,
    @ColumnInfo(name = "date_modified") @JvmField val dateModified: Long,
    @ColumnInfo(name = "date_taken") @JvmField val dateTaken: Long,
    @ColumnInfo(name = "file_size") @JvmField val size: Int,
    @ColumnInfo(name = "mime_type") @JvmField val mimeType: String,
    @JvmField val desc: String?,
    @JvmField val orientation: Int,
    @JvmField val height: Int,
    @JvmField val width: Int,
    @JvmField val path: String,
    @JvmField @ColumnInfo(name = "parent_path") val parent: String,
    @ColumnInfo @JvmField val latitude: Double,
    @ColumnInfo @JvmField val longitude: Double,
) {
    @DatabaseView(
        viewName = "vw_photos_info",
        value = "SELECT tbl_photos.*, COUNT(*) AS cardinality, MAX(date_modified) AS info_date_modified, " +
                "SUM(file_size) AS info_size FROM tbl_photos ORDER BY date_modified DESC",
    )
    data class Info(
        @Embedded val row: Photo,
        @JvmField val cardinality: Int,
        @ColumnInfo(name = "info_date_modified") val dateModified: Long,
        @ColumnInfo(name = "info_size") val size: Int
    )

    @DatabaseView(
        value = "SELECT tbl_photos.*, parent_path AS bucket_path, COUNT(*) AS cardinality, " +
                "MAX(date_modified) AS bucket_date_modified, SUM(file_size) AS bucket_size FROM tbl_photos " +
                "GROUP BY parent_path ORDER BY date_modified DESC",
        viewName = "vw_photo_bucket"
    )
    data class Bucket(
        @Embedded @JvmField val row: Photo,
        @JvmField @ColumnInfo(name = "bucket_path") val path: String,
        @JvmField val cardinality: Int,
        @JvmField @ColumnInfo(name = "bucket_size") val size: Long, // size in bytes
        @JvmField @ColumnInfo(name = "bucket_date_modified") val dateModified: Long,
    )

    val Bucket.name get() = FileUtils.name(path)
}

private const val VIDEO_COLUMN_ID = "video_id"

@Entity(tableName = "tbl_videos")
data class Video(
    @JvmField @PrimaryKey @ColumnInfo(name = VIDEO_COLUMN_ID) val id: Long,
    @JvmField val title: String,
    @ColumnInfo(name = "date_added") @JvmField val dateAdded: Long,
    @ColumnInfo(name = "date_modified") @JvmField val dateModified: Long,
    @ColumnInfo(name = "date_taken") @JvmField val dateTaken: Long,
    @ColumnInfo(name = "file_size") @JvmField val size: Int,
    @ColumnInfo(name = "mime_type") @JvmField val mimeType: String,
    @JvmField val desc: String?,
    @JvmField val category: String,
    @JvmField val language: String,
    @JvmField val tags: String?,
    @JvmField val duration: Int,
    @JvmField val orientation: Int,
    @JvmField val height: Int,
    @JvmField val width: Int,
    @JvmField val path: String,
    @JvmField @ColumnInfo(name = "parent_path") val parent: String,
    @ColumnInfo @JvmField val latitude: Double,
    @ColumnInfo @JvmField val longitude: Double,
    @ColumnInfo @JvmField val artist: String,
    @ColumnInfo @JvmField val album: String,
    @JvmField val resolution: Float,
    @JvmField val year: Int
) {
    @DatabaseView(
        viewName = "vw_video_info",
        value = "SELECT tbl_videos.*, COUNT(*) AS cardinality, MAX(date_modified) AS bucket_date_modified, " +
                "SUM(file_size) AS info_size FROM tbl_videos ORDER BY date_modified DESC",
    )
    data class Info(
        @Embedded @JvmField val row: Video,
        @JvmField val cardinality: Int,
        @ColumnInfo(name = "bucket_date_modified") @JvmField val dateModified: Long,
        @ColumnInfo(name = "info_size") @JvmField val size: Int
    )

    @DatabaseView(
        value = "SELECT tbl_videos.*, parent_path AS bucket_path, COUNT(*) AS cardinality, " +
                "MAX(date_modified) AS bucket_date_modified, SUM(file_size) AS bucket_size " +
                "FROM tbl_videos GROUP BY parent_path ORDER BY date_modified DESC",
        viewName = "vw_video_bucket"
    )
    data class Bucket(
        @Embedded @JvmField val row: Video,
        @JvmField @ColumnInfo(name = "bucket_path") val path: String,
        @JvmField val cardinality: Int,
        @JvmField @ColumnInfo(name = "bucket_size") val size: Long, // size in bytes
        @JvmField @ColumnInfo(name = "bucket_date_modified") val dateModified: Long,
    )

    val Bucket.name get() = FileUtils.name(path)

    @DatabaseView(
        value = "SELECT tbl_videos.*, artist AS artist_name, COUNT(*) AS tracks, COUNT(DISTINCT album) AS albums, " +
                "SUM(file_size) AS artist_size, SUM(duration) AS artist_duration FROM tbl_videos " +
                "GROUP BY artist ORDER BY date_modified DESC",
        viewName = "vw_video_artist"
    )
    data class Artist(
        @Embedded val row: Video,
        @JvmField @ColumnInfo(name = "artist_name") val name: String,
        @JvmField val tracks: Int,
        @JvmField val albums: Int,
        @JvmField @ColumnInfo(name = "artist_size") val size: Long,
        @JvmField @ColumnInfo(name = "artist_duration") val duration: Int,
    )

    @DatabaseView(
        viewName = "vw_video_album",
        value = "SELECT tbl_videos.*, album AS bucket_title, COUNT(*) AS tracks, COUNT(DISTINCT album) AS albums, " +
                "SUM(file_size) AS album_size, SUM(duration) AS album_duration, MAX(year) AS last_year, MIN(year) AS first_year " +
                "FROM tbl_videos GROUP BY album ORDER BY date_modified DESC"
    )
    data class Album(
        @Embedded val row: Video,
        @JvmField @ColumnInfo(name = "bucket_title") val title: String,
        @JvmField @ColumnInfo(name = "first_year") val firstYear: Int,
        @JvmField @ColumnInfo(name = "last_year") val lastYear: Int,
        @JvmField @ColumnInfo(name = "album_size") val size: Long,
        @JvmField @ColumnInfo(name = "album_duration") val duration: Int,
        @JvmField val tracks: Int,
    )
}

/**
 * The member table of playlist.
 */
private const val TABLE_ALBUM_MEMBER = "tbl_album_members"

private const val ALBUM_COLUMN_ID = "album_id"
private const val MEMBER_COLUMN_ORDER = "member_order"
private const val MEMBER_FILE_ID = "file_id"

/**
 * @param tag Unique among modules like audio player, video player, radio etc.
 */
@Entity(tableName = "tbl_albums")
data class Album(
    @JvmField @PrimaryKey(autoGenerate = true) @ColumnInfo(name = ALBUM_COLUMN_ID) val id: Long = 0,
    @JvmField val name: String,
    @ColumnInfo(defaultValue = "") val desc: String,
    @JvmField val tag: String,
    @JvmField @ColumnInfo(name = "date_created") val dateCreated: Long,
    @JvmField @ColumnInfo(name = "date_modified") val dateModified: Long,
) {
    @Entity(
        tableName = TABLE_ALBUM_MEMBER,
        primaryKeys = [ALBUM_COLUMN_ID, MEMBER_FILE_ID],
        foreignKeys = [
            ForeignKey(
                entity = Photo::class,
                parentColumns = [PHOTO_COLUMN_ID],
                childColumns = [MEMBER_FILE_ID],
                onDelete = CASCADE
            ),
            ForeignKey(
                entity = Album::class,
                parentColumns = [ALBUM_COLUMN_ID],
                childColumns = [ALBUM_COLUMN_ID],
                onDelete = CASCADE
            )
        ],
        indices = [
            Index(
                value = [ALBUM_COLUMN_ID, MEMBER_FILE_ID],
                unique = false
            )
        ]
    )
    data class Member(
        @JvmField @ColumnInfo(name = ALBUM_COLUMN_ID) val albumID: Long,
        @JvmField @ColumnInfo(name = MEMBER_FILE_ID) val id: String,
        @JvmField @ColumnInfo(name = MEMBER_COLUMN_ORDER) val order: Long
    )
}

@Database(
    entities = [Photo::class, Album::class, Album.Member::class, Video::class],
    version = 1,
    exportSchema = false,
    views = [Photo.Bucket::class, Video.Bucket::class, Video.Artist::class, Video.Album::class, Photo.Info::class, Video.Info::class]
)
private abstract class LocalDBImpl : RoomDatabase() {

    abstract val photos: Photos
    abstract val albums: Albums
    abstract val videos: Videos


    companion object {
        private const val DB_NAME = "localdb"

        /**
         * Create triggers in order to manage [MEMBER_COLUMN_PLAY_ORDER]
         */
        private const val TRIGGER = "trigger"

        //language=SQL
        private const val TRIGGER_BEFORE_INSERT =
            "CREATE TRIGGER IF NOT EXISTS ${TRIGGER}_reorder_insert BEFORE INSERT ON $TABLE_ALBUM_MEMBER " +
                    "BEGIN UPDATE $TABLE_ALBUM_MEMBER SET $MEMBER_COLUMN_ORDER = $MEMBER_COLUMN_ORDER + 1 " +
                    "WHERE new.${ALBUM_COLUMN_ID} == $ALBUM_COLUMN_ID AND $MEMBER_COLUMN_ORDER >= new.$MEMBER_COLUMN_ORDER;" +
                    "END;"

        //language=SQL
        private const val TRIGGER_AFTER_DELETE =
            "CREATE TRIGGER IF NOT EXISTS ${TRIGGER}_reorder_delete AFTER DELETE ON $TABLE_ALBUM_MEMBER " +
                    "BEGIN UPDATE $TABLE_ALBUM_MEMBER SET $MEMBER_COLUMN_ORDER = $MEMBER_COLUMN_ORDER - 1 " +
                    "WHERE old.${ALBUM_COLUMN_ID} == $ALBUM_COLUMN_ID AND old.$MEMBER_COLUMN_ORDER < $MEMBER_COLUMN_ORDER;" +
                    "END;"

        private val CALLBACK = object : RoomDatabase.Callback() {

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL(TRIGGER_BEFORE_INSERT)
                db.execSQL(TRIGGER_AFTER_DELETE)
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                /* Just in case not created in onCreate e.g. migration */
                db.execSQL(TRIGGER_BEFORE_INSERT)
                db.execSQL(TRIGGER_AFTER_DELETE)
            }
        }

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: LocalDBImpl? = null

        fun get(context: Context): LocalDBImpl {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDBImpl::class.java,
                    DB_NAME
                )
                    .addCallback(CALLBACK)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}


@Suppress("FunctionName")
fun GetAlbumsDb(context: Context) = LocalDBImpl.get(context).albums

@Suppress("FunctionName")
fun GetPhotosDb(context: Context) = LocalDBImpl.get(context).photos

@Suppress("FunctionName")
fun GetVideosDb(context: Context) = LocalDBImpl.get(context).videos


@Dao
interface Photos {

    @Query("SELECT MAX(date_modified) FROM tbl_photos")
    suspend fun lastModified(): Long?

    @Insert
    fun insert(photo: Photo): Long

    @Insert
    fun insert(values: List<Photo>): List<Long>

    @Query("DELETE FROM tbl_photos WHERE photo_id == :id")
    suspend fun delete(id: Long): Int

    @Suppress("FunctionName")
    @RawQuery(observedEntities = [Photo::class])
    fun __raw_delete(query: SupportSQLiteQuery): Int

    @Suppress("FunctionName")
    @Transaction
    fun _delete(ids: String): Int {
        //language=SQL
        val query = SimpleSQLiteQuery("DELETE FROM tbl_photos WHERE photo_id NOT IN ($ids)")
        return __raw_delete(query)
    }

    @Delete
    suspend fun delete(photo: Photo): Int

    @Query("SELECT * FROM tbl_photos WHERE :query IS NULL OR title LIKE '%' || :query || '%'")
    fun stream(query: String? = null): Flow<List<Photo>>

    @Query("SELECT * FROM tbl_photos WHERE photo_id == :id")
    fun stream(id: Long): Flow<Photo?>

    @Query("SELECT * FROM tbl_photos WHERE :query IS NULL OR title LIKE '%' || :query || '%'")
    suspend fun get(query: String? = null): List<Photo>

    @Query("SELECT * FROM tbl_photos WHERE photo_id == :id")
    suspend fun get(id: Long): Photo?

    @Query("SELECT COUNT(*) FROM tbl_photos")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT photo_id FROM tbl_photos WHERE photo_id == :id)")
    suspend fun exists(id: Long): Boolean

    @Query("SELECT * FROM vw_photo_bucket WHERE :query IS NULL OR bucket_path LIKE '%' || :query || '%'")
    fun buckets(query: String? = null): Flow<List<Photo.Bucket>>

    @Query("SELECT * FROM vw_photo_bucket WHERE bucket_path == :path")
    fun bucket(path: String): Flow<Photo.Bucket?>

    @Query("SELECT * FROM vw_photo_bucket WHERE :query IS NULL OR bucket_path LIKE '%' || :query || '%'")
    suspend fun getBuckets(query: String? = null): List<Photo.Bucket>

    @Query("SELECT * FROM vw_photo_bucket WHERE bucket_path == :path")
    suspend fun getBucket(path: String): Photo.Bucket?

    @Query("SELECT * FROM vw_photos_info")
    fun info(): Flow<Photo.Info>

    @Query("SELECT * FROM vw_photos_info")
    suspend fun getInfo(): Photo.Info
}

@Dao
interface Videos {

    @Query("SELECT MAX(date_modified) FROM tbl_videos")
    suspend fun lastModified(): Long?

    @Insert
    fun insert(video: Video): Long

    @Insert
    fun insert(values: List<Video>): List<Long>

    @Query("DELETE FROM tbl_videos WHERE video_id == :id")
    suspend fun delete(id: Long): Int

    @Suppress("FunctionName")
    @RawQuery(observedEntities = [Video::class])
    fun __raw_delete(query: SupportSQLiteQuery): Int

    @Suppress("FunctionName")
    @Transaction
    fun _delete(ids: String): Int {
        //language=SQL
        val query = SimpleSQLiteQuery("DELETE FROM tbl_videos WHERE video_id NOT IN ($ids)")
        return __raw_delete(query)
    }

    @Delete
    suspend fun delete(video: Video): Int

    @Query("SELECT * FROM tbl_videos WHERE :query IS NULL OR title LIKE '%' || :query || '%'")
    fun stream(query: String? = null): Flow<List<Video>>

    @Query("SELECT * FROM tbl_videos WHERE video_id == :id")
    fun stream(id: Long): Flow<Video?>

    @Query("SELECT * FROM tbl_videos WHERE :query IS NULL OR title LIKE '%' || :query || '%'")
    suspend fun get(query: String? = null): List<Video>

    @Query("SELECT * FROM tbl_videos WHERE video_id == :id")
    suspend fun get(id: Long): Video?

    @Query("SELECT COUNT(*) FROM tbl_videos")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT video_id FROM tbl_videos WHERE video_id == :id)")
    suspend fun exists(id: Long): Boolean

    @Query("SELECT * FROM vw_video_bucket WHERE :query IS NULL OR bucket_path LIKE '%' || :query || '%'")
    fun buckets(query: String? = null): Flow<List<Video.Bucket>>

    @Query("SELECT * FROM vw_video_bucket WHERE bucket_path == :path")
    fun bucket(path: String): Flow<Video.Bucket?>

    @Query("SELECT * FROM vw_video_bucket WHERE :query IS NULL OR bucket_path LIKE '%' || :query || '%'")
    suspend fun getBuckets(query: String? = null): List<Video.Bucket>

    @Query("SELECT * FROM vw_video_bucket WHERE bucket_path == :path")
    suspend fun getBucket(path: String): Video.Bucket?

    //artists
    @Query("SELECT * FROM vw_video_artist WHERE :query IS NULL OR artist_name LIKE '%' || :query || '%'")
    fun artists(query: String? = null): Flow<List<Video.Artist>>

    @Query("SELECT * FROM vw_video_artist WHERE artist_name == :name")
    fun artist(name: String): Flow<Video.Artist?>

    @Query("SELECT * FROM vw_video_artist WHERE :query IS NULL OR artist_name LIKE '%' || :query || '%'")
    suspend fun getArtists(query: String? = null): List<Video.Artist>

    @Query("SELECT * FROM vw_video_artist WHERE artist_name == :name")
    suspend fun getArtist(name: String): Video.Artist?

    //album
    //artists
    @Query("SELECT * FROM vw_video_album WHERE :query IS NULL OR album LIKE '%' || :query || '%'")
    fun albums(query: String? = null): Flow<List<Video.Album>>

    @Query("SELECT * FROM vw_video_album WHERE album == :name")
    fun album(name: String): Flow<Video.Album?>

    @Query("SELECT * FROM vw_video_album WHERE :query IS NULL OR album LIKE '%' || :query || '%'")
    suspend fun getAlbums(query: String? = null): List<Video.Album>

    @Query("SELECT * FROM vw_video_album WHERE album == :name")
    suspend fun getAlbum(name: String): Video.Album?

    @Query("SELECT * FROM vw_video_info")
    fun info(): Flow<Video.Info>

    @Query("SELECT * FROM vw_video_info")
    suspend fun getInfo(): Video.Info
}


@Dao
interface Albums