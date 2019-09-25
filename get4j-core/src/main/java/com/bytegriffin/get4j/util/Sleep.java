package com.bytegriffin.get4j.util;

import java.util.concurrent.TimeUnit;

public final class Sleep {

	public static void seconds(long timeout) {
		try {
			TimeUnit.SECONDS.sleep(timeout);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
