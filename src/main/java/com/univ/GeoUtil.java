package com.univ;

import static org.geotools.data.Transaction.AUTO_COMMIT;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * geojson转shp、shp上传至post gis
 */
@Slf4j
public class GeoUtil {

    private static DataStore postgisDatasore;

    public void initPostGisDataStore() {
        postgisDatasore = PGDatastore.getDefeaultDatastore();
    }

    /**
     * 通用，要素集写入postgis
     *
     * @param featureCollection
     * @param pgtableName       postgis创建的数据表
     * @return
     */
    public static boolean write2pg(FeatureCollection featureCollection, String pgtableName) {
        boolean result = false;
        try {
            if (Utility.isEmpty(featureCollection) || Utility.isEmpty(pgtableName)) {
                log.error("参数无效");
                return result;
            }
            SimpleFeatureType simpleFeatureType = (SimpleFeatureType) featureCollection.getSchema();
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.init(simpleFeatureType);
            typeBuilder.setName(pgtableName);

            SimpleFeatureType newtype = typeBuilder.buildFeatureType();
            // 注意：getSchema方法要求schema必须存在，这里最好有类似于exist方法，便于重复上传，很可惜没有
            // 看下能不能直接转成更具体的子类型(ContentFeatureSource)，然后查看其中是否有exist方法
//            SimpleFeatureType existedScheme = postgisDatasore.getSchema(newtype.getTypeName());
//            if (null != existedScheme) {
//                System.out.println(newtype.getTypeName());
//                System.out.println("======");
//                // 在上传了错误的schema后，此时需要先删除
//                postgisDatasore.removeSchema(existedScheme.getTypeName());
//            }
            // 这里要求此schema没有存在
            postgisDatasore.createSchema(newtype);
            FeatureIterator iterator = featureCollection.features();
            FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = postgisDatasore.getFeatureWriterAppend(pgtableName, AUTO_COMMIT);

            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                SimpleFeature simpleFeature = featureWriter.next();
                Collection<Property> properties = feature.getProperties();
                Iterator<Property> propertyIterator = properties.iterator();
                while (propertyIterator.hasNext()) {
                    Property property = propertyIterator.next();
                    simpleFeature.setAttribute(property.getName().toString(), property.getValue());
                }
                featureWriter.write();
            }
            iterator.close();
            featureWriter.close();

        } catch (Exception e) {
            log.error("失败", e);
        }
        return false;
    }

    /**
     * featureCollection写入到shp的datastore
     *
     * @param featureCollection
     * @param shpDataStore
     * @param geomFieldName     featureCollectio中的矢量字段，postgis可以修改使用不同的表名，默认为geom
     * @return
     */
    public static boolean write2shp(FeatureCollection featureCollection, ShapefileDataStore shpDataStore, String geomFieldName)
        throws IOException {
        boolean result = true;
        if (Utility.isEmpty(geomFieldName)) {
            geomFieldName = featureCollection.getSchema().getGeometryDescriptor().getType().getName().toString();
        }
        FeatureIterator<SimpleFeature> iterator = featureCollection.features();
        //shp文件存储写入
        FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = shpDataStore.getFeatureWriter(shpDataStore.getTypeNames()[0], AUTO_COMMIT);
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            SimpleFeature simpleFeature = featureWriter.next();
            Collection<Property> properties = feature.getProperties();

            for (Property property : properties) {
                if (property.getName().toString().equalsIgnoreCase(geomFieldName)) {
                    simpleFeature.setAttribute("the_geom", property.getValue());
                } else {
                    // mytodo:不如在这里过滤掉不符合要求的字段，这样能免于额外处理
                    simpleFeature.setAttribute(property.getName().toString(),
                        property.getValue());
                }
            }
            featureWriter.write();
        }
        iterator.close();
        featureWriter.close();
        shpDataStore.dispose();

        return result;
    }


    /**
     * 批量geoJson转shp文件
     *
     * @param geoJsonDir 存放geoJson文件的目录，会递归处理
     */
    public void geoJsonToShp(String geoJsonDir) {
        List<String> geoJsonFiles = new ArrayList<>(100);
        GeoJsonAndShpFileUtil.collectFileAbsPath(geoJsonDir, FileTypeEnum.GEO_JSON, geoJsonFiles);
        System.out.println("此目录下的所有geoJson文件有：");
        System.out.println(geoJsonFiles);

        geoJsonFiles.forEach(geoJsonFile -> {
            int i = geoJsonFile.lastIndexOf("/");
            String dir = geoJsonFile.substring(0, i);
            String[] split = geoJsonFile.split("/");
            String fileNameWithPostfix = split[split.length - 1];
            String fileName = fileNameWithPostfix.split("\\.")[0];
            String shpFile = dir + "/" + fileName + ".shp";
            geoJson2Shp(geoJsonFile, shpFile);
        });
    }

    /**
     * 批量shp上传到db
     *
     * @param shpFileDir 存放shp文件的目录，会递归处理
     */
    public void shpToPostGisDb(String shpFileDir) {
        List<String> shpFiles = new ArrayList<>(100);
        GeoJsonAndShpFileUtil.collectFileAbsPath(shpFileDir, FileTypeEnum.SHP, shpFiles);
        shpFiles.forEach(shp -> {
            String[] split = shp.split("/");
            String fileNameWithPostfix = split[split.length - 1];
            String fileName = fileNameWithPostfix.split("\\.")[0];
            shp2pgTable(shp, fileName);
        });
    }

    /**
     * geoJson转成shpfile文件, 使用utf-8字符集
     *
     * @param geoJsonAbsPath
     * @param shpFileAbsPath
     * @return
     */
    public boolean geoJson2Shp(String geoJsonAbsPath, String shpFileAbsPath) {
        boolean result = false;
        try {
            Utility.valiFileForRead(geoJsonAbsPath);
            // 指定经纬度精度
            FeatureJSON featureJSON = new FeatureJSON(new GeometryJSON(15));
            featureJSON.setEncodeNullValues(true);
            FeatureCollection featureCollection = featureJSON.readFeatureCollection(
                new InputStreamReader(Files.newInputStream(Paths.get(geoJsonAbsPath)), StandardCharsets.UTF_8)
            );

            File file = new File(shpFileAbsPath);
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
            ShapefileDataStore shpDataStore = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);

            //postgis获取的Featuretype获取坐标系代码
            SimpleFeatureType pgfeaturetype = (SimpleFeatureType) featureCollection.getSchema();

            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.init(pgfeaturetype);
            typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
            pgfeaturetype = typeBuilder.buildFeatureType();
            //设置成utf-8编码
            shpDataStore.setCharset(StandardCharsets.UTF_8);
            // 此时就会生成dbf、fix、prj、shx、shp等文件，只是内容还不完整
            shpDataStore.createSchema(pgfeaturetype);
            result = write2shp(featureCollection, shpDataStore, "");
            log.info("geojson转成shp，[result:{}, geoJsonAbsPath:{}, shpFileAbsPath:{}]", result, geoJsonAbsPath, shpFileAbsPath);
        } catch (Exception e) {
            log.error("geojson转成shp失败，[geoJsonAbsPath:{}, shpFileAbsPath:{}, ex:]", geoJsonAbsPath, shpFileAbsPath, e);
        }
        return result;
    }

    /**
     * geoJson文件写入到postgis里
     *
     * @param geoJsonPath
     * @param pgtableName
     * @return
     */
    public boolean geoJson2pgtable(String geoJsonPath, String pgtableName) {
        boolean result = false;
        try {
            if (Utility.isEmpty(geoJsonPath) || Utility.isEmpty(pgtableName)) {
                return result;
            }
            FeatureJSON featureJSON = new FeatureJSON();
            FeatureCollection featureCollection = featureJSON.readFeatureCollection(Files.newInputStream(Paths.get(geoJsonPath)));
            result = write2pg(featureCollection, pgtableName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * 重载方法，默认UTF-8编码SHP文件
     *
     * @param shpPath
     * @param geoJsonPath
     * @return
     */
    public boolean shp2geoJson(String shpPath, String geoJsonPath) {
        return shp2geoJson(shpPath, geoJsonPath, StandardCharsets.UTF_8);
    }

    /**
     * shp转成geoJson，保留15位小数
     *
     * @param shpPath     shp的路径
     * @param geoJsonPath geoJson的路径
     * @return
     */
    public boolean shp2geoJson(String shpPath, String geoJsonPath, Charset shpCharset) {
        boolean result = false;
        try {
            if (!Utility.valiFileForRead(shpPath) || Utility.isEmpty(geoJsonPath)) {
                return result;
            }
            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(new File(shpPath).toURI().toURL());
            shapefileDataStore.setCharset(shpCharset);
            ContentFeatureSource featureSource = shapefileDataStore.getFeatureSource();
            ContentFeatureCollection contentFeatureCollection = featureSource.getFeatures();
            FeatureJSON featureJSON = new FeatureJSON(new GeometryJSON(15));
            Utility.valiFileForWrite(geoJsonPath);
            featureJSON.writeFeatureCollection(contentFeatureCollection, new File(geoJsonPath));
            shapefileDataStore.dispose();
            result = true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * shpfile文件导入到postgis中
     *
     * @param shpFileAbsPath
     * @param pgTableName
     * @return
     */
    public boolean shp2pgTable(String shpFileAbsPath, String pgTableName) {
        boolean result = false;
        try {
            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(new File(shpFileAbsPath).toURI().toURL());
            shapefileDataStore.setCharset(StandardCharsets.UTF_8);
            FeatureCollection featureCollection = shapefileDataStore.getFeatureSource().getFeatures();
            result = write2pg(featureCollection, pgTableName);
            log.error("shp文件上传到post result:{}, shpPath:{}", result, shpFileAbsPath);
        } catch (Exception e) {
            log.error("shp文件上传到post gis失败, shpPath:{}, exception:", shpFileAbsPath, e);
        }
        return result;
    }

    /**
     * postgis数据表导出到成shpfile
     *
     * @param pgtableName
     * @param shpPath
     * @param geomField   postgis里的字段
     * @return
     */
    public boolean pgtable2shp(String pgtableName, String shpPath, String geomField) {
        boolean result = false;
        try {

            FeatureSource featureSource = postgisDatasore.getFeatureSource(pgtableName);

            // 初始化 ShapefileDataStore
            File file = new File(shpPath);
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
            ShapefileDataStore shpDataStore = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);

            //postgis获取的Featuretype获取坐标系代码
            SimpleFeatureType pgfeaturetype = ((SimpleFeatureSource) featureSource).getSchema();
            String srid = pgfeaturetype.getGeometryDescriptor().getUserData().get("nativeSRID").toString();
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.init(pgfeaturetype);
            if (!srid.equals("0")) {
                CoordinateReferenceSystem crs = CRS.decode("EPSG:" + srid, true);
                typeBuilder.setCRS(crs);
            }
            pgfeaturetype = typeBuilder.buildFeatureType();
            //设置成utf-8编码
            shpDataStore.setCharset(Charset.forName("utf-8"));
            shpDataStore.createSchema(pgfeaturetype);
            write2shp(featureSource.getFeatures(), shpDataStore, geomField);
            result = true;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * postgis指定的数据表转成geoJson文件保留15位小数
     *
     * @param pgtableName 表名
     * @param geoJsonpath geoJson存放位置
     * @return
     */
    public boolean pgtable2geoJson(String pgtableName, String geoJsonpath) {
        boolean result = false;
        try {
            FeatureSource featureSource = postgisDatasore.getFeatureSource(pgtableName);
            FeatureCollection featureCollection = featureSource.getFeatures();

            FeatureJSON featureJSON = new FeatureJSON(new GeometryJSON(15));
            featureJSON.setEncodeNullValues(true);

            String s = featureJSON.toString(featureCollection);
            FileUtils.writeStringToFile(new File(geoJsonpath), s, Charsets.toCharset("utf-8"), false);
            result = true;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    public boolean deletePgtable(String pgtableName) {
        boolean result = false;
        try {
            postgisDatasore.removeSchema(pgtableName);
            result = true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    /*
    //    测试调试专用，成功清除所有的sw开头的表（用来存储矢量数据的表）
        public boolean clearSWTable() throws Exception {
            postgisDatasore.removeSchema();
            //relkind char r = 普通表，i = 索 引， S = 序列，v = 视 图， m = 物化视图， c = 组合类型，t = TOAST表， f = 外部 表
            String strtables = " select string_agg(relname ,\',\') from pg_class where relname like \'%sw_%\'  and relkind=\'r\' ";
            List list =  postgisDatasore.getSessionFactory().getCurrentSession().createQuery(strtables).list();
            list.get(0).toString();
            Integer integer = 0;
            if (list.size() > 0) {
                integer = temp.getSessionFactory().getCurrentSession().createQuery("drop table " + strtables).executeUpdate();
            }
    //        与表有关联的其他序列自动删除
            String sequence = " select string_agg(relname ,\',\') from pg_class where relname like \'%sw_%\' and relkind=\'S\' and relname!=\'txsw_seq\'";
            resultSet = st.executeQuery(sequence);
            while (resultSet.next()) {
                sequence = resultSet.getString(1);
            }
            System.out.println("所有非txsw_seq的序列：" + sequence);
            i = st.executeUpdate("drop SEQUENCE " + strtables);
            return integer == 0 ? true : false;
        }
    */


}
