#!/usr/bin/env bash

set -x

echo "Hiya!  I'm foo."

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

cat "${DIR}/gem-list.txt"

while read LINE
do
  gem_name=$(echo $LINE |cut -f1)
  gem_version=$(echo $LINE |cut -f2)
  echo "NEED TO INSTALL GEM '${gem_name}', version '${gem_version}'"
done < "${DIR}/gem-list.txt"
