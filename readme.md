# 作用
普通java工程，批量解决：
* geojson转shp；
* shp上传至postgis db
* geoserver发布postgis db上的图层

# 使用
* 参见
  * [GeoJsonAndShpFileUtilTest.java](src%2Ftest%2Fjava%2Fcom%2Funiv%2FGeoJsonAndShpFileUtilTest.java)；
  * [GeoUtilTest.java](src%2Ftest%2Fjava%2Fcom%2Funiv%2FGeoUtilTest.java)
* 如果要使用geoserver的发布功能，请修改[GeoServerPublishUtil.java](src%2Fmain%2Fjava%2Fcom%2Funiv%2FGeoServerPublishUtil.java)中的连接信息；
* 如果要使用shp上传至postgis，请修改[PGDatastore.java](src%2Fmain%2Fjava%2Fcom%2Funiv%2FPGDatastore.java)中的连接信息；

# 参考资料
* [geoserver 官方github](https://github.com/geosolutions-it/geoserver-manager)
* [geotools 官方github](https://github.com/geotools/geotools)
* [github参考代码](https://github.com/yieryi/gts4vect)