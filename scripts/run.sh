#!/usr/bin/env bash
rundir=`dirname $0`
rundir=`readlink -f $rundir`
topdir=$rundir/../..
topdir=`readlink -f $topdir`

function exit_if_error
{
	status=$1
	if [ $status -ne 0 ]; then
		msg="$2"
		echo "Error: $msg" >&2
		exit $status
	fi
}

function convertsecs
{
	h=$(( $1 / 3600 ))
	m=$(( $1  % 3600 / 60 ))
	s=$(( $1 % 60 ))
	printf "%02d:%02d:%02d\n" $h $m $s
}

compile=yes
while getopts C opt; do
	case $opt in
		C)
			compile=no
			;;
		\?)
			echo "Invalid option: -$OPTARG" >&2
			exit 3
	esac
done

cd $topdir

if [ "$compile" == "yes" ]; then
	./setup.pl -i 
	exit_if_error $? "compile failed"
fi

tmpdir=$rundir/tmp
rm -rf $tmpdir && mkdir $tmpdir
exit_if_error $? "failed to prepare tmpdir $tmpdir"

threads=$(lscpu -p | egrep -v '^#' | wc -l)
threads=$(((threads-2)>2 ? (threads-2) : 2))
echo "Selecting to go with $threads threads (based on max(cores-2, 2))"
					     
config=$rundir/setup.cfg
if [ -e $config ]; then
	echo "$config already exists, using it..."
else
	./setup.pl -c $config
	exit_if_error $? "failed to generate config file $config"
	sed -i "s|^inputfile =.*|inputfile = '$rundir/$ncol'|" $config && \
	    sed -i "s|^tmpdir =.*|tmpdir = '$tmpdir'|" $config && \
	    sed -i "s|^threadcount =.*|threadcount = '$threads'|" $config && \
	    sed -i "s|^treelayout =.*|treelayout = '1'|" $config && \
	    sed -i "s|^useoriginalweights =.*|useoriginalweights = '1'|" $config && \
	    sed -i "s|^placeleafsclose =.*|placeleafsclose = '1'|" $config && \
	    echo "lgldir = '$topdir/bin'" >> $config
	## threadcount = '1'
	exit_if_error $? "failed to adjust config file $config"
fi

export PERL5LIB=$topdir/perls
start_time=`date +%s`
outfile=$rundir/$start_time.out
$topdir/bin/lgl.pl -c $config | tee $outfile
exitcode=$?
end_time=`date +%s`
echo
echo "lgl.pl took `convertsecs $(( $end_time - $start_time ))` to run." | tee -a $outfile
exit_if_error $exitcode "lgl.pl failed with code $exitcode"

echo "Generating color_file..." | tee -a $outfile
perl $topdir/perls/colorEdgesBasedOnLevel.pl $tmpdir/*/0.coords.edge_levels > $rundir/color_file

echo "Opening viewer(s)..." | tee -a $outfile
jar_path=$topdir/lglview.jar
if [ ! -e $jar_path -a -e $topdir/Java/lglview.jar ]; then
	jar_path=$topdir/Java/lglview.jar
fi
if [ ! -e $jar_path -a -e $topdir/Java/jar/LGLView.jar ]; then
	jar_path=$topdir/Java/jar/LGLView.jar
fi

view_command="java -jar $jar_path $tmpdir/*/0.lgl $tmpdir/*/0.coords"
echo $view_command | tee -a $outfile
$view_command >/dev/null 2>&1 &
view_command="java -jar $jar_path $tmpdir/final.mst.lgl $tmpdir/final.coords"
echo $view_command | tee -a $outfile
$view_command >/dev/null 2>&1 &

# Lets create a nice image as well
view_command="java -jar $topdir/Java/jar/ImageMaker.jar 2400 2400 $tmpdir/*/0.lgl $tmpdir/*/0.coords"
echo $view_command | tee -a $outfile

