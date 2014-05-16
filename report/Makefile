DOKUMENT = protokoll

all: de

clean: 
	rm -f $(DOKUMENT).tex~
	rm -f $(DOKUMENT).aux
	rm -f $(DOKUMENT).log
	rm -f $(DOKUMENT).out
	rm -f $(DOKUMENT).toc
	rm -f $(DOKUMENT).pdflatex.output

clean-all: clean
	rm -f $(DOKUMENT).pdf

de: $(DOKUMENT).pdf

$(DOKUMENT).pdf: $(DOKUMENT).tex
	pdflatex $(DOKUMENT).tex | tee $(DOKUMENT).pdflatex.output
	grep "Rerun to get cross-references right." $(DOKUMENT).pdflatex.output && pdflatex $(DOKUMENT).tex
	rm -f $(DOKUMENT).pdflatex.output

# end
