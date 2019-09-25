package com.bytegriffin.get4j.core;

public interface JobEngine {

    void pause();

    void continues();

    void destory(String jobid);

}
