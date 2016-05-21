#!/usr/bin/env bash

set -e
set -x

rm maven_json_benchmark_output.txt

mvn clean
mvn -P jruby17 compile
mvn -P jruby17 exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/pure"
mvn -P jruby17 exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/ext"

mvn clean
mvn -P jruby9k compile
mvn -P jruby9k exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/pure"
mvn -P jruby9k exec:java -Dexec.mainClass=jruby9k.Jruby9kJsonBenchmarkDriver -Dexec.args="json/ext"
