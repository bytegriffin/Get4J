package com.bytegriffin.get4j.conf;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * core-sites.xml配置文件处理类
 */
public class CoreSeedsXmlHandler extends AbstractConfig {

    private static Logger logger = LogManager.getLogger(CoreSeedsXmlHandler.class);

    /**
     * 加载core-seeds.xml配置文件内容到内存中
     */
    public List<Seed> load() {
        XmlHelper.validate(core_seeds_xml_file, core_seeds_xsd_file);
        logger.info("正在读取xml配置文件[" + core_seeds_xml_file + "]......");
        Document doc = XmlHelper.loadXML(core_seeds_xml_file);
        Element seedNode = null;
        try {
            seedNode = doc.getRootElement();
        } catch (NullPointerException e) {
            logger.error("读取的xml配置文件[{}]内容为空。",core_seeds_xml_file);
            System.exit(1);
        }
        List<Element> siteElements = seedNode.elements(seed_node);
        List<Seed> seeds = Lists.newArrayList();
        Set<String> hashset = Sets.newHashSet();//过滤相同seedName
        for (Element element : siteElements) {
            Seed seed = new Seed();
            List<Element> propertyElements = element.elements(property_node);
            for (Element property : propertyElements) {
                String name = property.element(name_node).getStringValue();
                String value = property.element(value_node).getStringValue();
                //配置文件中的seed.name必须要手工配置，API中可以不用写
                if (name.equalsIgnoreCase(seed_name)) {
                	if (!Strings.isNullOrEmpty(value)) {
                		seed.setSeedName(value);
                		hashset.add(name);
                	} else {
                		logger.error("xml配置文件[{}]中的[{}]属性的值不能为空。",core_seeds_xml_file,seed_name);
                		System.exit(1);
                	}
                }else if (name.equalsIgnoreCase(woker_thread_count)) {
                    if (!Strings.isNullOrEmpty(value)) {
                        seed.setThreadCount(Integer.valueOf(value));
                    }
                } else if (name.equalsIgnoreCase(fetch_url)) {
                    seed.setFetchUrl(value);
                } else if (name.equalsIgnoreCase(fetch_http_method)) {
                    seed.setFetchHttpMethod(value);  
                } else if (name.equalsIgnoreCase(fetch_probe_selector)) {
                    seed.setFetchProbeSelector(value);
                } else if (name.equalsIgnoreCase(fetch_probe_sleep)) {
                    seed.setFetchProbeSleep(value);
                } else if (name.equalsIgnoreCase(fetch_detailLink_selector)) {
                    seed.setFetchDetailLinkSelector(value);
                } else if (name.equalsIgnoreCase(fetch_total_pages)) {
                    seed.setFetchTotalPages(value);
                } else if (name.equalsIgnoreCase(fetch_login_username)) {
                    seed.setFetchUsername(value);
                } else if (name.equalsIgnoreCase(fetch_login_password)) {
                    seed.setFetchPassword(value);
                } else if (name.equalsIgnoreCase(fetch_timer_interval)) {
                    seed.setFetchInterval(value);
                } else if (name.equalsIgnoreCase(fetch_timer_start)) {
                    seed.setFetchStart(value);
                } else if (name.equalsIgnoreCase(fetch_http_proxy)) {
                    seed.setFetchHttpProxyFile(value);
                } else if (name.equalsIgnoreCase(fetch_http_user_agent)) {
                    seed.setFetchUserAgentFile(value);
                } else if (name.equalsIgnoreCase(fetch_sleep)) {
                    if (!Strings.isNullOrEmpty(value)) {
                        seed.setFetchSleep(Integer.valueOf(value));
                    }
                } else if (name.equalsIgnoreCase(fetch_sleep_range)) {
                    seed.setFetchSleepRange(value);
                } else if (name.equalsIgnoreCase(fetch_page_mode)) {
                    seed.setPageMode(value);
                } else if (name.equalsIgnoreCase(fetch_resource_selector)) {
                    seed.setFetchResourceSelectors(value);
                } else if (name.equalsIgnoreCase(fetch_javascript_support)) {
                    if (!Strings.isNullOrEmpty(value)) {
                        seed.setFetchJavascriptSupport(Boolean.valueOf(value));
                    }
                } else if (name.equalsIgnoreCase(download_disk)) {
                    seed.setDownloadDisk(value);
                } else if (name.equalsIgnoreCase(download_hdfs)) {
                    seed.setDownloadHdfs(value);
                } else if (name.equalsIgnoreCase(parse_class_impl)) {
                    seed.setParseClassImpl(value);
                } else if (name.equalsIgnoreCase(parse_element_selector)) {
                    seed.setParseElementSelector(value);
                } else if (name.equalsIgnoreCase(store_jdbc)) {
                    seed.setStoreJdbc(value);
                } else if (name.equalsIgnoreCase(store_mongodb)) {
                    seed.setStoreMongodb(value);
                } else if (name.equalsIgnoreCase(store_hbase)) {
                    seed.setStoreHBase(value);
                } else if (name.equalsIgnoreCase(store_lucene_index)) {
                    seed.setStoreLuceneIndex(value);
                }
            }
            seeds.add(seed);
        }
        if (hashset.size() < siteElements.size()) {
            logger.error("xml配置文件[{}]中不能设置相同的Seed Name，请重新检查。", core_seeds_xml_file);
            System.exit(1);
        }
        logger.info("xml配置文件[{}]读取完成。",core_seeds_xml_file);
        return seeds;
    }

}
