This is a program (run using runWebSocketProxy) that creates a websocket server. This server will broadcast any sent messages back to everyone (except the person who sent it).

The program also starts a webserver (at port 8888) showing all files in the current folder.

The websocket port (3037) and webserver port can be changed using the arguments.

Use the commands "exit" and "restart" to exit or restart the server. Anything else typed into the program is broadcast.

Note that the SSL code was stripped out of the library to get it to compile.