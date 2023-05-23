package com.univ;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author univ date 2023/5/18
 */
@Slf4j
public class GeoUtilTest {

    @Test
    public void testSingleGeoJsonToShp() {
        String geoJsonPath = "/Users/univ/Desktop/tmp/jiangxi/331002_full.geojson";
        String shapefilePath = "/Users/univ/Desktop/tmp/jiangxi/331002_full.shp";
        GeoUtil geoUtil = new GeoUtil();
        geoUtil.geoJson2Shp(geoJsonPath, shapefilePath);
    }

    @Test
    public void testSingleShpToDb() {
        GeoUtil geoUtil = new GeoUtil();
        geoUtil.initPostGisDataStore();

        String shpFilePath = "/Users/univ/Desktop/tmp/jiangxi/江西省-第二级/360100.shp";
        // 处理成只需要传绝对路径即可，这里给抽离
        boolean b = geoUtil.shp2pgTable(shpFilePath, "360100");
        System.out.println("上传结果：" + b);
    }

    @Test
    public void testGeoJsonToShp() {
        String geoJsonDir = "/Users/univ/Desktop/tmp/jiangxi";
        GeoUtil geoUtil = new GeoUtil();
        geoUtil.geoJsonToShp(geoJsonDir);
    }

    @Test
    public void testShpToPostGisDb() {
        String shpFileDir = "/Users/univ/Desktop/tmp/jiangxi";
        GeoUtil geoUtil = new GeoUtil();
        geoUtil.initPostGisDataStore();
        geoUtil.shpToPostGisDb(shpFileDir);
    }

}
