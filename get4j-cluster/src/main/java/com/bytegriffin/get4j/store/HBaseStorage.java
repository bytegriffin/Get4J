package com.bytegriffin.get4j.store;

import java.util.List;
import java.util.Map;

import org.apache.curator.shaded.com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hbase.thirdparty.com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.MD5Util;
import com.google.common.base.Strings;

/**
 * HBase数据库<br>
 * 增量式更新数据
 */
public class HBaseStorage implements Process {

	private static final Logger logger = LogManager.getLogger(HBaseStorage.class);
	private static final String namespace = "get4j";
	private static final String tableName = namespace + ":page";
	private static final String columnFamily = "cf";
	private static Map<String, Table> tables = Maps.newHashMap();

	@Override
	public void init(Seed seed) {
		// 1.初始化配置
		Configuration configuration = HBaseConfiguration.create();
		//configuration.set("hbase.zookeeper.property.clientPort", "2181");
		//configuration.set("hbase.master", "192.168.1.102:9000");
		configuration.set("hbase.zookeeper.quorum", seed.getStoreHBase());

		// 2.判断是否存在tableName，没有就新创建一个
		try {
			Connection connection = ConnectionFactory.createConnection(configuration);
			Admin admin = connection.getAdmin();
			TableName tName = TableName.valueOf(tableName);
			//判断命名空间是否已经存在，否则才要创建
			boolean isExist = false;
			NamespaceDescriptor[] nsArray = admin.listNamespaceDescriptors();
			for(NamespaceDescriptor ns : nsArray){
				if(namespace.equalsIgnoreCase(ns.getName())){
					isExist = true;
					break;
				}
			}
			if(!isExist){
				admin.createNamespace(NamespaceDescriptor.create(namespace).build());
			}
			if (!admin.tableExists(tName)) {	
				TableDescriptorBuilder builder = TableDescriptorBuilder.newBuilder(tName);
				List<ColumnFamilyDescriptor> cfList = Lists.newArrayList();
				cfList.add(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(columnFamily)).build());
				builder.setColumnFamilies(cfList).setReadOnly(false);
				TableDescriptor desc = builder.setDurability(Durability.ASYNC_WAL).build();
				admin.createTable(desc);
			}
			Table table = connection.getTable(tName);
			tables.put(seed.getSeedName(), table);
			admin.close();
		} catch (Exception e) {
			logger.error("种子[{}]的组件HBaseStorage的初始化出错。", seed.getSeedName(), e);
		}
		logger.info("种子[{}]的组件HBaseStorage的初始化完成。", seed.getSeedName());
	}

	@Override
	public void execute(Page page) {
		 Table table  = tables.get(page.getSeedName());
		 insertOrUpdate(table, page);
		 logger.info("线程[{}]保存种子[{}]url为[{}]到HBase数据库中。",Thread.currentThread().getName(),  page.getSeedName() , page.getUrl() );
	}

	@SuppressWarnings("unlikely-arg-type")
	public void insertOrUpdate(Table table, Page page) {
		try {
			Put put = new Put(Bytes.toBytes(MD5Util.convert(page.getUrl())));
			put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("SEED_NAME"), Bytes.toBytes(page.getSeedName()));
			put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("FETCH_URL"), Bytes.toBytes(page.getUrl()));
			put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("SITE_HOST"), Bytes.toBytes(page.getHost()));
			if(!Strings.isNullOrEmpty(page.getTitle())){
				put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("TITLE"), Bytes.toBytes(page.getTitle()));
			}
			if(!Strings.isNullOrEmpty(page.getAvatar())){
				put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("AVATAR"), Bytes.toBytes(page.getAvatar()));
			}
			put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("FETCH_CONTENT"),	Bytes.toBytes(page.getContent()));
			if(!Strings.isNullOrEmpty(page.getSetCookies())){
				put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("COOKIES"), Bytes.toBytes(page.getSetCookies()));
			}
			if(!page.getResources().isEmpty()){
				put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("RESOURCES_URL"),	Bytes.toBytes(page.getResources().toString()));
			}
			put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("FETCH_TIME"),	Bytes.toBytes(page.getFetchTime()));
			put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("CREATE_TIME"),  Bytes.toBytes(DateUtil.getCurrentDate()));
			for (int i=1; i<=page.getFields().size(); i++) {
				put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes("FIELD"+ i), Bytes.toBytes(page.getFields().get(i).toString()));
			}
			table.put(put);
		} catch (Exception e) {
			logger.error("种子[{}]在进行插入数据时出错。", page.getSeedName(), e);
		}
	}

}
