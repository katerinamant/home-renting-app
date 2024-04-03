#!/bin/bash

# Configuration file path
CONFIG_FILE="app.config"
# Output file for ports, without specifying the directory here
PORTS_FILE_PREFIX="ports.list"

# Generate a unique session number based on current date and time
# Format: YYYYMMDD_HHMMSS
SESSION_NUMBER=$(date +"%Y%m%d_%H%M%S")
# Define the session-specific log directory
LOG_DIR="./logs/$SESSION_NUMBER"
# Ensure the log directory exists
mkdir -p $LOG_DIR

# Update PORTS_FILE to include the session log directory
PORTS_FILE="$LOG_DIR/$PORTS_FILE_PREFIX"
# Log file path, now located in the session-specific log directory
LOG_FILE="$LOG_DIR/startup.log"

# Reading configuration
source $CONFIG_FILE

# Logging function
log() {
    echo "$(date +"%Y-%m-%d %H:%M:%S") - $1" >> $LOG_FILE
}

# Initial log entry
log "Starting application setup with $WORKERS workers."

# Generate ports and write them to PORTS_FILE, also pass log directory
java PortManager $WORKERS $PORTS_FILE $LOG_DIR
if [ $? -ne 0 ]; then
    log "Failed to generate ports."
    exit 1
else
    log "Successfully generated ports."
fi

# Counter for successfully started workers
SUCCESS_COUNT=0

# Start workers without waiting for them to exit, pass the log directory
while IFS= read -r port; do
    java Worker $ADDRESS $port $LOG_DIR &
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

# Start the server without waiting for it to exit, pass the log directory
java Server $PORTS_FILE $LOG_DIR &
PID=$!
if ! kill -0 $PID 2>/dev/null; then
    log "Failed to start server."
else
    log "Server started successfully."
fi
