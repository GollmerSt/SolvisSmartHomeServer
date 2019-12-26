########################################################################################################################
#
# Attention: it's not the FHEM archive, it's a private one
# $Id$
#
#                                          CHANGELOG
#
#	Version		Date		Programmer			Subroutine						Description of Change
#	00.01.00	17.12.2019	SCMP77				All								Initial Release



#####################################
# Offene Aufgaben:
# 	1. Unterdruecken der Readings aus Solvis-Daten, welche zeigen, dass Heizkreise o.ae. nicht existieren
#         In diesem fall gibt es Kanal-abhängige Werte wie 2500 oder 00. Muss im XML vermutlich Kanal-
#         weise definiert werden
#
#	2. Der Solvis-Name muss noch definierbar sein
#
#	3. Das Zurückgehen auf den letzten Userbildschirm inhibieren. Muss beim Schließen der Verbindung
#         zurückgesetzt werden
#
#	4. Events wie Poweron o.ä. müssen noch realisiert werden
#

#####################################
# Bekannte Probleme


##############################################

 
package main;

use strict;
use warnings;
use POSIX;
use JSON;

use constant MODULE => "SolvisClient";

use constant WATCH_DOG_INTERVAL => 300 ;	#Mindestens alle 5 Minuten muss der Client Daten vom Servere erhalten haben.
											# Andernfalls wird die Verbindung neu aufgebaut
use constant RECONNECT_AFTER_DISMISS => 5 ;

use constant _FALSE_ => 0 ;
use constant _TRUE_ => 1;

use constant CLIENT_VERSION => "00.01.00" ;
use constant FORMAT_VERSION_REGEXP => "m/01\.../" ;

my %SolvisClient_ChannelDescriptions ;



#####################################
#
#      Log
#

sub SolvisClient_Log($$$) {
	my ( $this, $loglevel, $text ) = @_;
	my $xline       = ( caller(0) )[2];   
	my $xsubroutine = ( caller(1) )[3];
	my $sub         = ( split( ':', $xsubroutine ) )[2];
	$sub =~ s/SolvisClient_//;

	my $instName = ( ref($this) eq "HASH" ) ? $this->{NAME} : $this;
	Log3 $this, $loglevel, MODULE." $instName: $sub.$xline " . $text;
} # end SolvisClient_Log



#####################################
#
#      Initialize
#

sub SolvisClient_Initialize($) {
	my ($this) = @_;
	$this->{DefFn}			= "SolvisClient_Define";
	$this->{UndefFn}		= "SolvisClient_Undef";
	$this->{DeleteFn}		= "SolvisClient_Delete";
	$this->{ReadFn}         = "SolvisClient_Read";
	$this->{ReadyFn}        = "SolvisClient_Ready";
	$this->{ShutdownFn}		= "SolvisClient_Undef";
	$this->{SetFn}			= "SolvisClient_Set";
	$this->{GetFn}			= "SolvisClient_Get";
	$this->{NotifyFn}       = "SolvisClient_Notify";
#	$this->{AttrFn}			= "SolvisClient_Attr";
	$this->{AttrList}		=
							  "SolvisName ".
#							  "Average ".
#							  "Interval ".
#							  "PowerOn ".
#							  "FirmwareLth2.21.02A ".
							  $readingFnAttributes;
	$this->{DbLog_splitFn}	= "SolvisClient_DbLog_splitFn";

	$this->{VERSION} = CLIENT_VERSION ;

} # end SolvisClient_Initialize



#####################################
#
#      Define
#

sub SolvisClient_Define($$) {  #define heizung SolvisClient 192.168.1.40 SGollmer e$am1kro

	my ( $this, $def ) = @_;
	my @args = split( "[ \t][ \t]*", $def );
	my $url  = $args[2];
	my $name = $this->{NAME};
	
	$this->{DeviceName}  = $url;	#Für DevIO, Name fest vorgegeben
	
	if( $init_done ) {
		my $result = SolvisClient_Connect( $this, 0 );
	} else {
		$this->{NOTIFYDEV} = "global";
	}
		
	return undef;
} # end SolvisClient_Define



sub SolvisClient_Notify($$)
{
	my ($this, $eventObject) = @_;
	my $ownName = $this->{NAME}; # own name
	
	if ( IsDisabled($ownName)) {
		return undef ;	# Return without any further action if the module is disabled
	}
  
	my $devName = $eventObject->{NAME}; # Device that created the events
	my $events = deviceEvents($eventObject, 1);

	if($devName eq "global" && grep(m/^INITIALIZED|REREADCFG$/, @{$events}))
	{
		 my $result = SolvisClient_Connect( $this, 0 );
		$this->{NOTIFYDEV} = "";
	}
}



#########################################
#
# Create Send data
#

sub SolvisClient_CreateSendData($$) {

	my($this, $sendData ) = @_ ;
	
	my $byteString = encode_json ( $sendData );
	
	my $length = length $byteString ;
	$byteString = pack("CCCa*", $length>>16&0xff, $length>>8&0xff, $length&0xff, $byteString ) ;
	
	SolvisClient_Log $this, 5, "ByteString: $byteString";

	return $byteString ;
	
} # end SolvisClient_CreateSendData


#####################################
#
#     Connect to Server
#

sub SolvisClient_Connect($$) {
	my ($this, $reopen) = @_;
	
	my $connectedSub = \&SolvisClient_SendConnectionData ;
	
	if ( $reopen ) {
	
		$connectedSub = \&SolvisClient_SendReconnectionData ;

	}
	
	my $error = DevIo_OpenDev($this, $reopen, $connectedSub);
	
	$this->{helper}{BUFFER} = "" ;

} # end SolvisClient_Connect



