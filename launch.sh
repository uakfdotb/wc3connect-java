rm cls/connect2/*.class
javac -source 1.8 -target 1.8 -classpath src -d cls src/connect2/Main.java && java -classpath cls connect2.Main
