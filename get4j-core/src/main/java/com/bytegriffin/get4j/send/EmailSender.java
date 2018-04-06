package com.bytegriffin.get4j.send;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.MD5Util;
import com.google.common.collect.Lists;
import com.sun.mail.util.MailSSLSocketFactory;

/**
 * 邮件发送器：当系统出现异常会将相关信息发送给指定接收人<br>
 * 为了避免反复发送相同内容的邮件和频繁启动线程，强制将相同错误的邮件仅发送一次。<br>
 * 系统一旦发生异常，就会自动发送一封邮件给系统管理员。<br>
 * 注意：接受者的环境必须能够收到sina邮箱发送的邮件
 */
public final class EmailSender implements Runnable{

	private static final Logger logger = LogManager.getLogger(EmailSender.class);
	private static final String host = "smtp.sina.com";
	private static final String username = "get4j@sina.com";
	// 加密过的授权密码
	private static final String encrypt_password = "Z2V0NGp2aXA=";
	private static String subject = "【Get4J提醒】爬虫系统发现异常";
	private static String recipient;// 邮件接收人
	private static Session session;
	// 存放已经发送过的内容，为了节省内存已经过md5加密成定长字符串
	private static List<String> md5_sended_content = Lists.newArrayList();
	private String content;

	private void setContent(String content){
		this.content = content;
	}
	
	public EmailSender(String recipient){
		init(recipient);
	}

	public void init(String recipient) {
		EmailSender.recipient = recipient;
		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.smtp.host", host);
		props.setProperty("mail.smtp.auth", "true");
		// props.put("mail.debug", "true"); //发送出错时可以打开调试模式
		try {
			MailSSLSocketFactory sf = new MailSSLSocketFactory();
			sf.setTrustAllHosts(true);
			props.put("mail.smtp.ssl.enable", "true");
			props.put("mail.smtp.ssl.socketFactory", sf);
			session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					// 客户端授权密码，并非用户登录密码
					return new PasswordAuthentication(username, getPassword());
				}
			});
		} catch (Exception e) {
			logger.error("系统组件EmailSender初始化失败。", e);
		}
		logger.info("系统组件EmailSender初始化完成。");
	}
	
	private String getPassword(){
		byte[] asBytes = Base64.getDecoder().decode(encrypt_password);
		try {
			return new String(asBytes, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("系统组件EmailSender转换Email发送者密码出错", e);
		}
		return null;
	}

	/**
	 * 调用入口方法一
	 * @param content  String
	 */
	public static void sendMail(String content){
    	if(Globals.emailSender == null){
			return;
		}
    	sendOnce(content);
    }
	
	/**
	 * 调用入口方法二
	 * @param t 异常
	 */
	public static void sendMail(Throwable t){
    	if(Globals.emailSender == null){
			return;
		}
    	String content = ExceptionCatcher.getStackTrace(t);
    	sendOnce(content);
    }
	
	/**
	 * 保证相同内容仅发送一次
	 * @param content String
	 */
	private static void sendOnce(String content){
		String baseContent = MD5Util.generateSeedName(content);
		if(md5_sended_content.contains(baseContent)){
			return;
		}
		md5_sended_content.add(baseContent);
		Globals.emailSender.setContent(content);
    	ExecutorService es = Executors.newSingleThreadExecutor();
    	es.execute(Globals.emailSender);
    	es.shutdown();
	}

	/**
	 * 当系统出现异常，系统会由get4j@sina.com发送给邮件接受者一封提醒邮件
	 */
	@Override
	public void run() {
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			setMultiReciption(message);
			message.setSubject(subject);
			message.getSentDate();
			//message.setContent(content, "text/html; charset=utf-8");
			message.setContent(buildHtmlBody(content));
			message.saveChanges();
			Transport.send(message);
			logger.info("线程[" + Thread.currentThread().getName() + "]发送Email完成。");
		} catch (MessagingException e) {
			// 如果发现是504错误，修改邮件标题和邮件内容，否则126会当成垃圾邮件处理，因此默认修改为sina邮箱
			logger.info("线程[" + Thread.currentThread().getName() + "]发送Email时失败。", e);
		}
	}

	/**
	 * 设置邮件接收人
	 * @param message 消息
	 * @throws MessagingException
	 * @throws AddressException
	 */
	private void setMultiReciption(MimeMessage message) throws AddressException, MessagingException {
		if (recipient.contains(DefaultConfig.email_recipient_split)) {
			String[] persons = recipient.split(DefaultConfig.email_recipient_split);
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(persons[0]));
			for(int i=1; i<persons.length; i++){
				message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(persons[i]));
			}
		} else {
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
		}
	}

	/**
	 * 构建邮件Html正文
	 * 
	 * @param exception 字符串类型的异常信息
	 * @return
	 */
	private MimeMultipart buildHtmlBody(String exception) {		
		MimeMultipart multipart = new MimeMultipart("alternative");
		try {
			// html部分
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			StringBuilder sb = new StringBuilder();
			sb.append("您好：<br>");
			sb.append("&nbsp;&nbsp;&nbsp;&nbsp;Get4J在["+DateUtil.getCurrentDate()+"]时刻发现的问题：<br>");
			sb.append(exception.replaceAll("\\s+at+\\s", "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; at "));
			messageBodyPart.setContent(sb.toString(), "text/html; charset=utf-8");
			multipart.addBodyPart(messageBodyPart);

			// 图片部分，如果要加图片背景，需要设置MimeMultipart为related
//			messageBodyPart = new MimeBodyPart();
//			DataSource fds = new FileDataSource("https://raw.githubusercontent.com/bytegriffin/get4j/master/logo.png");
//			messageBodyPart.setDataHandler(new DataHandler(fds));
//			messageBodyPart.setContentID("image");
//			multipart.addBodyPart(messageBodyPart);
			return multipart;
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return multipart;
		
	}

}
