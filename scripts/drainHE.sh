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

if [ $# -eq 0 ]; then
  echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] [val_user|suspend|claim_in <workunit>|claim_out]"
  echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] [stepStart <stepName> <stepid>|stepData <data>|stepEnd] "
  exit 1
fi

#EXIT/RETURN CODE
# 11 : MFS: Workunit is NOT valid or is NOT in the correct op 0807
# 12 : Duluth Testcell: Retry or Need TE to cleanup MFS testcell in duluth
# 13 : Duluth Testcell: Fail to copy file from cellproto. Retry
# 14 : MFS: Fail to login, check user authority or op authority
# 15 : Duluth Testcell: Fail to setstat, retry
# 16 : MFS: Workunit is not A frame workunit
# 17 : drainMFS.cfg file does not exist

#dlthUser=reefuser
#dlthSvr=sgad06e0.sg.ibm.com
#testcell=sga2k113
dlthUser=$1
dlthSvr=$2
testcell=$3
celltype=""

display=9.127.45.111:0.0

echo $@
sh_readCFG()
{
  if [ ! -f "drainMFS.cfg" ]
  then
    echo "drainMFS.cfg file does not exist!!!! Exiting."
    exit 17
  fi
  dlthUser=`grep Duluth_User drainMFS.cfg | awk '{print $2}'`
  dlthSvr=`grep Duluth_Server drainMFS.cfg | awk '{print $2}'`
  #testcell=`grep Testcell drainMFS.cfg | awk '{print $2}'`
  celltype=`grep Celltype drainMFS.cfg | awk '{print $2}'`
  echo "drainMFS.cfg file exist"
  cat drainMFS.cfg
}

if [ $4 != "setup" ]; then
  sh_readCFG
fi 

sh_cleanMFSfiles ()
{
  ssh -XY -l $dlthUser $dlthSvr "cd /testcells/$testcell;ls | grep -v addrfile.SGA| grep -v TCOchk| grep -v TCOinProg| grep -v cellproto| grep -v usage.tco| grep -v code | grep -v DOSCNFIG.DAT | grep -v teststat | grep -v mas_backup | grep -v tstfork.out" > mfs_fileList

  for fileN in `cat mfs_fileList`
  do
    echo $fileN
    if ! ssh -XY -l $dlthUser $dlthSvr "cd /testcells/$testcell;rm $fileN"; then
       echo "Could not clean MFS files in $dlthSvr:/testcell/$testcell"
       echo "Reset the testcell $testcell in duluth server $dlthSvr" 
       return 12
    fi
  done

  return 0
}

sh_resetMFSfile ()
{
  if ! ssh -XY -l $dlthUser $dlthSvr "cd /testcells/$testcell;cp  cellproto/DOSCNFIG.DAT ." ; then
    return 13
  fi 
  if ! ssh -XY -l $dlthUser $dlthSvr "cd /testcells/$testcell;cp  cellproto/addrfile.* ." ; then
    return 13
  fi 
  if ! ssh -XY -l $dlthUser $dlthSvr "cd /testcells/$testcell;cp  cellproto/teststat ." ; then
    return 13
  fi 
  return 0
}

sh_cleanProcess()
{
  ssh -l $dlthUser $dlthSvr "ps -ef| grep $testcell |grep -v grep|grep -v tco_chk| awk '{print \$2}'" > mfstransacPID
  #ssh -l $dlthUser $dlthSvr "ps -ef| grep $testcell | awk '{print \$2}'" 
  if [ `wc -l mfstransacPID | awk '{print $1}'` -eq 0 ]; then
    echo "No ghost process running in MFS interface, GOOD"
  else
    for pid_t in `cat mfstransacPID`; do
      ssh -l $dlthUser $dlthSvr "kill $pid_t"
      echo "==> MFS process id $pid_t killed"
    done
    echo "CLEANUP COMPLETED"
  fi

}

