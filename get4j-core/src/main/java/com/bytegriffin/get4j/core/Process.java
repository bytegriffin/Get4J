package com.bytegriffin.get4j.core;

import com.bytegriffin.get4j.conf.Seed;

public interface Process {

    void init(Seed seed);

    void execute(Page page);

}
