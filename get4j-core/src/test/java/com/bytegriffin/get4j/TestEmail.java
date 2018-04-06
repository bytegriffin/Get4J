package com.bytegriffin.get4j;

import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.send.EmailSender;

public class TestEmail {

	public static void main(String[] args) {
		Globals.emailSender = new EmailSender("get4jvip@163.com;get4j@sina.com");
		try {
			int a = 199 / 0;
			System.out.println(a);
		} catch (Exception e) {
			EmailSender.sendMail(e);
		}
	}

}
