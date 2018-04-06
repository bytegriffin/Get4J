package com.bytegriffin.get4j.conf;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 解析dynamic-fields.xml配置文件
 */
public class DynamicFieldXmlHandler extends AbstractConfig {

	private static Logger logger = LogManager.getLogger(DynamicFieldXmlHandler.class);

	@Override
	public List<DynamicField> load() {
		XmlHelper.validate(dynamic_fields_xml_file, dynamic_fields_xsd_file);
		logger.info("正在读取xml配置文件[" + dynamic_fields_xml_file + "]......");
		Document doc = XmlHelper.loadXML(dynamic_fields_xml_file);
        Element rootNode = null;
        try {
        	rootNode = doc.getRootElement();
        } catch (NullPointerException e) {
            logger.error("读取的xml配置文件[" + dynamic_fields_xml_file + "]内容为空。");
            return null;
        }
        List<Element> elements = rootNode.elements(seed_node);
        List<DynamicField> mappings = Lists.newArrayList();
        Set<String> hashset = Sets.newHashSet();
        for (Element element : elements) {
            String seedName = element.element(name_node).getStringValue();
            hashset.add(seedName);
            Element fields = element.element(fields_node);
            List<Element> fieldlist = fields.elements(field_node);
            DynamicField mapping = new DynamicField();
            mapping.setSeedName(seedName);
            Map<String,String> map = Maps.newHashMap();
            for (Element field : fieldlist) {
                String name = field.element(name_node).getStringValue();
                String value = field.element(selector_node).getStringValue();
                if(Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(value)){
                	continue;
                }
                map.put(name, value);
                mapping.setFields(map);
            }
            mappings.add(mapping);
        }
        if (hashset.size() < elements.size()) {
            logger.error("xml配置文件[" + dynamic_fields_xml_file + "]中不能设置相同的Seed Name，请重新检查。");
            System.exit(1);
        }
        logger.info("xml配置文件[" + dynamic_fields_xml_file + "]读取完成。");
        return mappings;
	}

}
