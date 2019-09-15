# ChatApp

## To Compile:

From Project Directory Run:
'javac src/*.java -d class/'

To Run Server(Tested on Ubuntu 16.04):

java java -cp class: ServerChat (mode)

##To Run Client(Tested on Ubuntu 16.04):

- java -cp class: TCPClient (username)
> 'java -cp class: TCPClient User01'

- java -cp class: TCPClient (mode) (username)
> 'java -cp class: TCPClient 2 User01'

- java -cp class: TCPClient (mode) (username) (server_ip)
> 'java -cp class: TCPClient 3 User01 localhost'