# This makefile was shamelessly copied from the mjc compiler for MultiJava.
# It is used here to provide an infrastructure for testing the jl5 compiler.

# It is likely there is lots of extraneous stuff in here, because I don't
# profess to understand makefiles so well.  But it seems to do the trick ok.

# The important things here are the "expected" and "runtests" targets.  These
# respectively build the test programs and check whether the proper results
# are obtained.

#the top-level testing directory
TOPTESTDIR =	.

## -------------------------------------------------------------
## GOALS

clean-this:	simple-clean local-clean

local-clean:
	$(RM) *.*-ckd *.class *.java core file-list.junk *-expected.old

## -------------------------------------------------------------
## Local targets and vars

PJC = ../bin/jl5c -c -noserial -ext jl5
PJCFLAGS = 
JFILESEP=/
# list any test files that don't end in .jl5
TESTFILES =
# list any subdirectories with test files in them
SUBDIRS = 

SED = sed
RM = rm -f
DIFF = diff -w
MV = mv -f
CP = cp -f
TOUCH = touch

.SUFFIXES: .java .java-ckd .java-expected

.java.java-expected:
	-mv $@ $@.old
	$(PJC) $(PJCFLAGS) $< 2>&1 | $(PATHTOFILE) | tee $@

.java.java-ckd:
	$(PJC) $(PJCFLAGS) $< 2>&1 | $(PATHTOFILE) | tee $@

all: check

check: runtests-this-junit

runtests-this-junit: junittests 

runtests-this-nojunit: checkjava #checksh


LIST_TESTS = ls *.jl5 | grep -v -f skip

junittests-before:
	@touch skip
	@$(LIST_TESTS) >  listTestsWithOutput

checkjava:
	@touch skip
	@-for f in `$(LIST_TESTS)` ; \
	do \
	    $(MAKE) $${f}-test ; \
	done 2>&1 | tee checkall.out-ckd

checksh:
	@-for f in `ls *.sh | grep -v -f skip` ; \
	do \
	    $(MAKE) $${f}-test ; \
	done 2>&1 | tee -a checkall.out-ckd
	@echo

%.jl5-test:
	@if  [ ! "$(QUIET)" ]  ; then echo Checking $(@F:%.jl5-test=%.jl5) ... ; else printf "." ; fi
	@$(RM) $(@F:%.jl5-test=%.java) ;
	@$(PJC) $(PJCFLAGS) $(@F:%.jl5-test=%.jl5) 2>&1 \
	     | $(PATHTOFILE) | $(FIXTILT) > $(@F:%.jl5-test=%.jl5-ckd) 
	@$(DIFF) $(@F:%.jl5-test=%.jl5-expected) $(@F:%.jl5-test=%.jl5-ckd) && \
	    ( if [ ! "$(QUIET)" ]; then echo 'TYPECHECKING COMPARED AS EXPECTED!' ; fi; ) && \
	    $(RM) $(@F:%.jl5-test=%.jl5-ckd)
	@$(TOUCH) $(@F:%.jl5-test=%.java) ;
	@$(DIFF) $(@F:%.jl5-test=%.java-expected) $(@F:%.jl5-test=%.java) && \
	    ( if [ ! "$(QUIET)" ]; then echo 'CODEGEN COMPARED AS EXPECTED!' ; fi; )

%.sh-test:
	@if  [ ! "$(QUIET)" ]  ; then echo Checking $(@F:%.sh-test=%.sh) ... ; else printf "." ; fi
	@chmod +x $(@F:%.sh-test=%.sh)
	@./$(@F:%.sh-test=%.sh) 2>&1 \
	     | $(PATHTOFILE) | $(FIXTILT) > $(@F:%.sh-test=%.sh-ckd) 
	@$(DIFF) $(@F:%.sh-test=%.sh-expected) $(@F:%.sh-test=%.sh-ckd) && \
	    ( if [ ! "$(QUIET)" ]; then echo 'Compared as expected!' ; fi; ) && \
	    $(RM) $(@F:%.sh-test=%.sh-ckd)

create expected:
	-for f in `ls *.java` ; \
	do \
	  $(RM) $$f ; \
	done
	-for f in `ls *.jl5` ; \
	do \
	  echo Compiling $$f ... ; \
	  $(MV) $${f}-expected $${f}-expected.old ; \
	  $(PJC) $(PJCFLAGS) $$f 2>&1 \
	     | $(PATHTOFILE) > $${f}-expected ; \
	$(MAKE) $${f}-codegen-expected ; \
	done 2>&1 | tee create.out-ckd

%.jl5-codegen-expected:
	$(TOUCH) $(@F:%.jl5-codegen-expected=%.java) ; \
	$(CP) $(@F:%.jl5-codegen-expected=%.java) $(@F:%.jl5-codegen-expected=%.java-expected)

## ----------------------------------------------------------------------
## GLOBAL DEFINITIONS

include $(TOPTESTDIR)/Make.Defs
