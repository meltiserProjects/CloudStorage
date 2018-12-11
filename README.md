# CloudStorage

In this cloud storage users can store their files on server and have access from any computer via client app. Based on JavaFX, Netty and PostgreSQL (connected through JDBC). What's been realized:
- Sending and downloading files with any size (big files are sending by dividing into parts);
- Managing files (renaming, deleting, seeing information about file);
- GUI on client app: 
	- copy your files to special folder or use drag'n'drop for adding files,
	- sort files using subfolders,
	- client folder updates automatically (using Java Wath Service),
	- open files directly from app;
- Signing in and signing up. Every user has his own folder named by login (information about users is stored in DB).
