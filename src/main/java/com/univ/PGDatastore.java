package com.univ;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;

/**
 * post gis db connection entity
 */
@Data
@Slf4j
public class PGDatastore {
    // pg 连接的表示
    private static DataStore dataStore = null;

    private String host;
    private String port;
    private String dbname;
    private String schema;
    private String username;
    private String password;

    public PGDatastore(String host, String port, String dbname, String schema, String username, String password) {
        this.host = host;
        this.port = port;
        this.dbname = dbname;
        this.schema = schema;
        this.username = username;
        this.password = password;
    }

    public static DataStore getDefeaultDatastore() {
        if (dataStore == null) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
            params.put(PostgisNGDataStoreFactory.SCHEMA.key, "public");

            // 本地
//            params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
//            params.put(PostgisNGDataStoreFactory.SCHEMA.key, "public");
            /*params.put(PostgisNGDataStoreFactory.HOST.key, "127.0.0.1");
            params.put(PostgisNGDataStoreFactory.PORT.key, new Integer(5432));
            params.put(PostgisNGDataStoreFactory.DATABASE.key, "xxx");
            params.put(PostgisNGDataStoreFactory.USER.key, "postgres");
            params.put(PostgisNGDataStoreFactory.PASSWD.key, "postgres");*/

            try {
                dataStore = DataStoreFinder.getDataStore(params);
            } catch (IOException e) {
                log.error("默认Postgis数据库连接失败", e);
            }
        }
        return dataStore;
    }
}