sh_validateUser ()
{
  sh_cleanProcess
  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;/mfs/VAL_USER.KSH $testcell" ; then
    ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;getstat RETCODE"
    echo "Fail to login to MFS client"
    return 14
  fi
  echo "Successfully login"
  rm -rf claimedWU
  mkdir claimedWU
  scp -q $dlthUser@$dlthSvr:/testcells/$testcell/* claimedWU
  return 0
}

sh_suspend ()
{
  sh_cleanProcess
  sh_svtr
  scp claimedWU/step.log $dlthUser@$dlthSvr:/testcells/$testcell
  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat SUSPEND_QC 45" ; then
    return 15
  fi
  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat PRCSTAT SUSPEND" ; then
    return 15
  fi

  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;/mfs/MFS_SUSPEND_S.KSH $testcell" ; then
    echo "Failed to suspend"
    exit 1
  fi
  return 0
}

sh_svhd ()
{
# SVHD  P8BRPOPR   9119   1ATQKY6   SGA2K111  START             2016-04-19  20.26.03                                     .
# SVHD  OPERATIN   MTM    MFGNNOx   CELLxxxx  START             DATExxxxxx  TIMExxxx
# SVHD  DRAINN63   2964   0YBDC51   sga2k113  START             2016-05-05  22.29.47                                     .
 OPERATION="DRAIN`grep MODEL claimedWU/teststat | cut -c 8-10`"
 MTM="`grep MT claimedWU/teststat | cut -c 5-8`"
 MFGNO="`grep SYSTEM claimedWU/teststat | cut -c 9-15`"
 CELL=${testcell}xxxx
 CELL=${CELL:0:8}
 DATE=`date +"%Y-%m-%d"`
 TIME=`date +"%H.%M.%S"`
 echo "SVHD  $OPERATION   $MTM   $MFGNO   $CELL  START             $DATE  $TIME                                     ." > claimedWU/step.log
 echo "SVHD~~$OPERATION~~~$MTM~~~$MFGNO~~~$CELL~~STARTX~~~~~~~~~~~~1111111111~~22222222~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~." > claimedWU/SVHD
}

sh_svtr()
{
  DATE=`date +"%Y-%m-%d"`
  TIME=`date +"%H.%M.%S"`
  SVTR=`cat claimedWU/SVHD`
  #echo $SVTR
  SVTR=`echo $SVTR|sed -e 's/ /~/g'`
  #echo $SVTR
  SVTR=${SVTR/SVHD/SVTR}
  SVTR=${SVTR/STARTX/PASSED}
  SVTR=${SVTR/1111111111/$DATE}
  SVTR=${SVTR/22222222/$TIME}
  echo $SVTR | sed -e 's/~/ /g' >> claimedWU/step.log
}

sh_up()
{
  sh_cleanProcess
  echo test
  STNTYP=$celltype
  STNSTAT=$STNTYP
  cat dummystep.log >> claimedWU/step.log
  sh_svtr
  scp claimedWU/step.log $dlthUser@$dlthSvr:/testcells/$testcell
  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat STNTYP $STNTYP" ; then
    return 15
  fi
  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat STNSTAT $STNSTAT" ; then
    return 15
  fi
  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat PRCSTAT PASSED" ; then
    return 15
  fi
  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat RETCODE 9" ; then
    return 15
  fi
  if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;/mfs/MFS_UP_S.KSH $testcell" ; then
    echo "FAIL UP, fail to claimout"
    return 1
  fi
  timestamp=`date +"%m%d%Y%H%M%S"`
  cat dummystep.log > dummystep.$timestamp
  > dummystep.log
}

sh_baseParamatric()
{
  if [ "$7" != "DUMMY" ]; then
    USERID=`grep "(USERID " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    MT=`grep "(MT " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    MODEL=`grep "(MODEL " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    CUR_OPNUM=`grep "(CUR_OPNUM " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    SN=`grep "(SN " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    ORDRNUM=`grep "(ORDRNUM " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    CTRL_NUM=`grep "(CTRL_NUM " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    SYSTEM=`grep "(SYSTEM " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    STNTYP=`grep "(STNTYP " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    DRAINSTN=`grep "(!STNID " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `

    echo "USERID     $USERID" > order.cfg
    echo "MT         $MT" >> order.cfg
    echo "MODEL      $MODEL" >> order.cfg
    echo "CUR_OPNUM  $CUR_OPNUM" >> order.cfg 
    echo "SN         $SN " >> order.cfg
    echo "ORDRNUM    $ORDRNUM" >> order.cfg
    echo "CTRL_NUM   $CTRL_NUM" >> order.cfg
    echo "SYSTEM     $SYSTEM" >> order.cfg
    echo "STNTYP     $STNTYP" >> order.cfg
    echo "DRAINSTN   $DRAINSTN" >> order.cfg
  else
    echo DUmmyInfo
   #sh_baseParamatric $USERID $1 $2 0807 $3 $4 DUMMY $5 AHECPC $6
    USERID=$1
    MT=$2
    MODEL=$3
    CUR_OPNUM=$4
    SN=$5
    ORDRNUM=$6
    CTRL_NUM=$7
    SYSTEM=$8
    STNTYP=$9
    DRAINSTN=${10}
  fi 
    echo "USERID|MT|MODEL|CUR_OPNUM|SN"
    echo "$USERID|$MT|$MODEL|$CUR_OPNUM|$SN"
    echo "ORDRNUM|CTRL_NUM|SYSTEM|STNTYP|DRAINSTN"
    echo "$ORDRNUM|$CTRL_NUM|$SYSTEM|$STNTYP|$DRAINSTN"
    sh_stepStart "BASEPARA" "A100"
    sh_stepData "USERID|MT|MODEL|CUR_OPNUM|SN"
    sh_stepData "$USERID|$MT|$MODEL|$CUR_OPNUM|$SN"
    sh_stepData "ORDRNUM|CTRL_NUM|SYSTEM|STNTYP|DRAINSTN"
    sh_stepData "$ORDRNUM|$CTRL_NUM|$SYSTEM|$STNTYP|$DRAINSTN"
    sh_stepEnd
}

sh_dn ()
{
    STNTYP=$celltype
    STNSTAT=$STNTYP
    PRCSTAT=START
    RETCODE=9
    CTRL_NUM=$1 # Mfnno : 0YBDC51 #<== 
    #CTRL_NUM=3BDL62MZ # Mfnno : 0YBDC51 #<== 
    #CTRL_NUM=0YBDC51 #<== 

    sh_cleanProcess
    if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat STNTYP $STNTYP" ; then
      return 15
    fi
    if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat STNSTAT $STNSTAT" ; then
      return 15
    fi
    if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat PRCSTAT START" ; then
      return 15
    fi
    if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat RETCODE 9" ; then
      return 15
    fi
    if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;setstat CTRL_NUM $CTRL_NUM" ; then
      return 15
    fi
    if ! ssh -XY -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;/mfs/MFS_DOWN_S.KSH $testcell" ; then
    #if ! ssh -X -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;/mfs/MFS_DOWN.KSH $testcell" ; then
      echo FAILDN
      ssh -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;cat LOG*"
      
      ssh -l $dlthUser $dlthSvr ". /home/$dlthUser/.profile;cd /testcells/$testcell;grep -q \"not found in WU10 file\" LOG.0000000"
      if (($? == 0)); then
        echo "Workunit $CTRL_NUM is not valid!!!"
#        echo "Check correct MFS environment or workunit entered is wrong"
      fi
      return 11
    fi
    #Grep all claimed in files into directory claimedWU
    rm -rf claimedWU
    mkdir claimedWU
    scp -q $dlthUser@$dlthSvr:/testcells/$testcell/* claimedWU
    echo "Workunit $CTRL_NUM files are located in directory claimedWU"

    #Check and ensure it's A frame
    frame=`cat claimedWU/*HWO | cut -c 38-39 | grep -v Pl|grep -v ==|sort -u|head -1`
    echo "frame==>$frame"
    if [ "$frame" != "0A" ]; then
      sh_suspend
      echo "$CTRL_NUM is NOT A frame"
      echo "Pls try again using workunit of A frame"
      exit 16
    else
     echo ""
    fi

    sh_svhd
  sh_baseParamatric 
  return 0 
}

sh_dummyInfo ()
{
  
  #DUMMY mt model SN orderNum mfgno
  #userid mt model SN orderNum workunit mfgno
    USERID=`grep "(USERID " claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g' `
    echo "USERID     $USERID" > order.cfg
    echo "MT         $1" >> order.cfg
    echo "MODEL      $2" >> order.cfg
    echo "CUR_OPNUM  0807" >> order.cfg 
    echo "SN         $3 " >> order.cfg
    echo "ORDRNUM    $4" >> order.cfg
    echo "CTRL_NUM   DUMMY" >> order.cfg
    echo "SYSTEM     $5" >> order.cfg
    echo "STNTYP     AHECPC" >> order.cfg
    echo "DRAINSTN   $6" >> order.cfg
  
    echo "USERID|MT|MODEL|CUR_OPNUM|SN|ORDRNUM|CTRL_NUM|SYSTEM|STNTYP"
   sh_baseParamatric $USERID $1 $2 0807 $3 $4 DUMMY $5 AHECPC $6
}


sh_stepStart()
{
  stepName=${1}"XXXXXXXXX"
  stepId=$2"XXXXXXXXX"
  sDate=`date +%Y-%m-%d`
  sTime=`date +%H.%M.%S`
  stepName=${stepName:0:8}
  stepId=${stepId:0:4}
  STEPSTRING="STRT~~XXXXXXXX~~~~~~~~~~YYYYYYYYYY~~~~~~~~ZZZZZZZZ~~~~~~~~STEP~=~AAAA~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~."
  STEPSTRING=${STEPSTRING/XXXXXXXX/$stepName}
  STEPSTRING=${STEPSTRING/AAAA/$stepId}
  echo $STEPSTRING > claimedWU/endStr
  STEPSTRING=${STEPSTRING/YYYYYYYYYY/$sDate}
  STEPSTRING=${STEPSTRING/ZZZZZZZZ/$sTime}
  echo $STEPSTRING
  echo $STEPSTRING | sed -e 's/~/ /g' >> claimedWU/step.log
}

sh_stepEnd()
{
  sDate=`date +%Y-%m-%d`
  sTime=`date +%H.%M.%S`
  STEPSTRING=`cat claimedWU/endStr`
  STEPSTRING=${STEPSTRING/STRT/END~}
  STEPSTRING=${STEPSTRING/YYYYYYYYYY/$sDate}
  STEPSTRING=${STEPSTRING/ZZZZZZZZ/$sTime}
  PORF=`echo "PORF~~PASS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"`
  PORF="${PORF:0:119}."
  echo $PORF | sed -e 's/~/ /g' >> claimedWU/step.log
  echo $STEPSTRING  | sed -e 's/~/ /g' >> claimedWU/step.log
}

sh_stepData()
{
  workunit=`grep CTRL_NUM order.cfg | awk '{print $2}'`
  sn=`grep SN order.cfg | awk '{print $2}'`

 #DATA  FARM  Resource: AIX720_TEST -- Server: sgap04e0.sg.ibm.com 
  DATA=`echo "DATA~~DRNS~~$workunit-$sn|$1~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" `
  DATA="${DATA:0:119}."
  echo $DATA|sed -e 's/~/ /g' >> claimedWU/step.log 
} 

sh_setup()
{
  if [ -f "drainMFS.cfg" ]
  then
    echo "drainMFS.cfg file already exist!!!! "
    #echo -n "Are you sure you want to recreate config file [Y/N] :"
    read -p "Are you sure you want to recreate config file (y/n)? " answer
    case ${answer:0:1} in
      y|Y )
        echo Yes
      ;;
      * )
        echo No
        exit 0
      ;;
    esac
    #exit 17
  fi

  echo "Hello, this script will do one time setup for drain station using MFS."

  echo -n "Enter duluth user id and press [ENTER]: "
  read dlthUser
  echo -n "Enter duluth server name and press [ENTER]: "
  read dlthServer
  echo -n "Enter celltype and press [ENTER]: "
  read celltype
  #testcell=`hostname | awk -F. '{print $1}'`
  echo 
  echo "Duluth_User 	$dlthUser" > drainMFS.cfg
  echo "Duluth_Server	$dlthServer" >>drainMFS.cfg
  #echo "Testcell	$testcell" >>drainMFS.cfg
  echo "Celltype	$celltype" >>drainMFS.cfg
  echo
  cat drainMFS.cfg
}

sh_archiveMFSWUs ()
{
  mkdir -p archive
  mfgno=`grep SYSTEM claimedWU/teststat | awk '{print $2}' | sed -e 's/)//g'`
  rm -rf archive/$mfgno
  mv claimedWU archive/$mfgno
}
if [ "$4" == "val_user" ]; then
  sh_cleanMFSfiles
  sh_resetMFSfile
  rm -rf claimedWU
  sh_validateUser
elif [ "$4" == "claim_in" ]; then
  if [ $# -ne 5 ]; then
    echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] [val_user|suspend|claim_in <workunit>|claim_out]"
    echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] [stepStart <stepName> <stepid>|stepData <data>|stepEnd] "
    exit 1
  else
    sh_dn $5
  fi
elif [ "$4" == "suspend" ]; then
  if [ $# -ne 4 ]; then
    echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] [val_user|suspend|claim_in <workunit>|claim_out]"
    echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] [stepStart <stepName> <stepid>|stepData <data>|stepEnd] "
    exit 1
  else
    sh_suspend
  fi
elif [ "$4" == "stepStart" ]; then
  sh_stepStart $5 $6
elif [ "$4" == "stepData" ]; then
  sh_stepData $5 
elif [ "$4" == "stepEnd" ]; then
  sh_stepEnd 
elif [ "$4" == "SVTR" ]; then
  sh_svtr 
elif [ "$4" == "claim_out" ]; then
  sh_up 
  sh_archiveMFSWUs
elif [ "$4" == "cleanProcess" ]; then
  sh_cleanProcess
elif [ "$4" == "baseParamatric" ]; then
  sh_baseParamatric
elif [ "$4" == "setup" ]; then
  sh_setup
elif [ "$4" == "DUMMY" ]; then
    #USERID|MT|MODEL|CUR_OPNUM|SN|ORDRNUM|CTRL_NUM|SYSTEM|STNTYP
    #USERID|MT|MODEL|SN|ORDRNUM|CTRL_NUM|SYSTEM|DRAIN_STN
  #sh_dummyInfo $1 $2 $3 $4 $5 $6 $7 $8 $9
  #DUMMY mt model SN orderNum mfgno 
  if [ $# -ne 9 ]; then
    echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] DUMMY [MT] [MODEL] [SN] [ORDERNO] [MFGNO]"
    exit 1
  fi
  sh_dummyInfo $5 $6 $7 $8 $9 $3
elif [ "$4" == "DUMMYEND" ]; then
  cat claimedWU/step.log >> dummystep.log
else
  echo "Wrong argument usage"
  echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] [val_user|suspend|claim_in <workunit>|claim_out]"
  echo "Usage : $FILEBASENAME [duluth user] [duluth server] [testcell] [stepStart <stepName> <stepid>|stepData <data>|stepEnd] "
  exit 1
fi

exit $?
