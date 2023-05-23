package com.univ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * @author univ date 2023/5/19
 */
public class GeoJsonAndShpFileUtilTest {

    @Test
    public void testSingleGeoJsonRemoveProperties() throws IOException {
        String geoJsonFileAbsPath = "/Users/univ/Desktop/tmp/jiangxi/360000_full.geojson";
        List<String> propertiesToDelete = Arrays.asList("centroid", "center");
        GeoJsonAndShpFileUtil.singleGeoJsonRemoveProperties(geoJsonFileAbsPath, propertiesToDelete);
    }

    @Test
    public void testGeoJsonRemoveProperties() throws IOException {
        String geoJsonFileAbsPath = "/Users/univ/Desktop/tmp/jiangxi";
        List<String> propertiesToDelete = Arrays.asList("subFeatureIndex");
        GeoJsonAndShpFileUtil.geoJsonRemoveProperties(geoJsonFileAbsPath, propertiesToDelete);
    }

    @Test
    public void testCollectFileAbsPath() {
        List<String> absPathList = new ArrayList<>();
        String dir = "/Users/univ/Desktop/tmp/归档 4";
        GeoJsonAndShpFileUtil.collectFileAbsPath(dir, FileTypeEnum.GEO_JSON, absPathList);
        System.out.println("总收集到文件" + absPathList.size() + "个, 如下：");
        absPathList.forEach(System.out::println);
    }

    @Test
    public void testListAllTableNames() {
        String dir = "/Users/univ/Desktop/tmp/jiangxi";
        List<String> shpNames = GeoJsonAndShpFileUtil.listAllShpTableNames(dir);
        System.out.println(shpNames);
    }

}
