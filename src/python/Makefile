VENV=$(CURDIR)/venv
PYTHON=$(VENV)/bin/python
PIP=$(VENV)/bin/pip

FTP=ftp
GREETER=greeter

.PHONY: build test install clean ftp greeter

$(VENV): requirements.txt
	python3 -m venv $(VENV)
	$(PIP) install --upgrade pip
	$(PIP) install -r requirements.txt

install: $(VENV)
	chmod +x $(FTP)/run.sh
	chmod +x $(GREETER)/run.sh

build: install
	$(PYTHON) -m grpc_tools.protoc \
		-I$(GREETER) \
		--python_out=$(GREETER) \
		--grpc_python_out=$(GREETER) \
		$(GREETER)/greeter.proto
	
	$(PYTHON) -m grpc_tools.protoc \
		-I$(FTP) \
		--python_out=$(FTP) \
		--grpc_python_out=$(FTP) \
		$(FTP)/ftp.proto

ftp: build
	$(FTP)/run.sh $(PYTHON)

greeter: build
	$(GREETER)/run.sh $(PYTHON)

clean: clean-venv clean-proto clean-cache

clean-venv:
	rm -rf $(VENV)

clean-proto:
	find . -name *pb2.py -delete
	find . -name *pb2_grpc.py -delete

clean-cache:
	find . -name *.pyc -delete
	find . -name __pycache__ -delete
