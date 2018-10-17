rm cls/connect2/*.class
javac -source 1.6 -target 1.6 -classpath src -d cls src/connect2/Main.java && java -classpath cls connect2.Main