####################################
#
#   Send connection data
#

sub SolvisClient_SendConnectionData($) {
	
	my( $this) = @_ ;
	
	$this->{CLIENT_ID} = undef ;
	
	my %sendConnectionInfo = (
		CONNECT => {
			#Url => 'http://192.168.1.40',
			#Account => 'Gollmer',
			#Password => 'e5am1kro',
			Id => AttrVal( $this->{NAME}, "SolvisName", $this->{NAME} )
		}
	) ;
	
	DevIo_SimpleWrite($this, SolvisClient_CreateSendData($this, \%sendConnectionInfo ), 0);

} # end SolvisClient_SendConnectionData



################################################
#
# Send reconnection data
#

sub SolvisClient_SendReconnectionData($) {

	my ($this) = @_;
	
	if ( defined( $this->{CLIENT_ID} ) ) {

		my %sendReconnectionInfo = (
			RECONNECT => {
				Id => $this->{CLIENT_ID}
			}
		) ;
		
		DevIo_SimpleWrite($this, SolvisClient_CreateSendData($this, \%sendReconnectionInfo ), 0);
		
	} else {
		 SolvisClient_SendConnectionData($this) ;
	}
}



################################################
#
# Ein Reconnect wird versucht
#

sub SolvisClient_Ready($) {
	my ($this) = @_;

    if($this->{STATE} eq "disconnected") {
		SolvisClient_Log $this, 4, "Reconnection try";
		SolvisClient_Connect($this, 1) ; # reopen
	}
	
	
}



################################################
#
# Daten vom Server erhalten
#

sub SolvisClient_Read($) {

	my ($this) = @_;
	my $name = $this->{NAME};
	
	RemoveInternalTimer($this, "SolvisClient_Reconnect" );
	my $timeStamp = gettimeofday() + WATCH_DOG_INTERVAL ;
	InternalTimer($timeStamp, "SolvisClient_Reconnect", $this );
	
	
	SolvisClient_Log $this, 5, "SolvisClient_Read entered";

	# einlesen der bereitstehenden Daten
	my $buf = DevIo_SimpleRead($this);

	if ( ! defined( $buf ) ) {
		return undef ;
	}
		
	$this->{helper}{BUFFER} .= $buf;

	#SolvisClient_Log $this, 3, "Current buffer content: ".$this->{helper}{BUFFER};
	
	while ( length($this->{helper}{BUFFER}) >= 3 ) {
	
		my $bufferLength = length($this->{helper}{BUFFER}) ;
		
		my @parts = unpack("CCC", $this->{helper}{BUFFER} ) ;
		my $length = $parts[2] | $parts[1] << 8 | $parts[0] << 16 ;

		SolvisClient_Log $this, 5, "Length of package: ".$length;

		if ( $length > $bufferLength - 3 ) {
			return undef ;
		}
		
		@parts = unpack("CCCa".$length."a*", $this->{helper}{BUFFER} ) ;
		$this->{helper}{BUFFER} = $parts[4] ;

		SolvisClient_Log $this, 5, "Package encoded: ".$parts[3];

		my $receivedData = decode_json ($parts[3]);
		
		SolvisClient_executeCommand($this, $receivedData) ;
	}
}

#####################################
#
#	Execute server commands
#
sub SolvisClient_executeCommand($$) {

	my ($this, $receivedData ) = @_;
	
	my @key = keys %$receivedData ;

	my $command = $key[0] ;

	SolvisClient_Log $this, 4, "Command detected: ".$command;
	
	if ( $command eq "CONNECTED" ) {
		$this->{CLIENT_ID} = $receivedData->{CONNECTED}->{ClientId} ;
		if ( defined ($receivedData->{CONNECTED}->{ServerVersion}) ) {
			$this->{SERVER_VERSION} = $receivedData->{CONNECTED}->{ServerVersion} ;
			my $formatVersion = $receivedData->{CONNECTED}->{FormatVersion} ;
			if ( ! $formatVersion =~ FORMAT_VERSION_REGEXP ) {
				SolvisClient_Log $this, 3, "Format version $formatVersion of client is depricated, use a newer client, if available." ;
				$this->{INFO} = "Format version is depricated" ;
			}
			Log3 $this, 3, MODULE.", Server version: $this->{SERVER_VERSION}" ;
		}
	} elsif ( $command eq "MEASUREMENTS" ) {
		SolvisClient_UpdateReadings($this, $receivedData->{MEASUREMENTS}) ;
	} elsif ( $command eq "CHANNEL_DESCRIPTIONS" ) {
		SolvisClient_CreateGetSetCommands($this, $receivedData->{CHANNEL_DESCRIPTIONS}) ;
	} elsif ($command eq "CONNECTION_STATE" ) {
		SolvisClient_InterpreteConnectionState($this, $receivedData->{CONNECTION_STATE}) ;
	} elsif ($command eq "SOLVIS_STATE" ) {
		SolvisClient_InterpreteSolvisState($this, $receivedData->{SOLVIS_STATE}) ;
	}
}

##########################################
#
#     Interprete connection status

