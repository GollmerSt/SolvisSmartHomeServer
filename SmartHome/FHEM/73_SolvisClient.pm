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

		return "The reading process was started.\nThe value will be output only in the readings." ;
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

<a name="SolvisClient"></a>
<h3>SolvisClient</h3>
<ul>
  <table>
    <tr><td>
		Um auf die Solarheizsysteme 
		<a href="https://www.solvis.de/solvisben">SolvisBen</a> oder <a href="https://www.solvis.de/solvismax">SolvisMax</a>
		mittels FHEM zugreifen zu k&ouml;nnen, wird die
		<a href="https://www.solvis.de/solvisremote">SolvisRemote</a>
		ben&ouml;tigt. Sie ist ein Kommunikations-Ger&auml;t, mit einer
		<a href="https://s3.eu-central-1.amazonaws.com/solvis-files/seiten/produkte/solvisremote/remote-mobile.png">Web-Oberfl&auml;che</a>.<BR>
		<BR>
		Um das SolvisClient-Modul nutzen zu k&ouml;nnen, muss man mit dem
		<a href="https://s3.eu-central-1.amazonaws.com/solvis-files/seiten/produkte/solvisremote/Download/konfig-remote.zip">Konfigurations-Programm</a>
		als erstes die IP-Adresse, den User und das Passwort definieren.<BR>
		Wahlweise kann der Router die IP-Adresse vorgeben, dann sollte jedoch im Router f&uuml;r die
		<a href="https://www.solvis.de/solvisremote">SolvisRemote</a>
		eine feste IP-Adresse festgelegt sein, damit der SolvisSmartHome-Server diese auch finden kann.<BR>
		<BR>
		Neben der <a href="https://www.solvis.de/solvisremote">SolvisRemote</a> ist noch der SolvisSmartHome-Server zu installieren und einzurichten,
		der Bestandteil des vorliegenden FHEM-Moduls ist. Gemäß der beiliegenden Anleitung ist dieser erst einzurichten und dort die Anlagenparameter
		zu definieren.<BR>
		<BR>
		Mit der Define-Anweisung des FHEM-Moduls wird dann eine Verbindung zum Server versucht. 
		Abh&auml;ngig, ob die Verbindung erfolgreich war, erfolgt eine entsprechende Meldung im Statusfeld.<BR>
		Ist die Verbindung erfolgreich, wird der Client regelm&auml;&szlig;ig mit Daten der Heizungsanlage vom Server versorgt.<BR>
		<BR>
		Neben der Erfassung dieser Werte berechnet der Server noch aus diesen Werten die Brennerstarts und Brennerlaufzeit der beiden Brennerstufen.
		Zusätzlich wird auch ein Reading des Brennerstatus berechnet sowie die Ruhestellung der Mischer.<BR>
		<BR>
		Folgende Readings werden zusätzlich berechnet:
      <ul>
        <ul>
          <table>
			<tr><td align="right" valign="top"><code>X1.BrennerStarts</code> : </td><td align="left" valign="top">Anzahl der Brennerstarts der Stufe 1</td></tr>
			<tr><td align="right" valign="top"><code>X3.BrennerStufe2Starts</code> : </td><td align="left" valign="top">Anzahl der Brennerstarts der Stufe 2</td></tr>
			<tr><td align="right" valign="top"><code>X2.BrennerLaufzeit_s</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 1</td></tr>
			<tr><td align="right" valign="top"><code>X4.BrennerStufe2Laufzeit_s</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 2</td></tr>
			<tr><td align="right" valign="top"><code>X5.BrennerStatus</code> : </td><td align="left" valign="top">Aktuelle Brennstufe des Brenners (off, Stufe1 oder Stufe2)</td></tr>
			<tr><td align="right" valign="top"><code>X6.UhrzeitSolvis</code> : </td><td align="left" valign="top">Uhrzeit der SolvisControl</td></tr>
			<tr><td align="right" valign="top"><code>X7.MischerPosition0_HK1</code> : </td><td align="left" valign="top">Mischer des Heizkreises 1 in Ruhestellung</td></tr>
			<tr><td align="right" valign="top"><code>X8.MischerPosition0_HK2</code> : </td><td align="left" valign="top">Mischer des Heizkreises 2 in Ruhestellung</td></tr>
          </table>
        </ul>
	  </ul><BR><BR>
	    Daneben gibt es noch weitere Werte, welche aus dem GUI der SolvisControl mittels OCR ermittelt werden. Diese werden nur dann aktualisiert, wenn der Wert durch
		einen GET/SET-Befehl ausgelesen/verändert wird. Sie sind also nicht immer auf dem aktuellen Stand.<BR>
		<BR>
		Folgende Readings werden aus der GUI ermittelt:
	  <ul>
	    <ul>
	      <table>
			<tr><td align="right" valign="top"><code>C01.StartsBrenner</code> : </td><td align="left" valign="top">Anzahl der Brennerstarts der Stufe 1</td></tr>
			<tr><td align="right" valign="top"><code>C02.LaufzeitBrenner</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 1 (in h)</td></tr>
			<tr><td align="right" valign="top"><code>C03.LaufzeitAnforderung2</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 2 (in h)</td></tr>
			<tr><td align="right" valign="top"><code>C04.WarmwasserPumpe</code> : </td><td align="left" valign="top">Warmwasserpume (an/aus/auto)</td></tr>
			<tr><td align="right" valign="top"><code>C05.WassertemperaturSoll</code> : </td><td align="left" valign="top">Solltemperatur Warmwasser (10 .. 65&deg;C)</td></tr>
			<tr><td align="right" valign="top"><code>C06.Anlagenmodus_HK1</code> : </td><td align="left" valign="top">Modus des Heizkreises 1 (Tag/Nacht/Standby/Timer)</td></tr>
			<tr><td align="right" valign="top"><code>C07.Tagestemperatur_HK1</code> : </td><td align="left" valign="top">Solltemperatur Tag Heizkreis 1</td></tr>
			<tr><td align="right" valign="top"><code>C08.Nachttemperatur_HK1</code> : </td><td align="left" valign="top">Solltemperatur Nacht Heizkreis 1</td></tr>
			<tr><td align="right" valign="top"><code>C09.TemperaturFeineinstellung_HK1</code> : </td><td align="left" valign="top">Temperaturfeineinstellung Heizkreis 1 (-5 ... 5)</td></tr>
			<tr><td align="right" valign="top"><code>C10.Raumeinfluss_HK1</code> : </td><td align="left" valign="top">Raumeinfluss Heizkreis 1 (0 ... 90%)</td></tr>
			<tr><td align="right" valign="top"><code>C11.Vorlauf_Soll_HK1</code> : </td><td align="left" valign="top">Sollwert der Vorlauftemperatur Heizkreis 1</td></tr>
			<tr><td align="right" valign="top"><code>C12.Anlagenmodus_HK2</code> : </td><td align="left" valign="top">Modus des Heizkreises 2 (Tag/Nacht/Standby/Timer)</td></tr>
			<tr><td align="right" valign="top"><code>C13.Tagestemperatur_HK2</code> : </td><td align="left" valign="top">Solltemperatur Tag Heizkreis 2</td></tr>
			<tr><td align="right" valign="top"><code>C14.Nachttemperatur_HK2</code> : </td><td align="left" valign="top">Solltemperatur Nacht Heizkreis 2</td></tr>
			<tr><td align="right" valign="top"><code>C15.TemperaturFeineinstellung_HK2</code> : </td><td align="left" valign="top">Temperaturfeineinstellung Heizkreis 2 (-5 ... 5)</td></tr>
			<tr><td align="right" valign="top"><code>C16.Raumeinfluss_HK2</code> : </td><td align="left" valign="top">Raumeinfluss Heizkreis 2(0 ... 90%)</td></tr>
			<tr><td align="right" valign="top"><code>C17.Vorlauf_Soll_HK2</code> : </td><td align="left" valign="top">Sollwert der Vorlauftemperatur Heizkreis 2</td></tr>
          </table>
        </ul>
      </ul><BR><BR><BR><BR>
        Auf folgende Weise kann man das Modul in FHEM einbinden und parametrisieren:
		<BR><BR>

      <table>
        <tr><td><a name="SolvisClientDefine"></a><b>Define</b></td></tr>
      </table>

      <table>
		<tr><td><ul><code>define &lt;ger&auml;t&gt; SolvisClient &lt;url&gt;</code></ul></td></tr>
      </table>

      <ul>
		<ul>
	      <table>
		    <tr><td align="right" valign="top"><code>&lt;ger&auml;t&gt;</code> :</td><td align="left" valign="top">
		      Der Name des Ger&auml;tes. Empfehlung: "mySolvisMax".</td></tr>
		    <tr><td align="right" valign="top"><code>&lt;url&gt;</code> :</td><td align="left" valign="top">
		      Eine g&uuml;ltiger Url (IP-Adresse oder Internet-Pfad) des SolvisSmartHome-Servers mit Channelnumber (normalerweise 10735). Eventuell im Router nachschauen welche
			  IP-Addresse der SolvisSmartHome-Server vom DHCP-Server zugeteilt wurde. Sollte der Server auf dem Fhem-System laufen, kann auch "localhost:10735" eingetragen werden</td></tr>
	      </table>
        </ul>
	  </ul><BR><BR>

      <table>
	    <tr><td><a name="SolvisClientSet"></a><b>Set</b></td></tr>
	    <tr><td>
		  <ul>
			Die Set Funktion &auml;ndert eine Untermenge der Anlagen-Werte der Solvis. Aktuell sind die Ver&auml;nderungen folgender Werte m&ouml;glich:
            <ul>
		      <ul>
	            <table>
					<tr><td align="right" valign="top"><code>C04.WarmwasserPumpe</code> : </td><td align="left" valign="top">Warmwasserpume (an/aus/auto)</td></tr>
					<tr><td align="right" valign="top"><code>C05.WassertemperaturSoll</code> : </td><td align="left" valign="top">Solltemperatur Warmwasser (10 .. 65&deg;C)</td></tr>
					<tr><td align="right" valign="top"><code>C06.Anlagenmodus_HK1</code> : </td><td align="left" valign="top">Modus des Heizkreises 1 (Tag/Nacht/Standby/Timer)</td></tr>
					<tr><td align="right" valign="top"><code>C07.Tagestemperatur_HK1</code> : </td><td align="left" valign="top">Solltemperatur Tag Heizkreis 1</td></tr>
					<tr><td align="right" valign="top"><code>C08.Nachttemperatur_HK1</code> : </td><td align="left" valign="top">Solltemperatur Nacht Heizkreis 1</td></tr>
					<tr><td align="right" valign="top"><code>C09.TemperaturFeineinstellung_HK1</code> : </td><td align="left" valign="top">Temperaturfeineinstellung Heizkreis 1 (-5 ... 5)</td></tr>
					<tr><td align="right" valign="top"><code>C10.Raumeinfluss_HK1</code> : </td><td align="left" valign="top">Raumeinfluss Heizkreis 1 (0 ... 90%)</td></tr>
					<tr><td align="right" valign="top"><code>C12.Anlagenmodus_HK2</code> : </td><td align="left" valign="top">Modus des Heizkreises 2 (Tag/Nacht/Standby/Timer)</td></tr>
					<tr><td align="right" valign="top"><code>C13.Tagestemperatur_HK2</code> : </td><td align="left" valign="top">Solltemperatur Tag Heizkreis 2</td></tr>
					<tr><td align="right" valign="top"><code>C14.Nachttemperatur_HK2</code> : </td><td align="left" valign="top">Solltemperatur Nacht Heizkreis 2</td></tr>
					<tr><td align="right" valign="top"><code>C15.TemperaturFeineinstellung_HK2</code> : </td><td align="left" valign="top">Temperaturfeineinstellung Heizkreis 2 (-5 ... 5)</td></tr>
					<tr><td align="right" valign="top"><code>C16.Raumeinfluss_HK2</code> : </td><td align="left" valign="top">Raumeinfluss Heizkreis 2(0 ... 90%)</td></tr>
	            </table>
              </ul>
			</ul><BR>
          </ul>
	    </td></tr>
      </table>

      <table>
	    <tr><td><ul><code>set &lt;ger&auml;t&gt; &lt;name&gt; &lt;value&gt;</code></ul></td></tr>
      </table>

      <ul>
        <ul>
	      <table>
		    <tr><td align="right" valign="top"><code>&lt;ger&auml;t&gt;</code> : </td><td align="left" valign="top">
					Der Name des Ger&auml;tes. Empfehlung: "mySolvisMax".</td></tr>
			<tr><td align="right" valign="top"><code>&lt;name&gt;</code> : </td><td align="left" valign="top">
					Der Name des Wertes, welcher gesetzt werden soll. Z.B.: "<code>C08.Nachttemperatur_HK1</code>"<BR></td></tr>
			<tr><td align="right" valign="top"><code>&lt;value&gt;</code> : </td><td align="left" valign="top">
					Ein g&uuml;ltiger Wert.<BR></td></tr>
	      </table><BR>
	    </ul>
	        neben der SET-Befehle zur &Auml;nderung der obigen Anlagen-Werte, existieren noch zus&auml;tzlich folgende Server-Befehle (&lt;name&gt;: "ServerCommand", <value> nach folgender Tabelle):
        <ul>
	      <table>
		    <tr><td align="right" valign="top"><code>BACKUP</code>: </td><td align="left" valign="top">
		      Sichert die berechneten Messwerte (X1 .. X8) in einen Backup-Datei<BR>
		    </td></tr>
		    <tr><td align="right" valign="top"><code>SCREEN_RESTORE_INHIBIT</code>: </td><td align="left" valign="top">
			  Normalerweise wird nach einer Parameter-Abfrage im GUI der SolvisControl wieder zum vorherigen Bildschirm zur&uuml;ckgegangen. Dieses verhalten wird durch diesen Befehl verhindert.<BR>
		    </td></tr>
		    <tr><td align="right" valign="top"><code>SCREEN_RESTORE_ENABLE</code>: </td><td align="left" valign="top">
			  Gegenstück zu dem ServerCommand SCREEN_RESTORE_INHIBIT<BR>
		    </td></tr>
	      </table>
		</ul>
      </ul><BR><BR>
      <table>
	    <tr><td><a name="SolvisClientGet"></a><b>Get</b></td></tr>
	    <tr><td>
		  <ul>
			  Die Get Funktion st&ouml;&szlig;t das Auslesen von Anlagenwerten aus der GUI der SolvisControl an. Folgende Werte k&ouml;nnen ausgelesen werden:
            <ul>
		      <ul>
	            <table>
					<tr><td align="right" valign="top"><code>C01.StartsBrenner</code> : </td><td align="left" valign="top">Anzahl der Brennerstarts der Stufe 1</td></tr>
					<tr><td align="right" valign="top"><code>C02.LaufzeitBrenner</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 1 (in h)</td></tr>
					<tr><td align="right" valign="top"><code>C03.LaufzeitAnforderung2</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 2 (in h)</td></tr>
					<tr><td align="right" valign="top"><code>C04.WarmwasserPumpe</code> : </td><td align="left" valign="top">Warmwasserpume (an/aus/auto)</td></tr>
					<tr><td align="right" valign="top"><code>C05.WassertemperaturSoll</code> : </td><td align="left" valign="top">Solltemperatur Warmwasser (10 .. 65&deg;C)</td></tr>
					<tr><td align="right" valign="top"><code>C06.Anlagenmodus_HK1</code> : </td><td align="left" valign="top">Modus des Heizkreises 1 (Tag/Nacht/Standby/Timer)</td></tr>
					<tr><td align="right" valign="top"><code>C07.Tagestemperatur_HK1</code> : </td><td align="left" valign="top">Solltemperatur Tag Heizkreis 1</td></tr>
					<tr><td align="right" valign="top"><code>C08.Nachttemperatur_HK1</code> : </td><td align="left" valign="top">Solltemperatur Nacht Heizkreis 1</td></tr>
					<tr><td align="right" valign="top"><code>C09.TemperaturFeineinstellung_HK1</code> : </td><td align="left" valign="top">Temperaturfeineinstellung Heizkreis 1 (-5 ... 5)</td></tr>
					<tr><td align="right" valign="top"><code>C10.Raumeinfluss_HK1</code> : </td><td align="left" valign="top">Raumeinfluss Heizkreis 1 (0 ... 90%)</td></tr>
					<tr><td align="right" valign="top"><code>C11.Vorlauf_Soll_HK1</code> : </td><td align="left" valign="top">Sollwert der Vorlauftemperatur Heizkreis 1</td></tr>
					<tr><td align="right" valign="top"><code>C12.Anlagenmodus_HK2</code> : </td><td align="left" valign="top">Modus des Heizkreises 2 (Tag/Nacht/Standby/Timer)</td></tr>
					<tr><td align="right" valign="top"><code>C13.Tagestemperatur_HK2</code> : </td><td align="left" valign="top">Solltemperatur Tag Heizkreis 2</td></tr>
					<tr><td align="right" valign="top"><code>C14.Nachttemperatur_HK2</code> : </td><td align="left" valign="top">Solltemperatur Nacht Heizkreis 2</td></tr>
					<tr><td align="right" valign="top"><code>C15.TemperaturFeineinstellung_HK2</code> : </td><td align="left" valign="top">Temperaturfeineinstellung Heizkreis 2 (-5 ... 5)</td></tr>
					<tr><td align="right" valign="top"><code>C16.Raumeinfluss_HK2</code> : </td><td align="left" valign="top">Raumeinfluss Heizkreis 2(0 ... 90%)</td></tr>
					<tr><td align="right" valign="top"><code>C17.Vorlauf_Soll_HK2</code> : </td><td align="left" valign="top">Sollwert der Vorlauftemperatur Heizkreis 2</td></tr>
	            </table>
              </ul>
		    </ul><BR>
          </ul>
	    </td></tr>
      </table>
      <table>
	    <tr><td><ul><code>get &lt;ger&auml;t&gt; &lt;name&gt;</code></ul></td></tr>
	  </table>
      <ul>
        <ul>
	      <table>
            <tr><td align="right" valign="top"><code>&lt;ger&auml;t&gt;</code> : </td><td align="left" valign="top">
				Der Name des Ger&auml;tes. Empfehlung: "mySolvisMax".</td></tr>
		    <tr><td align="right" valign="top"><code>&lt;name&gt;</code> : </td><td align="left" valign="top">
			    Der Name des Wertes, welcher ausgelesen werden soll. Z.B.: "<code>C08.Nachttemperatur_HK1</code>"<BR></td></tr>
	      </table>
	    </ul>
	  </ul><BR><BR>
      <table>
	    <tr><td><a name="SolvisClientAttr"></a><b>Attribute</b></td></tr>
	    <tr><td>
	      <ul>
		      Die folgenden Modul-spezifischen Attribute k&ouml;nnen neben den bekannten globalen Attributen gesetzt werden wie
		      z.B.: <a href="#room">room</a>.<BR>
            <ul>
		      <ul>
	            <table>
				    <tr><td align="right" valign="top"><code>SolvisName</code> : </td><td align="left" valign="top">Name der Id der Unit des Servers (in base.xml definiert). Wenn das Attribut nicht definiert ist, muss der FHEM-Gerätename identisch mit der Id der Unit des base.xml des Servers sein.</td></tr>
	            </table>
              </ul>
		    </ul><BR>
		  </ul>
	    </td></tr>
      </table>

      <table>
	    <tr><td><ul><code>attr &lt;ger&auml;t&gt; &lt;name&gt; &lt;value&gt;</code></ul></td></tr>
	  </table>
      <ul>
        <ul>
	      <table>
			  <tr><td align="right" valign="top"><code>&lt;ger&auml;t&gt;</code> : </td><td align="left" valign="top">
				  Der Name des Ger&auml;tes. Empfehlung: "mySolvisMax".</td></tr>
			  <tr><td align="right" valign="top"><code>&lt;name&gt;</code> : </td><td align="left" valign="top">
				  Der Name des Wertes, welcher gesetzt werden soll. Aktuell nur "SolvisName" m&ouml;glich</code>"<BR></td></tr>
			  <tr><td align="right" valign="top"><code>&lt;value&gt;</code> : </td><td align="left" valign="top">
				  Ein g&uuml;ltiger Wert.<BR></td></tr>
	      </table><BR>
	    </ul>
      </ul>
	</td></tr>
  </table>
</ul>
=end html