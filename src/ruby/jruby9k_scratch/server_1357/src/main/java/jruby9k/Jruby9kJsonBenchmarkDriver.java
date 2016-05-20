package jruby9k;

import org.jruby.CompatVersion;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.util.HashMap;

public class Jruby9kJsonBenchmarkDriver {
    public static void main(String[] args) {
        System.out.println("Hello World.");
        ScriptingContainer sc = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        sc.setCompatVersion(CompatVersion.RUBY1_9);
        sc.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        sc.setEnvironment(new HashMap<String, String>() {{
            put("JSON_GEM", "json/ext");
        }});
        sc.runScriptlet(PathType.RELATIVE, "./json_benchmark.rb");
    }
}
