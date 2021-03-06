/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.provider.DocumentsContract;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class FileUtil {
    /** Returns a new file name, ensuring that such a file does not already exist. */
    private static synchronized File getNewFile(File directory, String extension,
            String fileNameFormat) throws IOException {
        final Date date = new Date(System.currentTimeMillis());
        final SimpleDateFormat dateFormat = new SimpleDateFormat(fileNameFormat);
        final String numberedFileNameFormat = dateFormat.format(date) + "_%02d"
                + (TextUtils.isEmpty(extension) ? "" : ( "." + extension));
        for (int i = 1; i <= 99; i++) { // Only save 99 of the same file name.
            final String newName = String.format(Locale.US, numberedFileNameFormat, i);
            File testFile = new File(directory, newName);
            if (!testFile.exists()) {
                testFile.createNewFile();
                return testFile;
            }
        }
        LogUtil.e(LogUtil.BUGLE_TAG, "Too many duplicate file names: " + numberedFileNameFormat);
        return null;
    }

    //Bug 912660 begin
    private static synchronized Uri getNewFile(ContentResolver contentResolver, Uri targetUri, String contentType,
                                                String fileNameFormat) {
        final Date date = new Date(System.currentTimeMillis());
        final SimpleDateFormat dateFormat = new SimpleDateFormat(fileNameFormat);
        final String extension = contentType2Extension(contentType);
        final String numberedFileNameFormat = dateFormat.format(date) + "_%02d"
                + (TextUtils.isEmpty(extension) ? "" : ( "." + extension));
        Uri doc = DocumentsContract.buildDocumentUriUsingTree(targetUri, DocumentsContract.getTreeDocumentId(targetUri));
        Uri uri = null;
        for (int i = 1; i <= 99; i++) { // Only save 99 of the same file name.
            try {
                final String newName = String.format(Locale.US, numberedFileNameFormat, i);
                uri = DocumentsContract.createDocument(contentResolver, doc, contentType, newName);
            } catch (Exception e) {
                LogUtil.e(LogUtil.BUGLE_TAG, "create new file failed", e);
            }
            if (uri != null) {
                return uri;
            }
        }
        LogUtil.e(LogUtil.BUGLE_TAG, "Too many duplicate file names: " + numberedFileNameFormat);
        return null;
    }
    //Bug 912660 end

    /**
     * Creates an unused name to use for creating a new file. The format happens to be similar
     * to that used by the Android camera application.
     *
     * @param directory directory that the file should be saved to
     * @param contentType of the media being saved
     * @return file name to be used for creating the new file. The caller is responsible for
     *   actually creating the file.
     */
    public static File getNewFile(File directory, String contentType) throws IOException {
        return getNewFile(directory, contentType2Extension(contentType), getFileNameFormat(contentType));
    }

    //Bug 912660 begin
    public static Uri getNewFile(ContentResolver contentResolever, Uri targetUri, String contentType) {
        return getNewFile(contentResolever, targetUri, contentType, getFileNameFormat(contentType));
    }
    //Bug 912660 end

    private static String getFileNameFormat(String contentType) {
        /* Modify by SPRD for Bug:539365 2016.03.11 Start */
//        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
//        String fileExtension = mimeTypeMap.getExtensionFromMimeType(contentType);
//
//        final Context context = Factory.get().getApplicationContext();
//        String fileNameFormat = context.getString(ContentType.isImageType(contentType)
//                ? R.string.new_image_file_name_format : R.string.new_file_name_format);
        final Context context = Factory.get().getApplicationContext();
        String fileNameFormat = "";
        if (ContentType.isImageType(contentType)) {
            fileNameFormat = context.getString(R.string.new_image_file_name_format);
        } else {
            fileNameFormat = context.getString(R.string.new_file_name_format);
            if (ContentType.isVideoType(contentType)) {
                fileNameFormat = "'VID_'" + fileNameFormat;
            } else {
                fileNameFormat = "'OTH_'" + fileNameFormat;
            }
        }
        /* Modify by SPRD for Bug:539365 2016.03.11 End */
        return fileNameFormat;
    }

    private static String contentType2Extension(String contentType) {
        if (contentType != null) {
            // Change to lower case, @see libcore/luni/src/main/java/libcore/net/MimeUtils.java
            contentType = contentType.toLowerCase();
        }
        String fileExtension = "";
        // Handle system unsupported mime-type.
        // System can not return all medias' mime type,
        // @see libcore/luni/src/main/java/libcore/net/MimeUtils.java,
        // method add(String mimeType, String extension).
        if (ContentType.VIDEO_MOV.equals(contentType)) {
            fileExtension = ContentType.VIDEO_MOV_EXTENSION;
        } else if (ContentType.VIDEO_MP4.equals(contentType)) {
            fileExtension = contentType.replaceFirst("\\w+/", "");
        } else {
            // Handle system supported mime-type.
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            fileExtension = mimeTypeMap.getExtensionFromMimeType(contentType);
        }
        if (TextUtils.isEmpty(fileExtension)) {
            // Make file extension to be last part of the mime-type.
            if (contentType != null && contentType.matches("\\w+/\\w+")) {
                fileExtension = contentType.replaceFirst("\\w+/", "");
            }
        }
        if (fileExtension == null) {
            fileExtension = "";
        }
        LogUtil.d(LogUtil.BUGLE_TAG, "lxg - contentType2Extension: contentType="
                + contentType + ", fileExtension=" + fileExtension);
        return fileExtension;
    }

    /** Delete everything below and including root */
    public static void removeFileOrDirectory(File root) {
        removeFileOrDirectoryExcept(root, null);
    }

    /** Delete everything below and including root except for the given file */
    public static void removeFileOrDirectoryExcept(File root, File exclude) {
        if (root.exists()) {
            if (root.isDirectory()) {
                for (File file : root.listFiles()) {
                    if (exclude == null || !file.equals(exclude)) {
                        removeFileOrDirectoryExcept(file, exclude);
                    }
                }
                root.delete();
            } else if (root.isFile()) {
                root.delete();
            }
        }
    }

    /**
     * Move all files and folders under a directory into the target.
     */
    public static void moveAllContentUnderDirectory(File sourceDir, File targetDir) {
        if (sourceDir.isDirectory() && targetDir.isDirectory()) {
            if (isSameOrSubDirectory(sourceDir, targetDir)) {
                LogUtil.e(LogUtil.BUGLE_TAG, "Can't move directory content since the source " +
                        "directory is a parent of the target");
                return;
            }
            for (File file : sourceDir.listFiles()) {
                if (file.isDirectory()) {
                    final File dirTarget = new File(targetDir, file.getName());
                    dirTarget.mkdirs();
                    moveAllContentUnderDirectory(file, dirTarget);
                } else {
                    try {
                        final File fileTarget = new File(targetDir, file.getName());
                        Files.move(file, fileTarget);
                    } catch (IOException e) {
                        LogUtil.e(LogUtil.BUGLE_TAG, "Failed to move files", e);
                        // Try proceed with the next file.
                    }
                }
            }
        }
    }

    private static boolean isFileUri(final Uri uri) {
        return TextUtils.equals(uri.getScheme(), ContentResolver.SCHEME_FILE);
    }

    // Checks if the file is in /data, and don't allow any app to send personal information.
    // We're told it's possible to create world readable hardlinks to other apps private data
    // so we ban all /data file uris.
    public static boolean isInPrivateDir(Uri uri) {
        if (!isFileUri(uri)) {
            return false;
        }
        final File file = new File(uri.getPath());
        return FileUtil.isSameOrSubDirectory(Environment.getDataDirectory(), file);
    }

    // Checks if the file is in /data, and don't allow any app to send personal information.
    // We're told it's possible to create world readable hardlinks to other apps private data
    // so we ban all /data file uris. b/28793303
    public static boolean isInDataDir(File file) {
        return isSameOrSubDirectory(Environment.getDataDirectory(), file);
    }

    /**
     * Checks, whether the child directory is the same as, or a sub-directory of the base
     * directory.
     */
    private static boolean isSameOrSubDirectory(File base, File child) {
        try {
            base = base.getCanonicalFile();
            child = child.getCanonicalFile();
            File parentFile = child;
            while (parentFile != null) {
                if (base.equals(parentFile)) {
                    return true;
                }
                parentFile = parentFile.getParentFile();
            }
            return false;
        } catch (IOException ex) {
            LogUtil.e(LogUtil.BUGLE_TAG, "Error while accessing file", ex);
            return false;
        }
    }
}
