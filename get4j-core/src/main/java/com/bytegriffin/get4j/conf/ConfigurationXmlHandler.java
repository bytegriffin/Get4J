package com.bytegriffin.get4j.conf;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * configuration.xml配置文件处理类
 */
public class ConfigurationXmlHandler extends AbstractConfig {

    private static Logger logger = LogManager.getLogger(ConfigurationXmlHandler.class);

    /**
     * 加载configuration.xml配置文件内容到内存中
     */
    @Override
    public Configuration load() {
        logger.info("正在读取xml配置文件[" + configuration_xml_file + "]......");
        Document doc = XmlHelper.loadXML(configuration_xml_file);
        if (doc == null) {
            return null;
        }
        Element configurationNode = doc.getRootElement();
        if (configurationNode == null) {
            return null;
        }
        List<Element> configurationElements = configurationNode.elements(configuration_node);
        Configuration conf = new Configuration();
        for (Element property : configurationElements) {
            if (property == null) {
                continue;
            }
            String name = property.element(name_node).getStringValue();
            String value = property.element(value_node).getStringValue();
            if (name.equalsIgnoreCase(download_filename_rule)) {
                conf.setDownloadFileNameRule(value);
            } else if (name.equalsIgnoreCase(email_recipient)) {
            	conf.setEmailRecipient(value);
            }
        }
        return conf;
    }

}
