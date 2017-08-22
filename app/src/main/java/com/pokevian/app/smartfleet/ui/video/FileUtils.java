/*
 * Copyright (c) 2014. Pokevian Ltd.
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

package com.pokevian.app.smartfleet.ui.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.lib.common.util.StorageFinder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public final class FileUtils {

    private FileUtils() {
    }

    public static ArrayList<File> dir(File dir) {
        return dir(dir, null, false, false);
    }

    public static ArrayList<File> dir(File dir, boolean recursive, boolean excludeDir) {
        return dir(dir, null, recursive, excludeDir);
    }

    public static ArrayList<File> dir(File dir, FileFilter filter, boolean recursive, boolean excludeDir) {
        return dir(dir, filter, null, recursive, excludeDir);
    }

    public static ArrayList<File> dir(File dir, FileFilter filter, Comparator<File> comparator,
                                      boolean recursive, boolean excludeDir) {
        if (!dir.isDirectory()) {
            throw new InvalidParameterException("is not a directory");
        }

        ArrayList<File> list = new ArrayList<File>();
        if (recursive) {
            listFilesRecursively(dir, filter, list, excludeDir);
        } else {
            File[] files = dir.listFiles(filter);
            if (files != null) {
                for (File f : files) {
                    if (!excludeDir || !f.isDirectory()) {
                        list.add(f);
                    }
                }
            }
        }
        if (comparator != null) {
            Collections.sort(list, comparator);
        }

        return list;
    }

    private static void listFilesRecursively(File dir, FileFilter filter, ArrayList<File> list, boolean excludeDir) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles(filter);
        for (File f : files) {
            if (!excludeDir || !f.isDirectory()) {
                list.add(f);
            }
            if (f.isDirectory()) {
                listFilesRecursively(f, filter, list, excludeDir);
            }
        }
    }

    public static int copy(File src, File dst) {
        if (src.isFile() && dst.isFile()) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new BufferedInputStream(new FileInputStream(src));
                out = new BufferedOutputStream(new FileOutputStream(dst));
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
                return 1;
            } catch (IOException e) {
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException e) {
                }
                if (out != null) try {
                    out.close();
                } catch (IOException e) {
                }
            }
        } else if (src.isFile() && dst.isDirectory()) {
            dst.mkdirs();
            File newFile = new File(dst, src.getName());
            try {
                if (newFile.createNewFile()) {
                    return copy(src, newFile);
                }
            } catch (IOException e) {
            }
        } else if (src.isDirectory() && dst.isDirectory()) {
            dst.mkdirs();
            return copyRecursively(src, dst);
        }
        return 0;
    }

    private static int copyRecursively(File src, File dst) {
        int count = 0;
        if (src.isDirectory() && dst.isDirectory()) {
            File newDir = new File(dst, src.getName());
            newDir.mkdirs();
            File[] files = src.listFiles();
            for (File file : files) {
                File newFile = new File(newDir, file.getName());
                if (file.isFile()) {
                    try {
                        if (newFile.createNewFile()) {
                            count += copy(file, newFile);
                        }
                    } catch (IOException e) {
                    }
                } else {
                    count += copyRecursively(file, newFile);
                }
            }
        }
        return count;
    }

    public static boolean move(File src, File dstDir) {
        File newFile = new File(dstDir, src.getName());
        if (src.isDirectory()) {
            newFile.mkdirs();
        }
        return src.renameTo(newFile);
    }

    public static int delete(File f) {
        if (f == null || !f.exists()) {
            return 0;
        }

        int count = 0;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File file : files) {
                count += delete(file);
            }
            f.delete();
        } else {
            if (f.delete()) {
                count++;
            }
        }
        return count;
    }

    public static File exchangeExtension(File f, String ext, String newExt) {
        if (f == null) {
            return null;
        }

        String path = f.getAbsolutePath();
        int index = path.lastIndexOf(ext);
        String newPath = path.substring(0, index) + newExt;
        return new File(newPath);
    }

    public static boolean checkStorage(Context context) {
        File internal = getInternalStorageRoot(context);
        File external = getExternalStorageRoot(context);
        if (internal == null && external == null) {
            return false;
        }
        return true;
    }

    public static File getInternalStorageRoot(Context context) {

        File[] files = ContextCompat.getExternalFilesDirs(context, null);
        if (files != null) {
            return files[0];
        }

        return null;
    }

    @SuppressLint("NewApi")
    public static File getExternalStorageRoot(Context context) {
        if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
            File[] files = context.getExternalFilesDirs(null);
            if (files != null && files.length >= 2) {
                return files[1];
            } else {
                return null;
            }
        }

        return StorageFinder.getExternalStorageRoot();
    }

    public static File ensureDirExist(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getMetadataFileFromVideoFile(File videoFile) {
        return exchangeExtension(videoFile, Consts.DEFAULT_BLACKBOX_VIDEO_FILE_EXT, Consts.DEFAULT_BLACKBOX_METADATA_FILE_EXT);
    }

}
