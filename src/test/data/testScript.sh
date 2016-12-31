#!/bin/bash

LOG_FILE=/data/logfile
touch /data/logfile
echo "" > $LOG_FILE
cd /data
echo "This is a test script with parameter $1" >> $LOG_FILE
touch new_file
echo "Created a new file" >> $LOG_FILE