sub SolvisClient_InterpreteConnectionState($$) {

	my ($this, $state ) = @_;
	
	my @keys = keys(%$state) ;
	
	my $stateString  ;
	my $message  ;

	foreach my $key( keys(%$state)) {
		if ( $key eq "State") {
			$stateString = $state->{$key } ;
		} elsif ( $key eq "Message") {
			$message = $state->{$key } ;
		}
	}
	
	
	SolvisClient_Log $this, 3, "Connection status: $stateString";
	
	
	if ( $stateString eq "CLIENT_UNKNOWN") {
		$this->{CLIENT_ID} = undef ;
		SolvisClient_Log $this, 3, "Client unknown: $message";
		SolvisClient_ReconnectAfterDismiss($this);
	} elsif ( $stateString eq "CONNECTION_NOT_POSSIBLE") {
		SolvisClient_Log $this, 3, "Connection not possible: $message";
		SolvisClient_ReconnectAfterDismiss($this);
	} elsif ( $stateString eq "CONNECTED") {
		# TODO
	} elsif ( $stateString eq "DISCONNECTED") {
		# TODO
	} elsif ( $stateString eq "ALLIVE" ) {
		SolvisClient_Log $this, 3, "Alive received";
	}
	return undef ;
}

sub SolvisClient_ReconnectAfterDismiss($) {
	my ($this) = @_ ;

	my $timeStamp = gettimeofday() + RECONNECT_AFTER_DISMISS ;
	InternalTimer($timeStamp, "SolvisClient_Reconnect", $this );

	return undef ;
}

##########################################
#
#     Interprete solvis state

sub SolvisClient_InterpreteSolvisState($$) {

	my ($this, $state ) = @_;
	
	my @keys = keys(%$state) ;
	
	my $stateString  ;
	my $message  ;

	foreach my $key( keys(%$state)) {
		if ( $key eq "SolvisState") {
			$stateString = $state->{$key } ;
		}
	}
	
	SolvisClient_Log $this, 3, "Solvis status: $stateString";
	
	readingsSingleUpdate($this,"state",$stateString,1);
	
	#DoTrigger($this->{NAME}, $stateString);	
}



################################################
#
# Reconnect
#

sub SolvisClient_Reconnect($) {

	my ($this) = @_;
	
	SolvisClient_Log $this, 3, "Retry reconnection";
	DevIo_CloseDev($this) ;
	SolvisClient_Connect($this,0);
	return undef ;
}




#######################################
#    Create data for Set and Get commands

sub SolvisClient_CreateGetSetCommands($$) {

	my ($this, $descriptions ) = @_;
	
	%SolvisClient_ChannelDescriptions = ();

	foreach my $description( keys(%$descriptions)) {
		my %descriptionHash = %$descriptions{$description} ;
		my @keys = keys %descriptionHash ;
		my $channel = $keys[0] ;
		SolvisClient_Log $this, 5, "Processing of channel description: ".$channel;
		my %channelHash = %{$descriptionHash{$channel}} ;
		$SolvisClient_ChannelDescriptions{$channel} = {} ;
		$SolvisClient_ChannelDescriptions{$channel}{SET} = $channelHash{Writeable} ;
		$SolvisClient_ChannelDescriptions{$channel}{GET} = $channelHash{Type} eq "CONTROL" ;
		SolvisClient_Log $this, 5, "Writeable: ".$channelHash{Writeable};
		foreach my $keyName ( keys %channelHash ) {
			if ( $keyName eq "Accuracy") {
				$SolvisClient_ChannelDescriptions{$channel}{Accuracy} = $channelHash{Accuracy} ;
			} elsif  ( $keyName eq "Modes") {
				$SolvisClient_ChannelDescriptions{$channel}{Modes} = {} ;
				foreach my $mode (@{$channelHash{Modes}}) {
					$SolvisClient_ChannelDescriptions{$channel}{Modes}{$mode} = 1 ;
				}
			} elsif ( $keyName eq "Upper") {
				$SolvisClient_ChannelDescriptions{$channel}{Upper} = $channelHash{Upper} ; ;
			} elsif ( $keyName eq "Lower") {
				$SolvisClient_ChannelDescriptions{$channel}{Lower} = $channelHash{Lower} ; ;
			} elsif ( $keyName eq "Step") {
				$SolvisClient_ChannelDescriptions{$channel}{Step} = $channelHash{Step} ; ;
			} elsif ($keyName eq "IsBoolean") {
				$SolvisClient_ChannelDescriptions{$channel}{IsBoolean} = $channelHash{IsBoolean} ;
			} elsif ($keyName eq "Unit" ) {
				$SolvisClient_ChannelDescriptions{$channel}{Unit} = $channelHash{Unit} ;
			}
		}
	}
	
}



######################################
#    Update Readings

sub SolvisClient_UpdateReadings($$) {
	my ($this, $readings ) = @_;
	
	readingsBeginUpdate($this);
	
	foreach my $readingName( keys(%$readings)) {
		my $value = $readings->{$readingName} ;
		if ( $SolvisClient_ChannelDescriptions{$readingName}{IsBoolean} != _FALSE_ ) {
			$value = $value?"on":"off";
		}
		readingsBulkUpdate($this,$readingName,$value);
	}

	readingsEndUpdate($this, 1);
}


######################################
#      Undefine
sub SolvisClient_Undef($$) {
	my ($this, $args) = @_;
	
	DevIo_CloseDev($this) ;

	return undef ;
} # end SolvisClient_Undef

#####################################
#      Delete module
sub SolvisClient_Delete($$) {
	my ($this, $args) = @_;
	
	my $prefix = $this->{TYPE}."_".$this->{NAME}."_" ;

#	foreach my $keyName( keys(%SolvisClient_Secure)) {
#		my $index = $prefix.$keyName;
#		setKeyValue($index, undef );
#		$SolvisClient_Secure{$this->{NAME}}{$keyName} = "" ;
#	}
	return undef ;
}# end SolvisClient_Delete

