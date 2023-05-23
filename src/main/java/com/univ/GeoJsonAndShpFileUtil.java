package com.univ;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;


/**
 * 用来预处理前端给的geojson文件，比如删除某个字段
 * @author univ
 * date 2023/5/18
 */
@Slf4j
public class GeoJsonAndShpFileUtil {

    /**
     * 删除geoJson文件features---》properties下的某些属性并重新写回原文件中
     *
     * @param geoJsonFileAbsPath geoJson文件绝对路径
     * @param propertiesToDelete 要删除的属性名
     * @throws IOException  If an I/O error occurs
     */
    public static void singleGeoJsonRemoveProperties(String geoJsonFileAbsPath, List<String> propertiesToDelete) throws IOException {
        String str = fileToString(geoJsonFileAbsPath);
        log.info("原始geoJson文件内容为：{}", str);
        JSONObject jsonObject = JSONObject.parseObject(str);
        JSONArray jsonArray = jsonObject.getJSONArray("features");
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject properties = jsonArray.getJSONObject(i).getJSONObject("properties");
            propertiesToDelete.forEach(properties::remove);
        }
        log.info("处理之后的geoJson文件内容为：{}", jsonObject);
        // 写入到文件中
        try (FileWriter fileWriter = new FileWriter(geoJsonFileAbsPath)) {
            fileWriter.write(jsonObject.toString());
            // mytodo:记得调用，否则在大文件时可能会截断，要研究
            fileWriter.flush();
        }
    }

    /**
     * {@link #singleGeoJsonRemoveProperties(String, List)}的复数版本，用来批量处理
     * @param geoJsonDir geoJson目录，会递归处理
     * @param propertiesToDelete 要删除的属性
     * @throws IOException  If an I/O error occurs
     */
    public static void geoJsonRemoveProperties(String geoJsonDir, List<String> propertiesToDelete)
        throws IOException {
        List<String> absPathList = new ArrayList<>();
        collectFileAbsPath(geoJsonDir, FileTypeEnum.GEO_JSON, absPathList);
        for (String path : absPathList) {
            singleGeoJsonRemoveProperties(path, propertiesToDelete);
        }
    }

    /**
     * geoJson文件内容转成字符串
     * @param geoJsonFileAbsPath geoJson文件绝对路径
     * @return geoJson文件内容的字符串表示
     * @throws IOException  If an I/O error occurs
     */
    private static String fileToString(String geoJsonFileAbsPath) throws IOException {
        FileReader reader = new FileReader(geoJsonFileAbsPath);
        StringBuilder stringBuilder = new StringBuilder();
        char[] buffer = new char[100];
        int size;
        while ((size = reader.read(buffer)) != -1) {
            stringBuilder.append(buffer, 0, size);
        }
        reader.close();
        return stringBuilder.toString();
    }

    /**
     * 递归收集某个目录下的所有geojson或shp文件的绝对路径
     * @param dir 目录
     * @param fileTypeEnum {@link FileTypeEnum}
     * @param absPathList 用来承接结果的list
     */
    public static void collectFileAbsPath(String dir, FileTypeEnum fileTypeEnum, List<String> absPathList){
        File file = new File(dir);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    collectFileAbsPath(f.getAbsolutePath(), fileTypeEnum, absPathList);
                } else {
                    if(f.getName().contains(fileTypeEnum.getFileType())) {
                        absPathList.add(f.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * 递归获取目录下的所有shp文件的名称，发布到geoserver时用
     * @param shpFileDir 目录
     * @return [360000_full, 360000]
     */
    public static List<String> listAllShpTableNames(String shpFileDir) {
        List<String> tablesNames = new ArrayList<>();
        List<String> shpFileList = new ArrayList<>(100);
        collectFileAbsPath(shpFileDir, FileTypeEnum.SHP, shpFileList);
        shpFileList.forEach(shp -> {
            String[] split = shp.split("/");
            String fileNameWithPostfix = split[split.length - 1];
            String fileName = fileNameWithPostfix.split("\\.")[0];
            tablesNames.add(fileName);
        });
        return tablesNames;
    }

}
