SHELL = /bin/sh
fhemDir = /opt/fhem
fhemModul = 73_SolvisClient.pm
fhemCommand = ./contrib/commandref_join.pl -noWarnings
serviceFhem = fhem.service
fhemRestart = 0
solvisDir = /opt/SolvisSmartHomeServer
solvisJar = SolvisSmartHomeServer.jar
baseXml = base
systemD = /etc/systemd/system
serviceDebug = DebugSolvisSmartHomeServer
service = SolvisSmartHomeServer
java = /usr/bin/java
javaDebugPort = 0.0.0.0:10736
javaAgentlib = jdwp=transport=dt_socket,suspend=n,server=y,address=$(javaDebugPort)
perl = /usr/bin/perl
account = fhem
group = dialout
start = sudo -u $(account) $(java) -jar $(solvisDir)/$(solvisJar)
startDebug = sudo -u $(account) $(java) -agentlib:$(javaAgentlib) -jar $(solvisDir)/$(solvisJar)
startLocal = sudo -u $(account) $(java) -jar $(solvisJar)

.SUFFIXES:


install: installFHEM installSolvis


uninstall: uninstallFHEM uninstallSolvis


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


installFHEM: $(fhemDir)/FHEM/$(fhemModul)


uninstallFHEM:	
	rm $(fhemDir)/FHEM/$(fhemModul)
	cd $(fhemDir);sudo -u $(account) $(perl) $(fhemCommand)


uninstallSolvis: uninstallService uninstallDebugService
	rm $(solvisDir)/$(solvisJar)
	rm $(systemD)/$(service).service
	rm $(systemD)/$(serviceDebug).service
	

installSolvis: $(solvisDir)/$(solvisJar) $(solvisDir)/$(baseXml).xml  $(solvisDir)/$(baseXml).xsd copyService


update: stopServices install
	systemctl start $(service)


updateDebug: stopServices install
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
	$(start) --server-learn
	systemctl start $(serviceDebug)


debugServer: stopServices installSolvis
	$(startDebug)
	

terminate: stopServices installSolvis
	$(start) --server-terminate


foreground: stopServices installSolvis
	sudo -u $(account) $(java) -jar $(solvisDir)/$(solvisJar)




$(solvisDir):
	mkdir $@


$(fhemDir)/FHEM/$(fhemModul): $(fhemModul)
	cp $< $@
	chown -c $(account):$(group) $@ 
	cd $(fhemDir);sudo -u $(account) $(perl) $(fhemCommand)
 	ifeq ($(fhemRestart),1)
		systemctl restart $(serviceFhem)
 	endif



$(solvisDir)/$(solvisJar): $(solvisJar) $(solvisDir)
	cp $< $@
	chown -c $(account):$(group) $@
	
	
$(solvisDir)/$(baseXml).xml : $(baseXml).xml
	mv $@ $<.old
	chown -c `ls -l $< | awk '{ print $$3":"$$4 }'` $<.old 
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
