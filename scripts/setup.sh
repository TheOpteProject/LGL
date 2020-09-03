#!/usr/bin/env zsh

## run this with source so correct variables are setup
## i.e. source setup.sh


readlink=readlink # start with normal zcat on path

# if we have gzcat (gnu-version), lets use it
if hash greadlink 2>/dev/null; then
	readlink=greadlnk
fi

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(${readlink} "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

echo "Setting up relative to ${DIR} as script folder"

export lgldir="$(dirname ${DIR})"
export lglimage="java -jar ${lgldir}/Java/jar/ImageMaker.jar"