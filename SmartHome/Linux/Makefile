SHELL = /bin/sh
fhemDir = /opt/fhem
fhemModul = 73_SolvisClient.pm
packageFhemDir = FHEM
fhemCommand = ./contrib/commandref_join.pl -noWarnings
fhemCommandRef = $(fhemDir)/docs/commandref.html
serviceFhem = fhem.service
fhemRestart = 0
solvisDir = /opt/SolvisSmartHomeServer
solvisJar = SolvisSmartHomeServer.jar
baseXml = base
systemD = /etc/systemd/system
serviceDebug = DebugSolvisSmartHomeServer
service = SolvisSmartHomeServer
java = /usr/bin/java
javaDebugPort = 0.0.0.0:10737
javaAgentlib = jdwp=transport=dt_socket,suspend=n,server=y,address=$(javaDebugPort)
perl = /usr/bin/perl
account = fhem
group = dialout
start = sudo -u $(account) $(java) -jar $(solvisDir)/$(solvisJar)
startDebug = sudo -u $(account) $(java) -agentlib:$(javaAgentlib) -jar $(solvisDir)/$(solvisJar)
startLocal = sudo -u $(account) $(java) -jar $(solvisJar)
writablePath = `sed -n 's/.*\?writablePathLinux="\([^"]*\)".*/\1/gp' base.xml`
generatedBaseDir = SolvisServerData
generatedFiles = control.xml* control.xsd graficData.xml graficData.xsd  measurements.xml   measurements.xsd solvis-error.log* solvis.log* solvis-tiny.log* tinylog.properties log4j2.xml
generatedDirs = SolvisErrorImages LearnedImages

.SUFFIXES:

eraseGenerated:
	cd "$(writablePath)/$(generatedBaseDir)" ; \
	for file in $(generatedFiles) ; do \
		rm $$file ; \
	done ; \
	for dir in $(generatedDirs) ; do \
		rm -r $$dir ; \
	done ; \
	if [ -z "$$(ls -A .)" ]; then \
		cd - ; \
		rm -r "$(writablePath)/$(generatedBaseDir)" ; \
	fi

install: installSolvis installFHEM



uninstall: uninstallSolvis uninstallFHEM



uninstallAll: uninstallSolvis eraseGenerated uninstallFHEM



copyService: $(systemD)/$(service).service $(systemD)/$(serviceDebug).service



installService: installSolvis
	systemctl enable $(service)
	systemctl start $(service)



installDebugService: installSolvis
	systemctl enable $(serviceDebug)
	systemctl start $(serviceDebug)



stopServices:
	-systemctl stop $(service)
	-systemctl stop $(serviceDebug)



uninstallService: stopServices
	systemctl disable $(service)



uninstallDebugService: stopServices
	systemctl disable $(serviceDebug)



installFHEM: $(fhemCommandRef) 



uninstallFHEM: rmFHEMModul createCommandRef	



rmFHEMModul:
	rm $(fhemDir)/FHEM/$(fhemModul)



uninstallSolvis: uninstallService uninstallDebugService
	rm $(solvisDir)/$(solvisJar)
	rm $(systemD)/$(service).service
	rm $(systemD)/$(serviceDebug).service



installSolvis: $(solvisDir)/$(solvisJar) $(solvisDir)/$(baseXml).xml  $(solvisDir)/$(baseXml).xsd copyService



update: stopServices install
	systemctl start $(service)



updateSolvis: stopServices installSolvis
	systemctl start $(service)



updateDebug: stopServices installSolvis
	systemctl start $(serviceDebug)


learn: stopServices installSolvis
	$(start) --server-learn
	systemctl start $(service)



testmail:
	$(startLocal) --test-mail



crypt:
	@read -p "Enter the word to be encrypted:" password; \
	$(startLocal) --string-to-crypt=$$password



learnDebug: stopServices installSolvis
	$(startDebug) --server-learn
	systemctl start $(serviceDebug)



documentation:
	$(start) --documentation --csvSemicolon



iobroker:
	$(start) --iobroker



debugServer: stopServices installSolvis
	$(startDebug)



terminate: stopServices installSolvis
	$(start) --server-terminate



foreground: stopServices installSolvis
	sudo -u $(account) $(java) -jar $(solvisDir)/$(solvisJar)



$(solvisDir):
	mkdir $@



$(fhemCommandRef):$(fhemDir)/FHEM/$(fhemModul)
	make createCommandRef



$(fhemDir)/FHEM/$(fhemModul): $(packageFhemDir)/$(fhemModul)
	cp $< $@
	chown -c $(account):$(group) $@



createCommandRef:
	cd $(fhemDir); sudo -u $(account) $(perl) $(fhemCommand)
	@echo
	@echo "************************************"
	@echo "**                                **"
	@echo "**     Bitte FHEM neu starten     **"
	@echo "**                                **"
	@echo "** z.B. mittels                   **"
	@echo "**   sudo systemctl restart fhem  **"
	@echo "**                                **"
	@echo "************************************"



$(solvisDir)/$(solvisJar): $(solvisJar) $(solvisDir)
	cp $< $@
	chown -c $(account):$(group) $@



$(solvisDir)/$(baseXml).xml : $(baseXml).xml
	if [ -f "$@" ] ; then \
		mv $@ $<.old ; \
		chown -c `ls -l $< | awk '{ print $$3":"$$4 }'` $<.old ; \
	fi 
	cp $< $@
	chown -c $(account):$(group) $@
	chmod 600 $@



$(solvisDir)/$(baseXml).xsd : $(baseXml).xsd
	cp $< $@
	chown -c $(account):$(group) $@



$(systemD)/$(service).service: $(service).service
	cp $< $@
	systemctl daemon-reload



$(systemD)/$(serviceDebug).service: $(serviceDebug).service
	cp $< $@
	systemctl daemon-reload
