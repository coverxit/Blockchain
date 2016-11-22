SRCDIR = src
BINDIR = bin

JC = javac
JAVA = java

JFLAGS = -cp $(BINDIR) -d $(BINDIR)

SOURCES = $(wildcard $(addsuffix /*.java,$(SRCDIR)))
CLASSES = $(subst $(SRCDIR),$(BINDIR),$(patsubst %.java,%.class,$(SOURCES)))

all: $(CLASSES)

$(CLASSES): $(BINDIR)/%.class : $(SRCDIR)/%.java
	$(JC) $(JFLAGS) $< 

clean:
	rm -rf $(BINDIR)/*.class

testTxHandler: all
	$(JAVA) $(JFLAGS) TestTxHandler

.PHONY: all clean testTxHandler