#####################################
#   send set data


sub SolvisClient_SendData($$) {
	
	my( $this, $sendPackage ) = @_ ;
	
	my $byteString = encode_json ( $sendPackage );
	
	my $length = length $byteString ;
	$byteString = pack("CCCa*", $length&0xff, $length>>8&0xff, $length>>16&0xff, $byteString ) ;
	
	SolvisClient_Log $this, 5, "ByteString: $byteString";

	DevIo_SimpleWrite($this, $byteString, 0);
}



sub SolvisClient_SendSetData($$$) {
	
	my( $this, $channel, $data ) = @_ ;
	
	my %SetPackage = (
		SET => {
			$channel => $data
		}
	) ;
	
	SolvisClient_SendData($this, \%SetPackage);
}

sub SolvisClient_SendGetData($$) {
	my($this, $channel) = @_ ;
	my %GetPackage = (
		GET => {
			$channel => undef
		}
	) ;
	
	SolvisClient_SendData($this, \%GetPackage);
}


#####################################
#
#      Set command
#
sub SolvisClient_Set($@) {
	my ( $this, @a ) = @_;
#	

	if ( $a[1] eq '?' ) {
		my @channels = keys(%SolvisClient_ChannelDescriptions) ;
		my $params = "" ;
		my $firstO = _TRUE_ ;
		foreach my $channel (@channels) {
			if ( $SolvisClient_ChannelDescriptions{$channel}{SET} == _FALSE_ ) {
				next ;
			}
			if ( ! $firstO ) {
				$params .= ' ' ;
			} else {
				$firstO = _FALSE_ ;
			}
			$params .= $channel ;
			my $firstI = _TRUE_ ;
			if ( defined ($SolvisClient_ChannelDescriptions{$channel}{Modes}) ) {
				foreach my $mode (keys(%{$SolvisClient_ChannelDescriptions{$channel}{Modes}})) {
					if($firstI) {
						$params .= ':' ;
						$firstI = _FALSE_ ;
					} else {
						$params .= ','
					}
					$params .=$mode ;
				}
			} elsif ( defined ($SolvisClient_ChannelDescriptions{$channel}{Upper}) ) {
				for ( my $count = $SolvisClient_ChannelDescriptions{$channel}{Lower} ; $count <= $SolvisClient_ChannelDescriptions{$channel}{Upper} ; $count += $SolvisClient_ChannelDescriptions{$channel}{Step}) {
					if($firstI) {
						$params .= ':' ;
						$firstI = _FALSE_ ;
					} else {
						$params .= ','
					}
					$params .=$count ;
				}
			}
		}
		return "unknown argument $a[1] choose one of " .$params. " ServerCommand:BACKUP,SCREEN_RESTORE_INHIBIT,SCREEN_RESTORE_ENABLE";
	}

	### If not enough arguments have been provided
	if ( @a < 2 ) {
		return "\"set $this->{NAME}\" needs at least one argument";
	}
	
	my $command = $a[1] ;
	
	if ( $command eq "ServerCommand") {
	
		my ($device, $serverCommand, $command) = @a;

		
		SolvisClient_SendServerCommand($this, $command) ;

	} else {

	
		my ($device, $channel, $value) = @a;
		
		SolvisClient_Log $this, 4, "Set entered, device := $device, Cannel := $channel, Value := $value";
		
		if ( defined($SolvisClient_ChannelDescriptions{$channel}) ) {
			if ( defined ($SolvisClient_ChannelDescriptions{$channel}{Modes}) ) {
				if ( ! defined ($SolvisClient_ChannelDescriptions{$channel}{Modes}{$value})) {
					my @modes = keys(%{$SolvisClient_ChannelDescriptions{$channel}{Modes}}) ;
			SolvisClient_Log $this, 5, "Mode 1: ".join(" ", $modes[0]);
					return "unknown value $value choose one of " . join(" ", @modes);
				}
			} else {
				$value = int($value) ;
			}
			SolvisClient_SendSetData($this, $channel, $value) ;
		} else {
			my @channels = keys(%SolvisClient_ChannelDescriptions) ;
			SolvisClient_Log $this, 5, "Channels: ".join(" ", @channels);
			return "unknown argument $channel choose one of " . join(" ", @channels);
		}
	}
} # end SolvisClient_Set
#

sub SolvisClient_SendServerCommand($$) {
	
	my( $this, $command ) = @_ ;
	
	my %ServerCommandPackage = (
		SERVER_COMMAND => {
			"Command" => $command
		}
	) ;
	
	SolvisClient_SendData($this, \%ServerCommandPackage);
}



#####################################
#      Get
sub SolvisClient_Get($@) {
	my ( $this, @a ) = @_;

	### If not enough arguments have been provided
	if ( @a < 2 ) {
		return "\"set Solvis\" needs at least one argument";
	}

	my ($device, $channel) = @a;	

	if ( $channel eq '?' || ! defined($SolvisClient_ChannelDescriptions{$channel} ) ) {
		my @channels = keys(%SolvisClient_ChannelDescriptions) ;
		my $params = "" ;
		my $firstO = _TRUE_ ;
		foreach my $channel (@channels) {
			if ( $SolvisClient_ChannelDescriptions{$channel}{GET} == _FALSE_ ) {
				next ;
			}
			if ( ! $firstO ) {
				$params .= ' ' ;
			} else {
				$firstO = _FALSE_ ;
			}
			$params .= $channel ;
			my $firstI = _TRUE_ ;
		}
		return "unknown argument $a[1] choose one of " .$params;
	} else {

		SolvisClient_Log $this, 4, "Get entered, device := $device, Cannel := $channel";
		SolvisClient_SendGetData($this, $channel) ;

		return "<not valid>" ;
	}

} # end SolvisClient_get
#

