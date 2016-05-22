#!/usr/bin/env ruby

require 'benchmark'
require "#{ENV['JSON_GEM']}"

puts "JRUBY VERSION: #{JRUBY_VERSION}"
json_gem = ENV['JSON_GEM']
puts "JSON VERSION: #{json_gem}"
compile_mode = ENV['COMPILE_MODE']
puts "COMPILE MODE: #{compile_mode}"

if json_gem == 'jrjackson'
  # jrjackson supports basically the same API so we just alias the main class
  # to JSON, to match up with the other libraries.
  JSON = JrJackson::Json
end

def roundtrip()
  parsed = JSON.parse(File.read("./report.json"))
  back_to_json = JSON.generate(parsed)
end

open('maven_json_benchmark_output.csv', 'a') { |csv|
  num_warmup_runs = 100
  num_main_runs = 1000
  num_tail_runs = 100
  Benchmark.bm(7) do |x|
    warmup = x.report("warmup (#{num_warmup_runs} runs):") { (1..num_warmup_runs).each {|i| roundtrip() } }
    csv.puts("#{JRUBY_VERSION},#{json_gem},#{compile_mode},warmup,#{warmup.utime},#{warmup.stime},#{warmup.total},#{warmup.real}")
    middle = x.report("middle (#{num_main_runs} runs):") { (1..num_main_runs).each {|i| roundtrip() } }
    csv.puts("#{JRUBY_VERSION},#{json_gem},#{compile_mode},middle,#{middle.utime},#{middle.stime},#{middle.total},#{middle.real}")
    tail = x.report("tail (#{num_tail_runs} runs):") { (1..num_tail_runs).each {|i| roundtrip() } }
    csv.puts("#{JRUBY_VERSION},#{json_gem},#{compile_mode},tail,#{tail.utime},#{tail.stime},#{tail.total},#{tail.real}")
  end

  puts
}
