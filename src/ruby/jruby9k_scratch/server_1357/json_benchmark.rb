#!/usr/bin/env ruby

require 'benchmark'
require "#{ENV['JSON_GEM']}"

def roundtrip()
  parsed = JSON.parse(File.read("./report.json"))
  back_to_json = JSON.generate(parsed)
end

puts "JRUBY VERSION: #{JRUBY_VERSION}"
puts "JSON VERSION: #{ENV['JSON_GEM']}"

num_warmup_runs = 100
num_main_runs = 1000
num_tail_runs = 100
Benchmark.bm(7) do |x|
  x.report("warmup (#{num_warmup_runs} runs):") { (1..num_warmup_runs).each {|i| roundtrip() } }
  x.report("middle (#{num_main_runs} runs):") { (1..num_main_runs).each {|i| roundtrip() } }
  x.report("tail (#{num_tail_runs} runs):") { (1..num_tail_runs).each {|i| roundtrip() } }
end

puts