#####################################
#      DbLog event interpretation
sub SolvisClient_DbLog_splitFn($)
{
    my ($event) = @_;

    my ($reading, $value, $unit) ;

    my @splited = split(/ /,$event);

    $reading = $splited[0];;
    $reading =~ tr/://d;
	
	$unit = "" ;
	
	if ( defined( $SolvisClient_ChannelDescriptions{$reading}{Unit} ) ) {
		$unit = $SolvisClient_ChannelDescriptions{$reading}{Unit} ;
		$value = $splited[1];
	}
    return ($reading, $value, $unit) ;
}


1;

###START###### Description for fhem commandref ################################################################START####
=pod
=begin html

<a name="SolvisMax"></a>
<h3>SolvisMax</h3>
<ul>
<table>
	<tr>
		<td>
			Um auf die
			<a href="https://www.solvis.de/privatkunden/energiemanager-solvismax.html">Solarheizsystem SolvisMax</a>
			mittels FHEM zugreifen zu k&ouml;nnen, wird die
			<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote.html">SolvisRemote</a>
			ben&ouml;tigt. Sie ist ein Kommunikations-Ger&auml;t, mit einer
			<a href="https://www.solvis.de/public/_processed_/csm_remote_schema_01_c8032327a8.jpg">Web-Oberfl&auml;che</a>.<BR>
			<BR>
			Um das SolvisMax-Modul nutzen zu k&ouml;nnen, muss man mit dem
			<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote/konfigurationsprogramm.html">Konfigurations-Programm</a>
			als erstes die IP-Adresse, den User und das Passwort definieren.<BR>
			Wahlweise kann der Router die IP-Adresse vorgeben, dann sollte jedoch im Router f&uuml;r die
			<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote.html">SolvisRemote</a>
			eine feste IP-Adresse festgelegt sein, damit FHEM diese auch finden kann.<BR>
			<BR>
			Mit der Define-Anweisung wird ein erster Kontakt mit der
			<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote.html">SolvisRemote</a>
			versucht. Abh&auml;ngig, ob die Verbindung erfolgreich war, erfolgt eine entsprechende Meldung im Statusfeld.<BR>
			Wird das Modul das erste Mal gestartet, so wird ein Authorisationsfehler ausgegeben, da der User und das Password mittels
			des Set-Befehls einmalig definiert werden muss.<BR> 
			Ist die Verbindung erfolgreich, wird regelm&auml;&szlig;ig die
			<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote.html">SolvisRemote</a>
			abgefragt.<BR>
			<BR>
			Bedingt durch die Solvis-Software werden s&auml;mtliche Daten bei jeder Abfrage &uuml;bertragen. Um die Anzahl der Updates in
			den Readings zu verringen, unterteilt das SolvisMax-Modul die Daten in dynamische und statische Daten. Beispielsweise
			geh&ouml;rt zu den statischen Daten der Anlagentyp, zu den dynamischen die Vorlauftemperatur des Heizkreises 1.<BR>
			Das Abfrageintervall kann &uuml;ber das Attribut "Interval" ver&auml;ndert werden. Per Default steht es auf 30 (s).<BR>
			&Uuml;ber die Daten wird bei den Temperatur- und Durchfluss-Messungen ein Mittelwert gebildet, da ich bei meiner
			<a href="https://www.solvis.de/privatkunden/energiemanager-solvismax.html">SolvisMax-Anlage</a>
			gesehen habe, dass diese doch recht stark schwanken. Bei dem dabei verwendeten Algorithmus handelt es sich nicht um
			eine einfache Mittelwert-Bildung sondern gr&ouml;&szlig;ere &Auml;nderungen werden bevorzugt. Das bedeutet, dass bei einem steileren
			Temperatur- oder Durchflussanstieg bzw -abfall der neuere Wert h&ouml;her gewichtet wird. Die Anzahl der Messwerte, &uuml;ber
			welche der Mittelwert gebildet wird, kann &uuml;ber das Attribut "Average" ge&auml;ndert werden. Der Default-Wert ist 5.
			<BR>
			Neben der Erfassung dieser Werte berechnet das Modul noch aus diesen Werten die Brennerstarts und Brennerlaufzeit der beiden Brennerstufen.
			Zusätzlich wird auch ein Reading des Brennerstatus berechnet.<BR>
			Folgende Readings werden zusätzlich berechnet:
<ul><ul>
	<table>
		<tr><td align="right" valign="top"><code>X1.BrennerStarts</code> : </td><td align="left" valign="top">Anzahl der Brennerstarts der Stufe 1</td></tr>
		<tr><td align="right" valign="top"><code>X3.BrennerStufe2Starts</code> : </td><td align="left" valign="top">Anzahl der Brennerstarts der Stufe 2</td></tr>
		<tr><td align="right" valign="top"><code>X2.BrennerLaufzeit_s</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 1</td></tr>
		<tr><td align="right" valign="top"><code>X4.BrennerStufe2Laufzeit_s</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 2</td></tr>
		<tr><td align="right" valign="top"><code>X5.BrennerStatus</code> : </td><td align="left" valign="top">Aktuelle Brennstufe des Brenners (off, Stufe1 oder Stufe2)</td></tr>
	</table>
