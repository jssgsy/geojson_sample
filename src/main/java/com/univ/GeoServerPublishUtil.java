package com.univ;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.decoder.RESTDataStore;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

/**
 * 将postgis上的数据发布到geoserver上
 */
public class GeoServerPublishUtil {

	private static String url = "geoserver 网址" ;
	private static String username = "geoserver登录用户名" ;
	private static String passwd = "geoserver登录密码" ;
	private static String postgisHost = "" ;
	private static int postgisPort = 25432 ;
	private static String postgisUser = "postgres" ;
	private static String postgisPassword = "postgres" ;
	private static String postgisDatabase = "gis" ;
	private static String ws = "geoserver 工作区" ;
	private static String store_name = "geoserver 数据存储";


	public static void main(String[] args) throws MalformedURLException {
		//判断工作区（workspace）是否存在，不存在则创建
		URL u = new URL(url);
		GeoServerRESTManager manager = new GeoServerRESTManager(u, username, passwd);
		GeoServerRESTPublisher publisher = manager.getPublisher() ;
		List<String> workspaces = manager.getReader().getWorkspaceNames();
		if(!workspaces.contains(ws)){
			boolean createws = publisher.createWorkspace(ws);
			System.out.println("create ws : " + createws);
		}else {
			System.out.println("workspace已经存在了,ws :" + ws);
		}

		//判断数据存储（datastore）是否已经存在，不存在则创建
		RESTDataStore restStore = manager.getReader().getDatastore(ws, store_name);
		if(restStore == null){
			GSPostGISDatastoreEncoder store = new GSPostGISDatastoreEncoder(store_name);
			store.setHost(postgisHost);//设置url
			store.setPort(postgisPort);//设置端口
			store.setUser(postgisUser);// 数据库的用户名
			store.setPassword(postgisPassword);// 数据库的密码
			store.setDatabase(postgisDatabase);// 那个数据库;
			store.setSchema("public"); //当前先默认使用public这个schema
			store.setConnectionTimeout(20);// 超时设置
			//store.setName(schema);
			store.setMaxConnections(20); // 最大连接数
			store.setMinConnections(1);     // 最小连接数
			store.setExposePrimaryKeys(true);
			boolean createStore = manager.getStoreManager().create(ws, store);
			System.out.println("create store : " + createStore);
		} else {
			System.out.println("数据存储已经存在了,store:" + store_name);
		}

		//读取要发布的图层列表
		String shpFileDir = "";
//		List<String> nameList = GeoJsonFileUtil.listAllShpTableNames(shpFileDir);

		List<String> nameList = Arrays.asList("360000_full");
		if(!CollectionUtils.isEmpty(nameList)) {
			for (String table_name: nameList) {

				//判断图层是否已经存在，不存在则创建并发布
				RESTLayer layer = manager.getReader().getLayer(ws, table_name);
				if (layer == null) {
					GSFeatureTypeEncoder pds = new GSFeatureTypeEncoder();
					pds.setTitle(table_name);
					pds.setName(table_name);
					pds.setSRS("EPSG:4326");
					GSLayerEncoder layerEncoder = new GSLayerEncoder();
					boolean publish = manager.getPublisher().publishDBLayer(ws, store_name, pds, layerEncoder);
					System.out.println("publish : " + publish);
				} else {
					System.out.println("表已经发布过了,table:" + table_name);
				}
			}
		}
	}

}
