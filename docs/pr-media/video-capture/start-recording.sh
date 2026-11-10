#!/system/bin/sh
case "$1" in 429|430|431) ;; *) exit 2 ;; esac
screenrecord --size 1280x800 --bit-rate 3000000 --time-limit 60 "/data/local/tmp/xnotes-pr-video-$1.mp4" >"/data/local/tmp/xnotes-pr-video-$1.log" 2>&1 &
echo $!
