package dev.yong.wheel.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author coderyong
 */
class FileCache {

    private final File mCacheDir;
    private final long mMaxSize;
    private final int mMaxCount;
    private final AtomicLong mCacheSize;
    private final AtomicInteger mCacheCount;
    private final Map<String, File> mCacheFiles = Collections.synchronizedMap(new HashMap<>());
    private final Map<File, Long> mLastModified = Collections.synchronizedMap(new HashMap<>());

    FileCache(File cacheDir, long maxSize, int maxCount) {
        this.mCacheDir = cacheDir;
        this.mMaxSize = maxSize;
        this.mMaxCount = maxCount;
        this.mCacheSize = new AtomicLong();
        this.mCacheCount = new AtomicInteger();
        calculateCacheSizeAndCacheCount();
    }

    /**
     * 添加缓存文件
     *
     * @param key     缓存文件key
     * @param value   缓存数据
     * @param timeout 设置超时删除
     * @param unit    时间单位{@link TimeUnit}
     */
    public void put(String key, Serializable value, long timeout, TimeUnit unit) {
        ByteArrayOutputStream out;
        ObjectOutputStream oos = null;
        try {
            out = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(out);
            oos.writeObject(value);
            byte[] data = out.toByteArray();
            put(key, data, timeout, unit);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加缓存文件
     *
     * @param key     缓存文件key
     * @param value   缓存数据
     * @param timeout 设置超时删除
     * @param unit    时间单位{@link TimeUnit}
     */
    public void put(String key, byte[] value, long timeout, TimeUnit unit) {
        File file = newFile(key, timeout, unit);
        writeDataToFile(file, value);
        //重新计算缓存文件数量
        int curCacheCount = mCacheCount.get();
        while (curCacheCount + 1 > mMaxCount) {
            long freedSize = removeOldest();
            mCacheSize.addAndGet(-freedSize);
            curCacheCount = mCacheCount.addAndGet(-1);
        }
        mCacheCount.addAndGet(1);

        //重新计算缓存文件总大小
        long valueSize = file.length();
        long curCacheSize = mCacheSize.get();
        while (curCacheSize + valueSize > mMaxSize) {
            long freedSize = removeOldest();
            curCacheSize = mCacheSize.addAndGet(-freedSize);
        }
        mCacheSize.addAndGet(valueSize);
        mLastModified.put(file, file.setLastModified(System.currentTimeMillis())
                ? file.lastModified() : System.currentTimeMillis());
        mCacheFiles.put(getFileKey(file), file);
    }

    /**
     * 获取序列化数据
     *
     * @param key 缓存文件key
     * @return Serializable 数据
     */
    public Serializable getSerializable(String key) {
        byte[] data = getByte(key);
        if (data != null) {
            ByteArrayInputStream in = null;
            ObjectInputStream ois = null;
            try {
                in = new ByteArrayInputStream(data);
                ois = new ObjectInputStream(in);
                return (Serializable) ois.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (ois != null) {
                        ois.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 获取 byte 数据
     *
     * @param key 缓存文件key
     * @return byte 数据
     */
    public byte[] getByte(String key) {
        RandomAccessFile accessFile = null;
        try {
            File file = get(key);
            if (file == null || !file.exists()) {
                return null;
            }
            accessFile = new RandomAccessFile(file, "r");
            byte[] byteArray = new byte[(int) accessFile.length()];
            accessFile.read(byteArray);
            return byteArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (accessFile != null) {
                try {
                    accessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 判断缓存的String数据是否到期
     *
     * @param key 文件保存的Key
     * @return true：到期了 false：还没有到期
     */
    public boolean isObsolete(String key) {
        File file = getFile(key);
        if (file == null || !file.exists()) {
            return true;
        }
        long timeout = getFileTimeout(file);
        boolean isObsolete = timeout > 0 && timeout < System.currentTimeMillis();
        if (isObsolete) {
            if (file.delete()) {
                mCacheFiles.remove(key);
                mLastModified.remove(file);
            } else {
                isObsolete = false;
            }
        }
        return isObsolete;
    }

    /**
     * 移除指定缓存数据
     *
     * @param key 缓存文件key
     * @return 是否移除成功
     */
    public boolean remove(String key) {
        File file = getFile(key);
        return file == null || !file.exists() || file.delete();
    }

    /**
     * 清除缓存文件夹
     */
    public boolean clear() {
        mLastModified.clear();
        mCacheFiles.clear();
        mCacheSize.set(0);
        return deleteFile(mCacheDir);
    }

    /**
     * 计算 cacheSize和cacheCount
     */
    private void calculateCacheSizeAndCacheCount() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                int size = 0;
                int count = 0;
                File[] cachedFiles = mCacheDir.listFiles();
                if (cachedFiles != null) {
                    for (File cachedFile : cachedFiles) {
                        size += cachedFile.length();
                        count += 1;
                        mLastModified.put(cachedFile, cachedFile.lastModified());
                        mCacheFiles.put(getFileKey(cachedFile), cachedFile);
                    }
                    mCacheSize.set(size);
                    mCacheCount.set(count);
                }
            }
        });
    }

    private File get(String key) {
        if (!isObsolete(key)) {
            File file = getFile(key);
            mLastModified.put(file, file.setLastModified(System.currentTimeMillis())
                    ? file.lastModified() : System.currentTimeMillis());
            return file;
        }
        return null;
    }

    private String getFileKey(File cachedFile) {
        return cachedFile.getName().split("_")[0];
    }

    private File getFile(String key) {
        return mCacheFiles.get(key.hashCode() + "");
    }

    private long getFileTimeout(File cachedFile) {
        String name = cachedFile.getName();
        String timeStr = "-1";
        if (name.contains("_")) {
            timeStr = cachedFile.getName().split("_")[1];
        }
        try {
            return Long.parseLong(timeStr);
        } catch (Exception e) {
            return -1;
        }
    }

    private void writeDataToFile(File file, byte[] value) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File newFile(String key, long timeout, TimeUnit unit) {
        File file = getFile(key);
        if (file != null) {
            file.deleteOnExit();
        }
        String fileName = key.hashCode() + "";
        if (timeout > -1) {
            long time = System.currentTimeMillis() + unit.toMillis(timeout);
            fileName = key.hashCode() + "_" + time;
        }
        return new File(mCacheDir, fileName);
    }

    private boolean deleteFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.isFile()) {
            return file.delete();
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!deleteFile(f)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * 移除最老的文件
     *
     * @return 移除文件的大小
     */
    private long removeOldest() {
        if (mLastModified.isEmpty()) {
            return 0;
        }

        File oldestFile = null;
        Long oldestModified = null;
        Set<Map.Entry<File, Long>> entries = mLastModified.entrySet();
        synchronized (mLastModified) {
            for (Map.Entry<File, Long> entry : entries) {
                if (oldestFile == null) {
                    oldestFile = entry.getKey();
                    oldestModified = entry.getValue();
                } else {
                    Long lastModified = entry.getValue();
                    if (lastModified < oldestModified) {
                        oldestFile = entry.getKey();
                        oldestModified = lastModified;
                    }
                }
            }
        }
        if (oldestFile != null) {
            if (oldestFile.delete()) {
                mLastModified.remove(oldestFile);
                mCacheFiles.remove(getFileKey(oldestFile));
            }
        }
        return 0;
    }
}
