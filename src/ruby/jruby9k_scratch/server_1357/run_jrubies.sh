#!/usr/bin/env bash

set -e

eval "$(rbenv init -)"

rbenv shell jruby-1.7.20.1

set -x

export JSON_GEM='json/pure'
./json_benchmark.rb
export JSON_GEM='json/ext'
./json_benchmark.rb

set +x

rbenv shell jruby-9.1.1.0

set -x

export JSON_GEM='json/pure'
./json_benchmark.rb
export JSON_GEM='json/ext'
./json_benchmark.rb
