package com.univ;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.collection.CompositeCollection;

@Slf4j
public class Utility {

    //    读取判断
    public static boolean valiFileForRead(String filepath) {
        File file = new File(filepath);
        return file.exists();
    }

    //写入判断
    public static boolean valiFileForWrite(String filepath) {
        File file = new File(filepath);
        boolean result = false;
        if (file.exists()) {
            deleteDir(file);
        }
        try {
            result = file.createNewFile();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    //    删除指定文件或者文件夹
    public static boolean deleteDir(final File dir) {
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                final boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    //    判断空对象，判断空数组，判断空的字符串
    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if (o.getClass().equals(String.class) && String.valueOf(o).trim().length() < 1) {
            return true;
        }
        if (Collection.class.isAssignableFrom(o.getClass()) && ((Collection) (o)).size() < 1) {
            List list = new ArrayList();
            Collection collection = new CompositeCollection();
        }
        return false;
    }

}
