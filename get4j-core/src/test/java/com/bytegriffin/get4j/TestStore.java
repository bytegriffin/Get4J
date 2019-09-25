package com.bytegriffin.get4j;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.bson.Document;

import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.store.DBStorage;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 测试存储
 */
public class TestStore {

    public static void testDB() throws UnsupportedEncodingException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root");
        config.setMaximumPoolSize(50);
        config.setConnectionTestQuery("SELECT 1");
        HikariDataSource dataSource = new HikariDataSource(config);
        DBStorage dbs = new DBStorage();
        Page page = new Page("seed1", "https://invest.ppdai.com/loan/info/338997833");
        page.setHtmlContent("test");
        Page dbpage = dbs.readOne(dataSource, page);
        System.out.println(URLDecoder.decode(dbpage.getHtmlContent(), "UTF-8"));
    }

    private static final String database_name = "get4j";
    private static final String table_name = "page";
    private static final String index_field = "fetch_url";

    public static void testMongoDB() {
        MongoClientURI mc = new MongoClientURI("mongodb://localhost:27017");
        @SuppressWarnings("resource")
        MongoClient mongoClient = new MongoClient(mc);
        MongoDatabase database = mongoClient.getDatabase(database_name);
        MongoCursor<String> it = database.listCollectionNames().iterator();
        // 不存在Collecction，就创建一个，默认不存在
        boolean isExist = false;
        while (it.hasNext()) {
            String table = it.next();
            if (table_name.equalsIgnoreCase(table)) {
                isExist = true;
                break;
            }
        }
        if (!isExist) {
            database.createCollection(table_name);
        }
        MongoCollection<Document> collection = database.getCollection(table_name);
        IndexOptions indexOptions = new IndexOptions().unique(true);
        if (!isExist) {
            collection.createIndex(Indexes.ascending(index_field), indexOptions);
        }
        Document searchQuery = new Document();
        searchQuery.append("fetch_url", "newnewnew");

        Document findOne = collection.find(searchQuery).first();
        if (findOne==null || findOne.isEmpty()) {
            collection.insertOne(new Document().append("fetch_url", "newnewnew"));
        } else {

        }
        
        //修改的条件
        BasicDBObject bson= new BasicDBObject("fetch_url", "aaa");
        //修改后的值
        BasicDBObject bson2 = new BasicDBObject("$set",  new BasicDBObject("fetch_url", "bbb"));
        collection.updateOne(bson, bson2);
        System.out.println(collection.countDocuments());
    }

    public static void main(String[] args) throws Exception {
        testMongoDB();
    }
}
