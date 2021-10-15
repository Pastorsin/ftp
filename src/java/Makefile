MVN=mvn
FTP=ftp
GREETER=greeter

.PHONY: ftp greeter clean

ftp:
	$(FTP)/run.sh $(MVN)

greeter:
	$(GREETER)/run.sh $(MVN)

clean:
	cd $(GREETER) && $(MVN) clean:clean
	cd $(FTP) && $(MVN) clean:clean