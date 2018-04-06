package com.bytegriffin.get4j.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * resource-sync.yaml配置文件处理类
 */
public class ResourceSyncYamlHandler extends AbstractConfig {

    private static Logger logger = LogManager.getLogger(ResourceSyncYamlHandler.class);

    @Override
    ResourceSync load() {
        Yaml ya = new Yaml();
        try {
            return ya.loadAs(new FileInputStream(new File(resource_sync_yaml_file)), ResourceSync.class);
        } catch (FileNotFoundException e) {
            logger.error("系统中不存在yaml配置文件[" + resource_sync_yaml_file + "]，请重新检查。", e);
        }
        return null;
    }

}
