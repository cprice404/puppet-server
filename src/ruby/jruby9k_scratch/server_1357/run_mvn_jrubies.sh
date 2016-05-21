#!/usr/bin/env bash

set -x

rm maven_json_benchmark_output.txt

set -e

mvn clean
mvn -P jruby17 compile
mvn -P jruby17 exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/pure off"
mvn -P jruby17 exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/pure on"
mvn -P jruby17 exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/ext off"
mvn -P jruby17 exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/ext on"

mvn clean
mvn -P jruby9k compile
mvn -P jruby9k exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/pure off"
mvn -P jruby9k exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/pure on"
mvn -P jruby9k exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/ext off"
mvn -P jruby9k exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/ext on"
