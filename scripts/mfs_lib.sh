#!/bin/bash
###############################################################################
# Author   : Shahril <selamat@sg.ibm.com>
# Subject  : mfs sh library file
#
###############################################################################
FILE=$0
FILEPATH=${FILE%/*}
FILEBASENAME=${FILE##*/}

if [ $FILEPATH == "." ]; then
  FILEPATH=`pwd`
fi

sh_cleanMFSfiles ()
{
  DATA_TESTER_DIR=$1
        MFS_TESTER_DIR=$2
        echo "****sh_cleanMFSfiles $MFS_TESTER_DIR*********"
        ls -1 $MFS_TESTER_DIR | grep -v addr | grep -v cellproto > $DATA_TESTER_DIR/mfs_fileList
  for fileN in `cat $DATA_TESTER_DIR/mfs_fileList`
        do
                echo $fileN
                if ! rm -vf $MFS_TESTER_DIR/$fileN ; then
                        sh_retcode 1
                        return 1
                fi
  done
        sh_retcode 0
        return 0
}

