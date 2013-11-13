distributed_tictactoe_JMS
=========================

Common tic tac toe game implemented using JMS system


How Install it?
-------------------------

###Requirements:

1. [java] (https://www.java.com/it/download)
2. [javac] (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
3. [jaram] (http://joram.ow2.org/download/index.html)
4. [git] (http://git-scm.com/)
5. [make] (https://www.gnu.org/software/make/)

###Steps:

1. Install java and javac for your OS.
2. Download and extract joram archive where you wnant into the filesystem.
3. Include the bin directory inside the joram extrated folder in your PATH
    `export  PATH=$PATH:/path/to/your/joram/bin/folder`
4. Install git on your system following the instruction on the website
5. Clone the repository using
    `git clone https://github.com/m4mbr3/distributed_tictactoe_JMS.git`
wherever you want on the filesystem (to decide the name of the filder add at the end of the command above your preference).
6. Open the Makefile and change the classpath of the command to point to the exact ship/bundle/joram-client-jms.jar file inside the home of joram.
7. Now run it using the command `make` inside the main folder cloned from the repository.
