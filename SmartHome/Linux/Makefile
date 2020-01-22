SHELL = /bin/sh
fhemDir = /opt/fhem
fhemModul = 73_SolvisClient.pm
fhemCommand = ./contrib/commandref_join.pl -noWarnings
solvisDir = /opt/SolvisSmartHomeServer
solvisJar = SolvisSmartHomeServer.jar
baseXml = base
systemD = /etc/systemd/system
service = SolvisSmartHomeServer
java = /usr/bin/java
perl = /usr/bin/perl
account = fhem
group = dialout

.SUFFIXES:


install: installFHEM installSolvis


uninstall: uninstallFHEM uninstallSolvis


installService: installSolvis
	systemctl enable $(service)
	systemctl start $(service)


uninstallService: 
	systemctl stop $(service)
	systemctl disable $(service)


installFHEM: $(fhemDir)/FHEM/$(fhemModul)


uninstallFHEM:	
	rm $(fhemDir)/FHEM/$(fhemModul)
	cd $(fhemDir);sudo -u $(account) $(perl) $(fhemCommand)


uninstallSolvis: uninstallService
	rm $(solvisDir)/$(solvisJar)
	rm $(systemD)/$(service).service


installSolvis: $(solvisDir)/$(solvisJar) $(solvisDir)/$(baseXml).xml  $(solvisDir)/$(baseXml).xsd $(systemD)/$(service).service


learn: installSolvis
	sudo -u $(account) $(java) -jar $(solvisDir)/$(solvisJar) --server-learn
	

foreground: installSolvis
	sudo -u $(account) $(java) -jar $(solvisDir)/$(solvisJar)

$(solvisDir):
	mkdir $@


$(fhemDir)/FHEM/$(fhemModul): $(fhemModul)
	cp $< $@
	chown -c $(account):$(group) $@ 
	cd $(fhemDir);sudo -u $(account) $(perl) $(fhemCommand)


$(solvisDir)/$(solvisJar): $(solvisJar) $(solvisDir)
	cp $< $@
	chown -c $(account):$(group) $@
	
	
$(solvisDir)/$(baseXml).xml : $(baseXml).xml
	cp $< $@
	chown -c $(account):$(group) $@
	chmod 600 $@
	
	
$(solvisDir)/$(baseXml).xsd : $(baseXml).xsd
	cp $< $@
	chown -c $(account):$(group) $@


$(systemD)/$(service).service: $(service).service
	cp $< $@

