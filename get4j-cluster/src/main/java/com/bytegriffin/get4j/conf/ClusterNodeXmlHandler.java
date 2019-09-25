package com.bytegriffin.get4j.conf;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.google.common.base.Strings;

/**
 * cluster-node.xml配置文件处理类
 */
public class ClusterNodeXmlHandler extends AbstractConfig {

    private static Logger logger = LogManager.getLogger(ClusterNodeXmlHandler.class);

    // xml配置文件
    private final static String cluster_node_xml_file = conf_path + "cluster-node.xml";
    // xml格式检验文件
    private final static String cluster_node_xsd_file = conf_path + "cluster-node.xsd";

    private static final String redis_mode = "fetch.redis.mode";
    private static final String redis_address = "fetch.redis.address";
    private static final String redis_auth = "fetch.redis.auth";

    private static final String zookeeper_address = "ha.zookeeper.quorum";

    /**
     * 加载configuration.xml配置文件内容到内存中
     */
    @Override
    public ClusterNode load() {
    	XmlHelper.validate(cluster_node_xml_file, cluster_node_xsd_file);
        logger.info("正在读取xml配置文件[" + cluster_node_xml_file + "]......");
        Document doc = XmlHelper.loadXML(cluster_node_xml_file);
        if (doc == null) {
            return null;
        }
        Element root = doc.getRootElement();
        if (root == null) {
            return null;
        }
        List<Element> propElements = root.elements(property_node);
        ClusterNode conf = new ClusterNode();
        for (Element property : propElements) {
            if (property == null) {
                continue;
            }
            String name = property.element(name_node).getStringValue();
            String value = property.element(value_node).getStringValue();
            if (name.equalsIgnoreCase(node_name)) {
            	if (!Strings.isNullOrEmpty(value)) {
            		conf.setNodeName(value);
            	} else {
            		logger.error("xml配置文件[{}]中的[{}]属性的值不能为空。",cluster_node_xml_file,node_name);
            		System.exit(1);
            	}
            } else if (name.equalsIgnoreCase(redis_mode)) {
            	conf.setRedisMode(value);
            } else if (name.equalsIgnoreCase(redis_address)) {
            	conf.setRedisAddress(value);
            } else if (name.equalsIgnoreCase(redis_auth)) {
            	conf.setRedisAuth(value);
            } else if (name.equalsIgnoreCase(zookeeper_address)) {
            	conf.setZookeeperAddress(value);
            }
        }
        return conf;
    }

}
