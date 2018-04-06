package com.bytegriffin.get4j.probe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.MD5Util;
import com.google.common.collect.Lists;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Probe文件存储器 <br>
 */
public class ProbeFileStorage {

    private static final Logger logger = LogManager.getLogger(ProbeFileStorage.class);

    /**
     * probe序列化文件中对象的固定长度字节，这样才更加方便地遍历出文件中存储的多个相同大小的ProbePage对象
     * 可通过probePage.getSerializedSize()来获取大小值
     */
    private static final int probe_buffer_length = 71;

    /**
     * 设置状态位是为了抓取过程中程序非法关闭，重启后仍然能继续抓取，
     * 而不是判断probe文件有内容就不抓
     * 0表示抓取未完成，重启程序后需要继续抓取
     */
    static final String un_finish = "0";

    /**
     * 1表示已完成，重启程序后不用再抓，直接轮训监控
     */
    static final String finished = "1";

    /**
     * 读取Probe文件中与url相等的值
     *
     * @param url String
     * @return ProbePage
     */
    public static ProbePage read(String url) {
        ProbePage page = null;
        try (InputStream is = Files.newInputStream(Paths.get(DefaultConfig.probe_page_file), StandardOpenOption.READ)) {
            byte[] buff = new byte[probe_buffer_length];
            String md5url = MD5Util.convert(url);
            while (is.read(buff, 0, probe_buffer_length) > 0) {
            	ProbePage probepage = deserialize(buff);
                if (probepage.getUrl().equalsIgnoreCase(md5url)) {
                    page = probepage;
                    break;
                }
            }
            return page;
        } catch (IOException e) {
            logger.error("读probe文件时出错。", e);
            EmailSender.sendMail(e);
        	ExceptionCatcher.addException(e);
        }
        return page;
    }

    /**
     * 读取Probe文件
     *
     * @return
     */
    private static List<ProbePage> reads() {
        List<ProbePage> list = Lists.newArrayList();
        try (InputStream is = Files.newInputStream(Paths.get(DefaultConfig.probe_page_file), StandardOpenOption.READ)){
            byte[] buff = new byte[probe_buffer_length];
            while (is.read(buff, 0, probe_buffer_length) > 0) {
            	ProbePage probepage = deserialize(buff);
                list.add(probepage);
            }
            return list;
        } catch (Exception e) {
            logger.error("读probe文件时出错。", e);
            EmailSender.sendMail(e);
        	ExceptionCatcher.addException(e);
        } 
        return null;
    }

    /**
     * 更新Probe文件：不能直接删除并更新数据，需要生成一个临时文件进行转存
     *
     * @param url
     * @param content
     * @param status
     * @return 是否更新成功
     */
    synchronized static ProbePage update(String url, String content, String status) {
        ProbePage newProbePage = null;
        // 2.1 更新就是先创建一个临时文件 
        String tempfile = DefaultConfig.probe_page_file + "." + System.currentTimeMillis() + ".tmp";
        File tempFile = new File(tempfile);
        try {
        	tempFile.createNewFile();
        	List<ProbePage> list = reads();
            // 2.2 将probe文件除了url相同的数据转存到临时文件中
            for (ProbePage pp : list) {
                if (MD5Util.convert(url).equals(pp.getUrl())) {
                    continue;
                }
                append(tempFile.getCanonicalPath(), pp.getUrl(), pp.getContent(), pp.getStatus(), false);
            }
            // 2.3 将要更新的数据追加到临时文件中
            newProbePage = append(tempFile.getCanonicalPath(), url, content, status, true);
            // 2.4 将临时文件重命名为probe文件，原probe文件存在的话就直接覆盖
            // 必须要在流关闭之后才能重命名，否则在windows系统下报错
            if (!list.isEmpty()) {
            	Files.move(Paths.get(tempfile), Paths.get(DefaultConfig.probe_page_file), StandardCopyOption.REPLACE_EXISTING);
            }
            return newProbePage;
        } catch (IOException e) {
            logger.error("更新probe文件时出错。", e);
            EmailSender.sendMail(e);
        	ExceptionCatcher.addException(e);
        } 
        return newProbePage;
    }

    /**
     * 追加内容到Probe文件：将url和页面内容转成固定长度的MD5格式，
     * 以便每次探测时进行判断内容是否变化
     *
     * @param file
     * @param url
     * @param content
     * @param status
     * @param isEncode 是否进行加密
     */
    synchronized static ProbePage append(String file, String url, String content, String status, boolean isEncode) {
       ProbePage probePage = null;
        try {
        	if(isEncode){
        		url = MD5Util.convert(url);
        		content = MD5Util.convert(content);
        	}
        	probePage = ProbePage.create(url, content, status);
        	byte[] bytes = serialize(probePage);
        	Files.write(Paths.get(file), bytes, StandardOpenOption.APPEND);
            return probePage;
        } catch (IOException e) {
            logger.error("追加probe文件时出错。", e);
            EmailSender.sendMail(e);
        	ExceptionCatcher.addException(e);
        } 
        return probePage;
    }

    /**
     * 序列化  ProbePage === byte[]
     * @param probePage 探测页面对象
     * @return 序列化后的探测页面字节数组
     */
    public static byte[] serialize(ProbePage probePage) {
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<ProbePage> schema = RuntimeSchema.getSchema(ProbePage.class);
            return ProtostuffIOUtil.toByteArray(probePage, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化   byte[] === ProbePage
     * @param data 字节数组
     * @return 反序列化后的探测页面对象
     */
    public static ProbePage deserialize(byte[] data) {
        try {
            Schema<ProbePage> schema = RuntimeSchema.getSchema(ProbePage.class);
            ProbePage probePage = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(data, probePage, schema);
            return probePage;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}
