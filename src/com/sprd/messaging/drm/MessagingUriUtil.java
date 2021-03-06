package com.sprd.messaging.drm;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.EnvironmentEx;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import com.android.messaging.util.UriUtil;
import com.android.messaging.datamodel.MediaScratchFileProvider;

import java.io.File;

public class MessagingUriUtil {
    private static String TAG = "MessagingUriUtil";

    private static String mDefaultDrmPath = "/storage/emulated/0";
    public static String getPath(final Context context, final Uri uri) {
        // DocumentProvider
        Log.d(TAG, " uri = " + uri);
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];
            Log.d(TAG,"entent getPath isExternalStorageDocument type = " + type + "  docId = " + docId);

            final StorageManager storage = context.getSystemService(StorageManager.class);
            VolumeInfo info = null;
            if ("primary".equalsIgnoreCase(type)) {
                info = storage.findVolumeById(VolumeInfo.ID_EMULATED_INTERNAL);
            } else {
                info = storage.findVolumeByUuid(type);
            }
            final int userId = UserHandle.myUserId();
            String path = null;
            if (info != null) {
                File file = info.getPathForUser(userId);
                if (file != null) {
                    path = file.getPath();
                }
                Log.d(TAG,"getPath VolumeInfo path = " + path);
            }

            if (split.length > 1) {
                path = path + "/" + split[1];
            }
            return path;
        }
        // DownloadsProvider
        else if (isDownloadsDocument(uri)) {

            final String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/all_downloads"),  //Bug 1010130 public_downloads -> all_downloads
                    Long.valueOf(id));
            Log.d(TAG, " isDownloadsDocument contentUri = " + contentUri);
            return getDataColumn(context, contentUri, null, null);
        } else if (isContacts(uri)) { // contacts
            return null;
        }
        // MediaProvider
        else if (isMediaDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];
            Log.d(TAG, " isMediaDocument type = " + type);
            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
            final String selection = "_id=?";
            final String[] selectionArgs = new String[]{split[1]};
            return getDataColumn(context, contentUri, selection, selectionArgs);
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, " isContentScheme ");
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            if (isMessagingMediaScrathUri(uri)) {
                String path = MediaScratchFileProvider.getFileFromUri(uri)
                        .getAbsolutePath();
                Log.d(TAG, " isMessagingMediaScrathUri path is " + path);
                return path;
            }
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, " isFileSheme ");
            return uri.getPath();
        }
        Log.d(TAG, " return null ");
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            if ("com.sprd.fileexplorer.fileProvider".equals(uri.getAuthority())) {
                if (uri.getPath() != null && uri.getPath().startsWith("/storage_root")) {
                    return uri.getPath().replaceFirst("/storage_root", "");
                }
                return uri.getPath();
            }
            throw e;
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is contacts
     */
    public static boolean isContacts(Uri uri) {
        return "com.android.contacts".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return uri.getAuthority().startsWith("com.google.android.apps");
    }

    public static boolean isMessagingMediaScrathUri(Uri uri) {
        return uri.getAuthority().startsWith("com.android.messaging");
    }
}
