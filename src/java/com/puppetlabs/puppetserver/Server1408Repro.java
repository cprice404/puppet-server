package com.puppetlabs.puppetserver;

import com.puppetlabs.puppetserver.jruby.ScriptingContainer;
import org.jruby.embed.LocalContextScope;

public class Server1408Repro {
    public static void main(String[] args) throws InterruptedException {
        String objIdIncScript =
                "(0..2**32).each {|i| ''.object_id }\n" +
                "puts \"new object id: #{''.object_id}\"\n";
        String throwScript =
                "begin\n" +
                "  throw :foo\n" +
                "rescue => e\n" +
                "  puts \"CAUGHT EXCEPTION: #{e}\"\n" +
                "end\n";

        ScriptingContainer sc = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        long start = System.currentTimeMillis();
        System.out.println("Beginning object id generation.");
        sc.runScriptlet(objIdIncScript);
        System.out.println("Finished object id generation in " + (System.currentTimeMillis() - start) + " ms.");

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                sc.runScriptlet(throwScript);
            }
        });
        t.start();
        t.join();
    }
}
