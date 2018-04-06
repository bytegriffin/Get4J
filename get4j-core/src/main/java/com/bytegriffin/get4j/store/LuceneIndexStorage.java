package com.bytegriffin.get4j.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.download.DiskDownloader;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.FileUtil;

/**
 * Lucene索引存储器
 */
public class LuceneIndexStorage implements Process {

    private static final Logger logger = LogManager.getLogger(DiskDownloader.class);
    private static final Analyzer analyzer = new SmartChineseAnalyzer(); // 中文分词
    private static final String unique_term = "url";// 唯一值

    @Override
    public void init(Seed seed) {
        String indexpath = seed.getStoreLuceneIndex();
        String folderName;
        if (DefaultConfig.default_value.equalsIgnoreCase(indexpath)) {
            indexpath = DefaultConfig.getLuceneIndexPath(seed.getSeedName());
        } else if (indexpath.contains(File.separator) || indexpath.contains(":")) {// eg. C: C盘
            if (!indexpath.contains(seed.getSeedName())) {
                indexpath = indexpath + File.separator + seed.getSeedName();
            }
        } else {
            logger.error("Lucene索引文件夹[" + indexpath + "]配置出错，请重新检查。");
            System.exit(1);
        }
        folderName = FileUtil.makeDiskDir(indexpath);// 获取用户配置的索引文件夹
        Globals.LUCENE_INDEX_DIR_CACHE.put(seed.getSeedName(), folderName);
        initParams(seed.getSeedName());
        logger.info("种子[" + seed.getSeedName() + "]的组件LuceneStorage的初始化完成。");
    }

    /**
     * 初始化Lucene参数<br>
     * 1.优化IndexWriterConifg参数<br>
     * 2.保证每个Seed实例对应一个IndexWriter，即每个Seed对应一个Directory，从而达到节省资源开销与线程安全
     *
     * @param seedName String
     */
    private void initParams(String seedName) {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(OpenMode.CREATE_OR_APPEND);
        LogDocMergePolicy policy = new LogDocMergePolicy();
        // 设置合并因子，控制合并频率和大小，即：每N个document合并成一个小段，每N个小段合并成一个大段
        // 值较大，使用的内存越大，索引速度越快，搜索速度越慢；值较小，使用的内存速度越小，索引速度越慢，搜索速度越快
        // 当启用list_detail模式，一个列表页中最大会有20条详情页链接，那么每次就按照一页来设置合并因子
        policy.setMergeFactor(20);
        config.setMergePolicy(policy);
        // 控制索引文档的内存上限，如果达到此数，内存索引就会写入磁盘，此值越大索引速度越快
        // 假设一个页面最大为10M，那么按照10个页面来计算，可以最大设置为100M，它的默认值是16M
        config.setRAMBufferSizeMB(100);
        // 内存中保存最大的document数，一旦超过此数，内存索引会将在磁盘上生成一个新的segment文件
        // 此参数一般是默认关闭的，如果开启那么就与RAMBufferSizeMB共同起作用，一般不建议随便设置
        // config.setMaxBufferedDocs(20);
        // 关闭复合文件格式(即：合并多个Segment文件到一个.cfs中)，加快创建索引速度，但同时会增加搜索和索引使用的文件句柄的数量
        config.setUseCompoundFile(false);
        try {
            FSDirectory dir = FSDirectory.open(Paths.get(Globals.LUCENE_INDEX_DIR_CACHE.get(seedName)));
            IndexWriter indexWriter = new IndexWriter(dir, config);
            Globals.INDEX_WRITER_CACHE.put(seedName, indexWriter);
        } catch (Exception e) {
            logger.error("系统初始化种子[" + seedName + "]的Lucene索引时出错。");
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(seedName, e);
        }
    }

    @Override
    public void execute(Page page) {
        Document doc = new Document();
        // StringField 只索引但不分词
        doc.add(new StringField("seed_name", page.getSeedName(), Field.Store.YES));
        doc.add(new StringField(unique_term, page.getUrl(), Field.Store.YES));
        doc.add(new StringField("site_host", page.getHost(), Field.Store.YES));
        doc.add(new StringField("fetch_time", page.getFetchTime(), Field.Store.YES));
        doc.add(new StringField("create_time", DateUtil.getCurrentDate(), Field.Store.YES));
        //以下三个字段暂时没必要索引
//		doc.add(new StringField("avatar", page.getAvatar(), Field.Store.NO));
//		doc.add(new StringField("cookies", page.getCookies(), Field.Store.NO));
//		doc.add(new StringField("resources_url", page.getResources().toString(), Field.Store.NO));

        // TextField 索引并分词
        doc.add(new TextField("title", page.getTitle(), Field.Store.YES));
        doc.add(new TextField("content", page.getContent(), Field.Store.NO)); // 内容不保存
        int i = 1;
        for(Object obj : page.getFields().values()){
        	if(obj instanceof String){
        		doc.add(new TextField("FIELD"+i++, obj.toString(), Field.Store.YES));
        	} else if(obj instanceof Integer){
        		doc.add(new StoredField("FIELD"+i++, Integer.valueOf(obj.toString()))); 
        	}
        }

        try {
            IndexWriter indexWriter = Globals.INDEX_WRITER_CACHE.get(page.getSeedName());
            // 先删除，后增加，删除只是放到回收站
            indexWriter.updateDocument(new Term(unique_term, page.getUrl()), doc);
            // 清空回收站
            indexWriter.forceMergeDeletes();
            indexWriter.commit();
        } catch (IOException e) {
        	EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
            logger.error("线程[" + Thread.currentThread().getName() + "]保存种子[" + page.getSeedName() + "]url为[" + page.getUrl()
                    + "]到Lucene索引中是出错。", e);
        }

        logger.info("线程[" + Thread.currentThread().getName() + "]保存种子[" + page.getSeedName() + "]url为[" + page.getUrl()
                + "]到Lucene索引中。");

    }

}
