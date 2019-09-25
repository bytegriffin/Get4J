package com.bytegriffin.get4j.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.util.DateUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * MongoDB数据库存储器<br>
 * 增量式更新数据
 */
public class MongodbStorage implements Process {

    private static final String database_name = "get4j";
    private static final String table_name = "page";
    private static final String index_field = "FETCH_URL";//注意：mongodb大小写敏感

    private static final Logger logger = LogManager.getLogger(MongodbStorage.class);

    @Override
    public void init(Seed seed) {
        MongoClientURI mc = new MongoClientURI(seed.getStoreMongodb());
        @SuppressWarnings("resource")
        MongoClient mongoClient = new MongoClient(mc);
        MongoDatabase database = mongoClient.getDatabase(database_name);
        MongoIterable<String> it = database.listCollectionNames();
        // 不存在Collecction，就创建一个，默认不存在
        boolean isExist = false;
        for (String table : it) {
            if (table_name.equalsIgnoreCase(table)) {
                isExist = true;
                break;
            }
        }
        // 动态创建表
        if (!isExist) {
            database.createCollection(table_name);
        }
        MongoCollection<Document> collection = database.getCollection(table_name);
        // 设置唯一索引
        if (!isExist) {
            IndexOptions indexOptions = new IndexOptions().unique(true);
            collection.createIndex(Indexes.ascending(index_field), indexOptions);
        }
        Globals.MONGO_COLLECTION_CACHE.put(seed.getSeedName(), collection);
        logger.info("种子[" + seed.getSeedName() + "]的组件MongodbStorage的初始化完成。");
    }

    @Override
    public void execute(Page page) {
        MongoCollection<Document> collection = Globals.MONGO_COLLECTION_CACHE.get(page.getSeedName());
        Document searchQuery = new Document();
        searchQuery.append(index_field, page.getUrl());

        Document findOne = collection.find(searchQuery).first();
        if (findOne == null || findOne.isEmpty()) {
            Document doc = buildDocument(page);
            doc.append("CREATE_TIME", DateUtil.getCurrentDate())
                    .append(index_field, page.getUrl()).append("SITE_HOST", page.getHost());
            collection.insertOne(doc);
        } else {
            Page dbpage = getDatabasePage(page, findOne);
            if (page.isRequireUpdateNoEncoding(dbpage)) {
                Document doc = buildDocument(page);
                Document updateObject = doc.append("UPDATE_TIME", DateUtil.getCurrentDate());
                collection.updateOne(Filters.eq(index_field, page.getUrl()), new Document("$set", updateObject));
            }
        }
        logger.info("线程[" + Thread.currentThread().getName() + "]保存种子[" + page.getSeedName() + "]url为[" + page.getUrl() + "]到MongoDB数据库中。");
    }

    /**
     * 构建原始文档，用于insert或update操作
     *
     * @param page Page
     * @return Document
     */
    private Document buildDocument(Page page) {
        Document doc = new Document().append("SEED_NAME", page.getSeedName())
                .append("TITLE", page.getTitle()).append("AVATAR", page.getAvatar())
                .append("COOKIES", page.getSetCookies()).append("RESOURCES_URL", page.getResources())
                .append("FETCH_TIME", page.getFetchTime());
        if (page.isHtmlContent()) {
            doc.append("FETCH_CONTENT", page.getHtmlContent());
        } else if (page.isJsonContent()) {
            doc.append("FETCH_CONTENT", page.getJsonContent());
        } else if (page.isXmlContent()) {
            doc.append("FETCH_CONTENT", page.getXmlContent());
        }
        int i = 1;
        for(Object obj : page.getFields().values()){
        	doc.append("FIELD"+i++, obj);
        }
        return doc;
    }

    /**
     * 获取数据库中的page对象，需要去和新抓取的page对象对比
     *
     * @param page Page
     * @param doc  Document
     * @return Page
     */
    private Page getDatabasePage(Page page, Document doc) {
        Page dbpage = new Page();
        dbpage.setAvatar(doc.getString("AVATAR"));
        dbpage.setSeedName(doc.getString("SEED_NAME"));
        dbpage.setSetCookies(doc.getString("COOKIES"));
        dbpage.setTitle(doc.getString("TITLE"));
        if (page.isHtmlContent()) {
            dbpage.setHtmlContent(doc.getString("FETCH_CONTENT"));
        } else if (page.isJsonContent()) {
            dbpage.setJsonContent(doc.getString("FETCH_CONTENT"));
        } else if (page.isXmlContent()) {
            dbpage.setXmlContent(doc.getString("FETCH_CONTENT"));
        }
        dbpage.setHost(doc.getString("SITE_HOST"));
        return dbpage;

    }

}
