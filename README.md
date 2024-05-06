# A distributed application for home renting

## Instructions

###### To compile and execute the files, make sure that you include the required .jar files under lib/ in the classpath.

###### The below instructions are provided assuming that the workers will run on the same machine

###### Set the IP addresses for the workers, server, and reducer by changing the constants on java.com.homerentals.backend.BackendUtils (default is localhost)

- <u>**For Linux and Mac OS**</u>: Simply execute our script by running `bash startup.sh`

- <u>**For Windows**</u>:
  1. `cd src/main/java`
     <br>
  2. Compile the files `javac -cp ../../../lib/commons-io-2.15.1.jar;../../../lib/json-20240303.jar;. com/homerentals/**/*.java`
     <br>
  3. Reserve worker ports:
     `java -cp ../../../lib/commons-io-2.15.1.jar;../../../lib/json-20240303.jar;. com.homerentals.backend.PortManager <NUMBER_OF_WORKERS> com/homerentals/backend/ports.list`
     <br>
  4. Start the server:
     `java -cp ../../../lib/commons-io-2.15.1.jar;../../../lib/json-20240303.jar;. com.homerentals.backend.Server com/homerentals/backend/ports.list`
     <br>
  5. Start the workers.
     Run the below for as many workers as you defined above. The port should be one of the automatically generated ports inside `src/main/java/com/homerentals/backend/ports.list`:
     `java -cp ../../../lib/commons-io-2.15.1.jar;../../../lib/json-20240303.jar;. com.homerentals.backend.Worker <PORT>`
     <br>
  6. Start the reducer instance:
     `java -cp ../../../lib/commons-io-2.15.1.jar;../../../lib/json-20240303.jar;. com.homerentals.backend.Reducer <NUMBER_OF_WORKERS>`
     <br>
  7. The backend is up and running. You can connect to it by either a dummy guest console with the credentials `guest@example.com:guest`<br>
     `java -cp ../../../lib/commons-io-2.15.1.jar;../../../lib/json-20240303.jar;. com.homerentals.backend.GuestConsole`
     <br>or as the host via the host console with the credentials `admin:admin`<br>
     `java -cp ../../../lib/commons-io-2.15.1.jar;../../../lib/json-20240303.jar;. com.homerentals.backend.`
     <br>_(credentials in `username:password` format)_.
     <br>
  8. Success! You are now connected.

## Authors

Created by [Alex Papadopoulos](https://github.com/alexisthedev) and [Katerina Mantaraki](https://github.com/katerinamant) for 🎓
