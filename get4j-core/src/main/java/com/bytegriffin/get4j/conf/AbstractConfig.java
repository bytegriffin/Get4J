package com.bytegriffin.get4j.conf;

import java.io.File;

/**
 * 配置文件
 */
public abstract class AbstractConfig {

    /**
     * 配置文件加载
     * @return 加载配置对象
     */
    abstract Object load();

    // 配置文件路径
    protected static final String conf_path = System.getProperty("user.dir") + File.separator + "conf" + File.separator;
    // xml配置文件
    protected final static String core_seeds_xml_file = conf_path + "core-seeds.xml";
    // xml格式检验文件
    protected final static String core_seeds_xsd_file = conf_path + "core-seeds.xsd";
    // 全局xml配置文件
    protected final static String configuration_xml_file = conf_path + "configuration.xml";
    // 资源同步yaml文件
    public final static String resource_sync_yaml_file = conf_path + "resource-sync.yaml";
    // 全局xml配置文件
    protected final static String dynamic_fields_xml_file = conf_path + "dynamic-fields.xml";
    // xml格式检验文件
    protected final static String dynamic_fields_xsd_file = conf_path + "dynamic-fields.xsd";
    
    /******** xml node ************/
    protected static final String seed_node = "seed";
    protected static final String name_node = "name";
    protected static final String property_node = "property";
    protected static final String value_node = "value";
    protected static final String configuration_node = "configuration";

    /******** xml node name ************/
    protected static final String seed_name = "seed.name";
    protected static final String node_name = "node.name";
    protected static final String woker_thread_count = "worker.thread.count";
    protected static final String fetch_page_mode = "fetch.page.mode";
    protected static final String fetch_url = "fetch.url";
    protected static final String fetch_http_method = "fetch.http.method";
    protected static final String fetch_probe_selector = "fetch.probe.selector";
    protected static final String fetch_probe_sleep = "fetch.probe.sleep";
    protected static final String fetch_detailLink_selector = "fetch.detailLink.selector";
    protected static final String fetch_total_pages = "fetch.total.pages";
    protected static final String fetch_login_username = "fetch.login.username";
    protected static final String fetch_login_password = "fetch.login.password";
    protected static final String fetch_timer_interval = "fetch.timer.interval";
    protected static final String fetch_timer_start = "fetch.timer.start";
    protected static final String fetch_http_proxy = "fetch.http.proxy";
    protected static final String fetch_http_user_agent = "fetch.http.user_agent";
    protected static final String fetch_sleep = "fetch.sleep";
    protected static final String fetch_sleep_range = "fetch.sleep.range";
    protected static final String fetch_javascript_support = "fetch.javascript.support";
    protected static final String fetch_resource_selector = "fetch.resource.selector";
    protected static final String download_disk = "download.disk";
    protected static final String download_hdfs = "download.hdfs";
    protected static final String parse_class_impl = "parse.class.impl";
    protected static final String parse_element_selector = "parse.element.selector";
    protected static final String store_jdbc = "store.jdbc";
    protected static final String store_mongodb = "store.mongodb";
    protected static final String store_hbase = "store.hbase";
    protected static final String store_lucene_index = "store.lucene.index";
    protected static final String download_filename_rule = "download.filename.rule";
    protected static final String email_recipient = "email.recipient";

    /******** yaml node ************/
    public static final String ftp_node = "ftp";
    public static final String rsync_node = "rsync";
    public static final String scp_node = "scp";
    public static final String protocal_node = "protocal";
    public static final String batch_count_node = "batch.count";
    public static final String batch_time_node = "batch.time";
    public static final String open_node = "open";
    public static final String host_node = "host";
    public static final String port_node = "port";
    public static final String username_node = "username";
    public static final String password_node = "password";
    public static final String dir_node = "dir";
    public static final String module_node = "module";
    
    /******** xml node ************/
    protected static final String fields_node = "fields";
    protected static final String field_node = "field";
    protected static final String selector_node = "selector";

}