</ul></ul><BR>
			Daneben kann man mit dem Modul auch den Zustand der Anlage (Tag, Nacht, Zeitgeber und Standby) einstellen.
			<BR><BR><BR>
			Auf folgende Weise kann man das Modul in FHEM einbinden und parametrisieren:
			<BR><BR>

<table>
<tr><td><a name="SolvisMaxDefine"></a><b>Define</b></td></tr>
</table>

<table><tr><td><ul><code>define &lt;ger&auml;t&gt; SolvisMax &lt;url&gt;</code></ul></td></tr></table>

<ul><ul>
	<table>
		<tr><td align="right" valign="top"><code>&lt;ger&auml;t&gt;</code> : </td><td align="left" valign="top">
			Der Name des Ger&auml;tes. Empfehlung: "mySolvisMax".</td></tr>
		<tr><td align="right" valign="top"><code>&lt;url&gt;</code> : </td><td align="left" valign="top">
			Eine g&uuml;ltiger Url (IP-Adresse oder Internet-Pfad) des SolvisRemote. Eventuell im Router nachschauen welche
			IP-Addresse der SolvisRemote vom DHCP-Server zugeteilt wurde.</td></tr>
	</table>
</ul></ul>

<BR>

<table>
	<tr><td><a name="SolvisMaxSet"></a><b>Set</b></td></tr>
	<tr><td>
		<ul>
				Die set Funktion &auml;ndert eine Untermenge der Werte der SolvisMax. Aktuell ist nur &Auml;nderung der Brennerlaufzeiten und
				Brennerstarts m&ouml;glich. Das sind auch aktuell nur Pseudo-Werte des Moduls, die Werte der Solvis-Steuerung
				bleiben unver&auml;ndert. Die Laufzeit und die Brennerstarts kann aktuell noch nicht &uuml;ber das Interface
				ausgelesen werden. Um einen Gleichstand zwischen Solvis-Steuerung und Modul zu erreichen, existieren diese beiden
				Set-Befehle.<BR>
				Au&szlig;erdem kann der Zustand der <a href="https://www.solvis.de/privatkunden/energiemanager-solvismax.html">SolvisMax-Anlage</a>
				zwischen Tag, Nacht, Zeit und Standby ge&auml;ndert werden. Dies erfolgt durch simulierte Klicks auf der Consolenoberfl&auml;che. Dieser
				Vorgang dauert relativ lange und um Fhem in dieser Zeit nicht zu blockieren, werden diese Befehle im Hintergrund
				ausgef&uuml;hrt. Es werden max. 10 Befehle in eine Queue zwischengepuffert. Beim &Uuml;berlauf dieser Queue gibt es eine
				Fehlermeldung und der letzte Zustandswechsel wird ignoriert.<BR>
				Mittels der Set-Funktion wird auch der User und das Password f&uuml;r den Zugriff auf die
				<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote.html">SolvisRemote</a>
				definiert, welche mit dem
				<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote/konfigurationsprogramm.html">SolvisRemote-Konfigurator</a>
				festgelegt wurde. Diese beiden Werte werden verschl&uuml;sselt im Modul-Verzeichnis unter FhemUtils/uniqueID abgelegt.
		</ul>
	</td></tr>
</table>

<table><tr><td><ul><code>set &lt;ger&auml;t&gt; &lt;name&gt; &lt;value&gt;</code></ul></td></tr></table>

<ul>
<ul>
	<table>
		<tr><td align="right" valign="top"><code>&lt;ger&auml;t&gt;</code> : </td><td align="left" valign="top">
			Der Name des Ger&auml;tes. Empfehlung: "mySolvisMax".</td></tr>
		<tr><td align="right" valign="top"><code>&lt;name&gt;</code> : </td><td align="left" valign="top">
			Der Name des Wertes, welcher gesetzt werden soll. Z.B.: "<code>X.BrennerStarts</code>"<BR></td></tr>
		<tr><td align="right" valign="top"><code>&lt;value&gt;</code> : </td><td align="left" valign="top">
			Ein g&uuml;ltiger Wert.<BR></td></tr>
	</table>
	<BR>
	</ul>
	Folgende Set-Befehle werden aktuell unterst&uuml;tzt:
	<table>
		<tr>
			<td>
			<tr><td align="right" valign="top"><li><code>X1.BrennerStarts</code>: </li></td><td align="left" valign="top">
				Hier&uuml;ber kann die im Modul hinterlegte Anzahl der Brennerstarts der Stufe 1 ge&auml;ndert werden, beispielsweise
				um diese mit der der Solvis-Steuerung zu synchronisieren.<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>X2.BrennerLaufzeit_s</code>: </li></td><td align="left" valign="top">
				Hier&uuml;ber kann die im Modul hinterlegte Brennerlaufzeit der Stufe 1 ge&auml;ndert werden, beispielsweise um diese
				mit der der Solvis-Steuerung zu synchronisieren. Die Einheit ist s.<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>X3.BrennerStufe2Starts</code>: </li></td><td align="left" valign="top">
				Hier&uuml;ber kann die im Modul hinterlegte Anzahl der Brennerstarts der Stufe 2 ge&auml;ndert werden, beispielsweise
				um diese mit der der Solvis-Steuerung zu synchronisieren.<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>X4.BrennerStufe2Laufzeit_s</code>: </li></td><td align="left" valign="top">
				Hier&uuml;ber kann die im Modul hinterlegte Brennerlaufzeit der Stufe 2 ge&auml;ndert werden, beispielsweise um diese
				mit der der Solvis-Steuerung zu synchronisieren. Die Einheit ist s.<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>User</code>: </li></td><td align="left" valign="top">
				Hiermit wird der in der mit dem
				<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote/konfigurationsprogramm.html">SolvisRemote-Konfigurator</a>
				festgelegte User dem SolvisMax-Modul &uuml;bergeben.<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>Password</code>: </li></td><td align="left" valign="top">
				Hiermit wird dar in der mit dem
				<a href="https://www.solvis.de/privatkunden/fernbedienung-solvisremote/konfigurationsprogramm.html">SolvisRemote-Konfigurator</a>
				festgelegte Passwort dem SolvisMax-Modul &uuml;bergeben.<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>SolvisState</code>: </li></td><td align="left" valign="top">
				Hiermit kann der Zustand der <a href="https://www.solvis.de/privatkunden/energiemanager-solvismax.html">SolvisMax-Anlage</a>
				zwischen Tag, Nacht, Zeitgeber und Standby umgeschaltet werden. Die Parameter-Werte (value) hierfür sind "day", "night", "time" und "standby".<BR>
			</td></tr>
			</td>
		</tr>
	</table>
