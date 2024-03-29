# Java Makefile

	NewSUFFIXES = $(SUFFIXES) .class .java
.SUFFIXES:	$(NewSUFFIXES)

JAVADIR=#/home/jp/jdk1.6.0_30/bin/
JC=$(JAVADIR)javac
CLASS_DIR = ./classes
SHW_DIR = ./xmlfiles/subtypes
ACTUAL_LOCATION = $(CLASS_DIR)
#DEFAULTCLASSES=:/pkg/java_1.1/lib/classes.zip:.
JFLAGS= -d $(CLASS_DIR) -Xlint -g #-Xdepend
EXTRACLASSES = $(CLASS_DIR)
COMPILE.java=$(JC) $(JFLAGS)
XML_FILES=./xmlfiles/current
$(ACTUAL_LOCATION)/%.class: %.java
# .java.class:
	$(JC) $(JFLAGS) $<
#######################################################################################
#######################################################################################



all:	
	$(JC) -d $(CLASS_DIR) -Xlint -g @files.list ;
	chmod g+rw $(CLASS_DIR)/*.class ;
	$(JAVADIR)jar cvfm RLI.jar main.manifest ${XML_FILES}/*hw  -C $(CLASS_DIR) . ; 
	#$(JAVADIR)jar cvfm RLI.jar main.manifest PC1core.hw PC2core.hw PC8core1g.hw PC8core10g.hw VM.hw SWR5_1.shw SWR8.shw SWR16.shw HostFltrs1corewLimit.shw HostFltrs2corewLimit.shw VMsmall.shw  -C $(CLASS_DIR) . ; 
	$(JAVADIR)jar cvfm RLIbeta.jar beta.manifest ${XML_FILES}/*hw -C $(CLASS_DIR) . ; 
	#$(JAVADIR)jar cvfm RLIbeta.jar beta.manifest PC1core.hw PC2core.hw PC8core1g.hw PC8core10g.hw VM.hw SWR5_1.shw SWR8.shw SWR16.shw HostFltrs1corewLimit.shw HostFltrs2corewLimit.shw VMsmall.shw -C $(CLASS_DIR) . ; 
#	$(JAVADIR)jar cvfm RLIbatch.jar batch.manifest IXP.hw PC1core.hw PC2core.hw PC8core1g.hw PC8core10g.hw PC48core.hw VM.hw SWR5_1.shw SWR8.shw SWR16.shw NPR.shw HOST1core.shw HOST2core.shw HOST48core.shw VMsmall.shw VM64bit.shw VM64bit_2port.shw -C $(CLASS_DIR) . ; 
	chmod g+rw *jar;

pwcrypt:	
	$(JC) -d $(CLASS_DIR) -Xlint -g @pwfiles.list ;
	chmod g+rw $(CLASS_DIR)/*.class ;
	$(JAVADIR)jar cvfm PWCRYPT.jar pwcrypt.manifest -C $(CLASS_DIR) . ; 
	chmod g+rw *jar;

clean:
	rm -f *~;
	rm -f $(ACTUAL_LOCATION)/*.class $(ACTUAL_LOCATION)/*.jar


# dependancies follow





