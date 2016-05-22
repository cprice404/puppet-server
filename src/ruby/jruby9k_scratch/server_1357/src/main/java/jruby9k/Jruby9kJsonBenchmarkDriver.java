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
        final String jrubyVersion = args[0];
        final String gemHome = args[1];
        final String jsonGem = args[2];
        final String compileModeStr = args[3];
        RubyInstanceConfig.CompileMode compileMode;

        switch (compileModeStr){
            case "default":
                compileMode = null;
                break;
            case "off":
                compileMode = RubyInstanceConfig.CompileMode.OFF;
                break;
            case "jit":
                compileMode = RubyInstanceConfig.CompileMode.JIT;
                break;
            case "force":
                compileMode = RubyInstanceConfig.CompileMode.FORCE;
                break;
            default:
                throw new IllegalArgumentException("Unrecognized compile mode '" + compileModeStr + "'");
        }

        System.out.println("Opening maven_json_benchmark_output.txt, setting as JRuby stdout.");
        Writer writer = new FileWriter("./maven_json_benchmark_output.txt", true);
        ScriptingContainer sc = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        sc.setOutput(writer);
        if (jrubyVersion.startsWith("1.7")) {
            writer.write("Detected JRuby version 1.7.x, setting compatVersion 1.9.\n");
            sc.setCompatVersion(CompatVersion.RUBY1_9);
        } else if (jrubyVersion.startsWith("9.")) {
            writer.write("Detected JRuby version 9k, skipping compatVersion.\n");
        } else {
            throw new IllegalArgumentException("Unrecognized JRuby version: '" + jrubyVersion + "'");
        }
        if (compileMode == null) {
            writer.write("Leaving compileMode at default value.\n");
        } else {
            writer.write("Setting compileMode to: '" + compileMode + "'\n");
            sc.setCompileMode(compileMode);
        }
        writer.write("Using JSON gem: '" + jsonGem + "'\n");
        sc.setEnvironment(new HashMap<String, String>() {{
            put("GEM_HOME", gemHome);
            put("JSON_GEM", jsonGem);
            put("COMPILE_MODE", compileModeStr);
        }});
        sc.runScriptlet(PathType.RELATIVE, "./json_benchmark.rb");
        writer.close();
        sc.terminate();
    }
}