</ul>

<BR>

<!--table>
	<tr><td><a name="KM200Get"></a><b>Get</b></td></tr>
	<tr><td>
		<ul>
				Die get-Funktion ist in der Lage einen Wert eines Service innerhalb der KM200/KM50 Service Struktur auszulesen.<BR>
				Die zus&auml;tzliche Liste von erlaubten Werten oder der Wertebereich zwischen Minimum und Maximum wird nicht zur&uuml;ck gegeben.<BR>
		</ul>
	</td></tr>
</table>

<table><tr><td><ul><code>get &lt;service&gt; &lt;option&gt;</code></ul></td></tr></table>

<ul><ul>
	<table>
		<tr>
			<td align="right" valign="top"><code>&lt;service&gt;</code> : </td><td align="left" valign="top">Der Name des Service welcher ausgelesen werden soll. Z.B.:  "<code>/heatingCircuits/hc1/operationMode</code>"<BR>
																											 &nbsp;&nbsp;Es gibt nur den Wert, aber nicht die Werteliste oder den m&ouml;glichen Wertebereich zur&uuml;ck.<BR>
			</td>
		</tr>
	</table>
</ul></ul>

<ul><ul>
	<table>
		<tr>
			<td align="right" valign="top"><code>&lt;option&gt;</code> : </td><td align="left" valign="top">Das optionelle Argument f𲠤ie Ausgabe des get-Befehls Z.B.:  "<code>json</code>"<BR>
																											 &nbsp;&nbsp;Folgende Optionen sind verf𧢡r:<BR>
																											 &nbsp;&nbsp;json - Gibt anstelle des Wertes, die gesamte Json Antwort des KMxxx als String zur𣫮 
			</td>
		</tr>
	</table>
</ul></ul>

<BR-->

<table>
	<tr><td><a name="SolvisMaxAttr"></a><b>Attribute</b></td></tr>
	<tr><td>
		<ul>
				Die folgenden Modul-spezifischen Attribute k&ouml;nnen neben den bekannten globalen Attributen gesetzt werden wie
				z.B.: <a href="#room">room</a>.<BR>
		</ul>
	</td></tr>
</table>

<ul><ul>
	<table>
		<tr>
			<td>
			<tr><td align="right" valign="top"><li><code>Average</code>: </li></td><td align="left" valign="top">
				Hier wird festgelegt, &uuml;ber welche Anzahl von Messwerten die modifizierte Mittelwertbildung erfolgt.<BR>
				Der Default-Wert ist 5.<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>Interval</code>: </li></td><td align="left" valign="top">
				Ein g&uuml;ltiges Abfrageintervall f&uuml;r die sich st&auml;ndig ver&auml;ndernden dynamischen Werte des
				SolvisMax-Moduls. Der Wert sollte >=10s sein, um FHEM gen&uuml;gend Zeit einzur&auml;umen eine volle Abfrage
				auszuf&uuml;hren und auch noch &uuml;brige Aufgaben durchzuf&uuml;hren, bevor die n&auml;chste Abfrage startet.
				Einen Anhaltspunkt, wie lange eine Abfrage dauert, ergibt sich aus der Summe der Werte "FhemTime" und
				"HttpTime" der Internals, dabei gibt die "FhemTime" an, wie lange das Fhem durch das Modul bei jeder Abfrage
				gebremst wird. Die "HttpTime" ist f&uuml;r Fhem nicht sichtbar. Das minimale Interval sollte deutlich dar&uuml;ber
				liegen.<BR>
				Der Default-Wert ist 30s.<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>Hidden</code>: </li></td><td align="left" valign="top">
				Da meist keine Anlage voll ausgebaut ist, kann man mittels dieses Attributes die nicht ben&ouml;tigten Readings
				verstecken. Dazu ist als Attributwert eine Liste mit den zu unterdr&uuml;ckenden Reading-Namen durch Leerzeichen
				getrennt zu &uuml;bergeben<BR>
				Der Default-Wert ist leer (keine Readings werden unterdr&uuml;ckt).<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>ButtonReleaseTime_ms</code>: </li></td><td align="left" valign="top">
				Dieses Attribut bestimmt die Release-Zeit nach einem Tastendruck auf der Solvis-Console (Tag, Nacht, Stanby, bzw. Zeit).
				Der Default-Wert ist 500 (ms). Bei Schwierigkeiten bei den Set-Befehlen "day", "night", "standby" bzw. "time"
				sollte dieser Wert vergrößert werden<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>WaittimeAfterButtonRelease_ms</code>: </li></td><td align="left" valign="top">
				Dieses Attribut bestimmt die Wartezeit nachdem die Taste auf der Solvis-Console "losgelassen" wurde.
				Der Default-Wert ist 1000 (ms). Bei Schwierigkeiten bei den Set-Befehlen "day", "night", "standby" bzw. "time"
				sollte dieser Wert vergrößert werden<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>PowerOn</code>: </li></td><td align="left" valign="top">
				Dieses Attribut erm&ouml;glicht Sensor-spezifisch Events für einen bestimmten Zeitraum nach dem Power-On zu unterdr&uuml;cken.
				Der Raumf&uuml;hler ben&oumltigt beispielsweise etwa 30 Minuten!, bis sich seine Temperatur nach einem PowerOn stabilisiert hat.
				Die betroffenen Sensoren mit dem notwendigen Wert werden durch Komma getrennt in dieses Attribut geschrieben.<BR>
				Die Syntax für einen einzelenn Sensor lautet: &lt;Sensorname&gt;:&lt;Zeit in Minuten&gt;<BR>
				Beispiel: RF1.Raumfuehler_HK1:30,S10.Aussentemperatur:20,S02.Warmwassertemperatur:7,S12.Vorlauftemperatur_HK1:7<BR>
			</td></tr>
			<tr><td align="right" valign="top"><li><code>FirmwareLth2.21.02A</code>: </li></td><td align="left" valign="top">
				Dieses Attribut kann auf TRUE gesetzt werden, wenn eine FIrmwareversion >= 2.21.02A verwendet wird. Dann erfolgt
				ein zus&aumltzlicher Test über den Erfolg bzgl. des Dr&uumlckens eines Buttons. Der ist erst von dieser Version
				an m&oumlglich da vorher immer das Dr&uumlcken eines Buttons eine Fehlermeldung erzeugte (War ein Bug von Solvis).
			</td></tr>
			
			
							  "WaittimeAfterButtonRelease_ms".

			</td>
		</tr>
	</table>
