SRCDIR = src
BINDIR = bin

JC = javac
JAVA = java

LIBS = algs4.jar:blockChainGrader.jar:rsa.jar:txCoinGrader.jar
CLASSPATH = $(BINDIR):$(LIBS)

JCFLAGS = -classpath $(CLASSPATH) -sourcepath $(SRCDIR) -d $(BINDIR)
JFLAGS = -classpath $(CLASSPATH)

SOURCES = $(wildcard $(addsuffix /*.java,$(SRCDIR)))
CLASSES = $(subst $(SRCDIR),$(BINDIR),$(patsubst %.java,%.class,$(SOURCES)))

all: $(BINDIR) $(CLASSES)

$(BINDIR): 
	@if [ ! -d "$(BINDIR)" ]; then mkdir -p $(BINDIR); fi

$(CLASSES): $(BINDIR)/%.class : $(SRCDIR)/%.java
	$(JC) $(JCFLAGS) $< 

clean:
	rm -rf $(BINDIR)

testTxHandler: all
	$(JAVA) $(JFLAGS) TestTxHandler

.PHONY: all clean testTxHandler
