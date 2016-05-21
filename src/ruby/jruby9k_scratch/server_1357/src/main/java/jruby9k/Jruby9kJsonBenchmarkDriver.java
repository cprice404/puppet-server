package jruby9k;

import org.jruby.CompatVersion;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.*;
import java.util.HashMap;

public class Jruby9kJsonBenchmarkDriver {
    public static void main(final String[] args) throws IOException {
        Writer writer = new FileWriter("./maven_json_benchmark_output.txt", true);
        ScriptingContainer sc = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        sc.setOutput(writer);
        sc.setCompatVersion(CompatVersion.RUBY1_9);
        sc.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        sc.setEnvironment(new HashMap<String, String>() {{
            put("JSON_GEM", args[0]);
        }});
        sc.runScriptlet(PathType.RELATIVE, "./json_benchmark.rb");
        writer.close();
    }
}
