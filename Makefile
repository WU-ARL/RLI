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
$(ACTUAL_LOCATION)/%.class: %.java
# .java.class:
	$(JC) $(JFLAGS) $<
#######################################################################################
#######################################################################################



all:	
	$(JC) -d $(CLASS_DIR) -Xlint -g @files.list ;
	chmod g+rw $(CLASS_DIR)/*.class ;
	$(JAVADIR)jar cvfm RLI.jar main.manifest IXP.hw PC1core.hw PC2core.hw PC8core1g.hw PC8core10g.hw PC48core.hw VM.hw SWR5_1.shw SWR8.shw SWR16.shw NPR.shw HOST1core.shw HOST2core.shw HOST48core.shw VMsmall.shw VM64bit.shw VM64bit_2port.shw -C $(CLASS_DIR) . ; 
	$(JAVADIR)jar cvfm RLIbeta.jar beta.manifest IXP.hw PC1core.hw PC2core.hw PC8core1g.hw PC8core10g.hw PC48core.hw VM.hw SWR5_1.shw SWR8.shw SWR16.shw NPR.shw HOST1core.shw HOST2core.shw HOST48core.shw VMsmall.shw VM64bit.shw VM64bit_2port.shw -C $(CLASS_DIR) . ; 
#	$(JAVADIR)jar cvfm RLIbatch.jar batch.manifest IXP.hw PC1core.hw PC2core.hw PC8core1g.hw PC8core10g.hw PC48core.hw VM.hw SWR5_1.shw SWR8.shw SWR16.shw NPR.shw HOST1core.shw HOST2core.shw HOST48core.shw VMsmall.shw VM64bit.shw VM64bit_2port.shw -C $(CLASS_DIR) . ; 
	chmod g+rw *jar;

pwcrypt:	
	$(JC) -d $(CLASS_DIR) -Xlint -g @pwfiles.list ;
	chmod g+rw $(CLASS_DIR)/*.class ;
	$(JAVADIR)jar cvfm PWCRYPT.jar pwcrypt.manifest -C $(CLASS_DIR) . ; 
	chmod g+rw *jar;

spp:
	$(JC) -d $(CLASS_DIR) -Xlint -g @files.list ;
	chmod g+rw $(CLASS_DIR)/*.class ;
	$(JAVADIR)jar cvfm SPPmon.jar sppmon.manifest SPPbase.hw SPP.shw -C $(CLASS_DIR) . ;

clean:
	rm -f *~;
	rm -f $(ACTUAL_LOCATION)/*.class $(ACTUAL_LOCATION)/*.jar


# dependancies follow





