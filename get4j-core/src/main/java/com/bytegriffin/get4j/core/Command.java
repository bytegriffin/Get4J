package com.bytegriffin.get4j.core;

public interface Command {
	
	void begin();
	void idle();
	void continues();
	void destory();
	void pause();

}