</ul></ul>
	
<!--ul><ul>
	<table>
		<tr>
			<td>
			<tr><td align="right" valign="top"><li><code>IntervalStatVal</code> : </li></td><td align="left" valign="top">Ein g&uuml;ltiges Abfrageintervall f&uuml;r die statischen Werte des KM200/KM50. Der Wert muss gr&ouml;&szlig;er gleich >=20s sein um dem Modul gen&uuml;gend Zeit einzur&auml;umen eine volle Abfrage auszuf&uuml;hren bevor die n&auml;chste Abfrage startet. <BR>
																														  Der Default-Wert ist 3600s.<BR>
																														  Der Wert "0" deaktiviert die wiederholte Abfrage der statischen Werte bis das fhem-System erneut gestartet wird oder die fhem.cfg neu geladen wird.<BR>
			</td></tr>
			</td>
		</tr>
	</table>
</ul></ul>

<ul><ul>
	<table>
		<tr>
			<td>
			<tr><td align="right" valign="top"><li><code>PollingTimeout</code> : </li></td><td align="left" valign="top">Ein g&uuml;ltiger Zeitwert um dem KM200/KM50 gen&uuml;gend Zeit zur Antwort einzelner Werte einzur&auml;umen. Normalerweise braucht dieser Wert nicht ver&auml;ndert werden, muss jedoch im Falle eines langsamen Netzwerks erh&ouml;ht werden<BR>
																														 Der Default-Wert ist 5s.<BR>
			</td></tr>
			</td>
		</tr>
	</table>
</ul></ul>

<ul><ul>
	<table>
		<tr>
			<td>
			<tr><td align="right" valign="top"><li><code>ConsoleMessage</code> : </li></td><td align="left" valign="top">Ein g&uuml;ltiger Boolean Wert (0 oder 1) welcher die Aktivit&auml;ten und Fehlermeldungen des Modul in der Konsole ausgibt. "0" (Deaktiviert) or "1" (Aktiviert)<BR>
																														 Der Default-Wert ist 0 (Deaktiviert).<BR>
			</td></tr>			
			</td>
		</tr>
	</table>
</ul></ul>

<ul><ul>
	<table>
		<tr>
			<td>
			<tr><td align="right" valign="top"><li><code>DoNotPoll</code> : </li></td><td align="left" valign="top">Eine durch Leerzeichen (Blank) getrennte Liste von Services welche von der Abfrage aufgrund irrelevanter Werte oder fhem - Abst&uuml;rzen ausgenommen werden sollen.<BR>
																													Die Liste kann auch Hierarchien von services enthalten. Dies bedeutet, das alle Services unterhalb dieses Services ebenfalls gel&ouml;scht werden.<BR>
																													Der Default Wert ist (empty) somit werden alle bekannten Services abgefragt.<BR>
			</td></tr>			
			</td>
		</tr>
	</table>
</ul></ul>

<ul><ul>
	<table>
		<tr>
			<td>
			<tr><td align="right" valign="top"><li><code>ReadBackDelay</code> : </li></td><td align="left" valign="top">Ein g&uuml;ltiger Zeitwert in Mllisekunden [ms] f&uuml;r die Pause zwischen schreiben und zur𣫬esen des Wertes durch den "set" - Befehl. Der Wert muss >=0ms sein.<BR>
																												   Der  Default-Wert ist 100 = 100ms = 0,1s.<BR>
			</td></tr>
			</td>
		</tr>
	</table>
</ul></ul-->

</ul>
=end html