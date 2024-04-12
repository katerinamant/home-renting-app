#!/bin/bash

# Configuration file path
CONFIG_FILE="app.config"
# Name of output file for ports, without specifying the directory
PORTS_FILE_NAME="ports.list"

# Generate a unique session number based on current date and time
# Format: YYYYMMDD_HHMMSS
SESSION_NUMBER=$(date +"%Y%m%d_%H%M%S")
# Define the session-specific log directory
LOG_DIR="logs/$SESSION_NUMBER"
# Ensure the log directory exists
mkdir -p $LOG_DIR

# Reading configuration
source $CONFIG_FILE

# Logging function
log() {
    echo "$(date +"%Y-%m-%d %H:%M:%S") - $1" >> $LOG_FILE
}

# ! The script is executed from the below directory
JAVA_COMMANDS_DIR="src/main/java"
cd $JAVA_COMMANDS_DIR

# Java files
BACKEND_PACKAGE="com/homerentals/backend"
PORT_MANAGER="$BACKEND_PACKAGE/PortManager"
WORKER="$BACKEND_PACKAGE/Worker"
SERVER="$BACKEND_PACKAGE/Server"
REDUCER="$BACKEND_PACKAGE/Reducer"

# Define the ports file dir
PORTS_FILE="$BACKEND_PACKAGE/$PORTS_FILE_NAME"

# Define directory for log file
LOG_FILE="../../../$LOG_DIR/startup.log"

log "Starting application setup with $WORKERS workers."

javac com/homerentals/**/*.java

# Generate reserved ports for workers
java $PORT_MANAGER $WORKERS $PORTS_FILE
if [ $? -ne 0 ]; then
    log "Failed to generate ports."
    exit 1
else
    log "Successfully generated ports."
fi

# Start workers
SUCCESS_COUNT=0
while IFS= read -r port; do
    gnome-terminal --title="Worker:$port" -- bash -c "java $WORKER $port; bash;" &
    PID=$!
    if ! kill -0 $PID 2>/dev/null; then
        log "Failed to start worker on port $port."
    else
        log "Successfully started worker on port $port."
        ((SUCCESS_COUNT++))
    fi
done < $PORTS_FILE

# Log the number of successfully started workers
log "$SUCCESS_COUNT/$WORKERS workers started successfully."

# Give some time for all workers to start
sleep 1

# Start the server and keep terminal open
gnome-terminal --title="Server" -- bash -c "java $SERVER $PORTS_FILE; bash;" &
PID=$!
if ! kill -0 $PID 2>/dev/null; then
    log "Failed to start server."
else
    log "Server started successfully."
fi

sleep 1

# Start the reducer and keep terminal open
gnome-terminal --title="Reducer" -- bash -c "java $REDUCER $WORKERS; bash;" &
PID=$!
if ! kill -0 $PID 2>/dev/null; then
    log "Failed to start reducer."
else
    log "Reducer started succesfully."
fi

log "Startup complete."
