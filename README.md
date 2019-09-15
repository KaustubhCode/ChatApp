# ChatApp

Chat application supporting encrypted messaging with signature verification.
It supports the following modes.

- `mode = 1` No Encrytion
- `mode = 2` End-to-end Encryption
- `mode = 3` End-to-end Encryption & Message Signatures

### Compile Instructions:

From Project Directory Run:
> javac src/*.java -d class/

### To Run Server:

Open a new terminal and run the following command from project directory (Tested on Ubuntu 16.04): `java java -cp class: ServerChat (mode)`

> java java -cp class: ServerChat 1

Alternatively, go to class directory in project folder and run the following

> java ServerChat 3

### To Run Client:

For each client open a new terminal and run one of the following from project directory (Tested on Ubuntu 16.04)

- For running on local with default mode(no encryption): `java -cp class: TCPClient (username)`
> java -cp class: TCPClient User01

- For running on local with different modes:  `java -cp class: TCPClient (mode) (username)`
> java -cp class: TCPClient 2 User01

- For connecting to server on known IP with different modes:  `java -cp class: TCPClient (mode) (username) (server_ip)`
> java -cp class: TCPClient 3 User01 localhost

Alternatively, go to class directory in project folder and run the following

> java TCPClient 3 User01 localhost