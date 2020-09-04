#!/bin/bash

## This script just creates the images I use in my thesis
## It is not meant to run on any other machine than my machines

## Load exports
export lgldir="$(dirname $(pwd))"
export lglimage="java -jar ${lgldir}/Java/jar/ImageMaker.jar"
export runfolder="../testrun"

function generategraph(){
    folder=$(echo $1 | cut -d ' ' -f1)
    folderpath="${runfolder}/${folder}"
    if [ -d "$folderpath" ]; then
        res=$(echo $1 | cut -d " " -f2)
        echo "Generating image of ${folderpath} with resolution ${res}"
        $lglimage "$res" "$res" "${folderpath}/${folder}.lgl" "${folderpath}/${folder}.coords" -c "${folderpath}/color_file" > /dev/null
        cp "${folderpath}/${folder}.coords_${res}x${res}_light.png" "${folderpath}/${folder}.latest_light.png"
        cp "${folderpath}/${folder}.coords_${res}x${res}_dark.png" "${folderpath}/${folder}.latest_dark.png"
        cp "${folderpath}/${folder}.coords_${res}x${res}_transparent.png" "${folderpath}/${folder}.latest_transparent.png"
        #open "${folder}/${folder}.coords_${2}x${2}_dark.png"
        #open "${folder}/${folder}.coords_${2}x${2}_light.png"
        #open "${folder}/${folder}.coords_${2}x${2}_transparent.png"
    else
        echo "${folderpath} does not exist, skipping"
    fi
}

function mytest()
{
    echo "mytest 1:${1} end my test"
    folder=$(echo $1 | cut -d " " -f1)
    res=$(echo $1 | cut -d " " -f2)
    echo "res $res folder $folder"
}

function mylglstaticinternet(){

    threads=2
    . $(which env_parallel.bash)

    env_parallel -P ${threads} -n 1 generategraph ::: "bview_2020_09	2000"\
            "bview_2019_09	1935"\
            "bview_2018_09	1861"\
            "bview_2017_09	1741"\
            "bview_2016_09	1668"\
            "bview_2015_09	1583"\
            "bview_2014_09	1538"\
            "bview_2013_09	1483"\
            "bview_2012_09	1410"\
            "bview_2011_09	1323"\
            "bview_2010_09	1215"\
            "bview_2009_09	1157"\
            "bview_2008_09	1097"\
            "bview_2007_09	989"\
            "bview_2006_09	911"\
            "bview_2005_09	843"\
            "bview_2004_09	785"\
            "bview_2003_09	732"\
            "bview_2002_09	690"\
            "bview_2001_09	671"\
            "bview_2000_09	610"\    
}


mylglstaticinternet

### Old images (licentiate)
    # echo "bview.20000901.0610   651"\
    #      "bview.20010901.1508   715"\
    #      "bview.20020903.0454   733"\
    #      "bview.20030903.0000   773"\
    #      "bview.20040902.0000   829"\
    #      "bview.20050902.0000   894"\
    #      "bview.20060902.0000   955"\
    #      "bview.20070902.0759   1034"\
    #      "bview.20080902.0759   1130"\
    #      "bview.20090902.0759   1192"\
    #      "bview.20100902.0000   1255"\
    #      "bview.20110902.0000   1346"\
    #      "bview.20120902.0000   1431"\
    #      "bview.20130902.0000   1519"\
    #      "bview.20140902.0000   1573"\
    #      "bview.20150902.0000   1641"\
    #      "bview.20160902.0000   1728"\
    #      "bview.20170902.0000   1803"\
    #      "bview.20180902.0000   1899"\
    #      "bview.20190902.0000   2000"\
    #     | xargs -P ${threads} -n 2 mylglgenerate.sh