########################################################################################################################
#
# Attention! This file isn't in the FHEM repository, a private one is used.
# $Id$
#
#  (c) 2019-2020 Copyright: Stefan Gollmer (Stefan dot Gollmer at gmail dot com)
#  All rights reserved
#
#                                          CHANGELOG
#
#   Version     Date        Programmer          Description of Change
#   00.01.00    17.12.2019  SCMP77              Initial Release
#   00.02.00    21.01.2020  SCMP77              Server commands are determined by the server itself
#   00.02.01    06.02.2020  SCMP77              Updated to current messages
#   00.02.02    13.02.2020  SCMP77              Attribut enable gui commands addes
#   00.02.03    24.03.2020  SCMP77              Using FHEM::SolvisClient-Package, Supports FHEM:Meta, Allgemeine Perl
#   00.02.04    01.05.2020  SCMP77              Reconnction after unknown client shortend, Timeout reported
#   00.02.05    06.05.2020  SCMP77              Length bug of SendData fixed, Some optimizations
#   00.02.06    11.05.2020  SCMP77              HumanAccess status added, HTML-Beschreibung ergänzt
#   00.02.07    21.05.2020  SCMP77              GUI_COMMANDS_ENABLE/DISABLE was incorrectly sent on reconnection
#   00.02.08    25.05.2020  SCMP77              Some variables moved to helper (should not be visible in Web-Interface)
#   00.02.09    05.06.2020  SCMP77              Set of a binary value now possible, Implementation of a min time between connection
#   00.02.10    10.06.2020  SCMP77              ReadyFn simplified, reconnect on JSON errors
#   00.02.11    02.07.2020  SCMP77              Set bug fixed
#   00.02.12    19.07.2020  SCMP77              Server compile time is written to the log
#   00.02.13    25.09.2020  SCMP77              Meta-Daten-Begandlung ergänzt (wegen WW_Pumpe_Min_Laufzeit).
#   00.02.14    04.10.2020  SCMP77              Meta-Beschreibung nun Device-abhängig, Doku ergänzt.
#   00.02.15    18.11.2020  SCMP77              Nur HTML-Doku
#   00.02.16    07.12.2020  SCMP77              Command SelectScreen supported
#   00.02.17    12.01.2021  SCMP77              Command error message will now be written to the log file
#   00.02.18    01.02.2021  SCMP77              Fixed: Solange der Server nicht erreichbar ist, wurde bis zum Timeout FHEM blockiert.
#   00.02.19    13.02.2021  SCMP77              Fixed: Limit des Connection-Intervalls hatte keine  Wirkung
#   00.02.20    26.02.2021  SCMP77              testing -> stable
#   00.02.21    26.02.2021  SCMP77              supports server format version >= 2.0
#   00.02.22    26.02.2021  SCMP77              Eine define-Anweisung im laufenden Betrieb bewirkte eine ständige
#                                                 Verbindungsaufbauwiederholung, da die Fehlerbehandlung unvollständig war.

# !!!!!!!!!!!!!!!!! Zu beachten !!!!!!!!!!!!!!!!!!!
# !! Version immer hinten in META.json eintragen !!
# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!



#####################################
# Offene Aufgaben:
#   1. Unterdruecken der Readings aus Solvis-Daten, welche zeigen, dass Heizkreise o.ae. nicht existieren
#         In diesem fall gibt es Kanal-abhängige Werte wie 2500 oder 00. Muss im XML vermutlich Kanal-
#         weise definiert werden
#

#####################################
# Bekannte Probleme


##############################################


package FHEM::SolvisClient;

use strict;
#use v5.10;
use warnings;
use FHEM::Meta;
use GPUtils qw(GP_Import GP_Export);

use constant MODULE => 'SolvisClient';

use constant WATCH_DOG_INTERVAL => 300 ;            #Violates 'ProhibitConstantPragma'
                                                    #Mindestens alle 5 Minuten muss der Client Daten vom Servere erhalten haben.
                                                    # Andernfalls wird die Verbindung neu aufgebaut
use constant RECONNECT_AFTER_UNKNOWN_CLIENT => 2 ;  #Violates 'ProhibitConstantPragma'
use constant RECONNECT_AFTER_DISMISS => 30 ;        #Violates 'ProhibitConstantPragma'
use constant RECONNECT_AFTER_TRANSFER_ERROR => 30 ; #Violates 'ProhibitConstantPragma'

use constant MIN_CONNECTION_INTERVAL => 5 ;         #Violates 'ProhibitConstantPragma'
use constant MAX_CONNECTION_INTERVAL => 120 ;       #Violates 'ProhibitConstantPragma'

use constant _FALSE_ => 0 ;                         #Violates 'ProhibitConstantPragma'
use constant _TRUE_ => 1;   #Violates 'ProhibitConstantPragma'

use constant MIN_SUPPORTED_FORMAT => 1.00 ;          #Violates 'ProhibitConstantPragma'
use constant MAX_SUPPORTED_FORMAT => 2.99 ;          #Violates 'ProhibitConstantPragma'
use constant MIN_RECOMMENDED_FORMAT => 2 ;          #Violates 'ProhibitConstantPragma'

use constant _DISABLE_GUI_ => 0 ;                   #Violates 'ProhibitConstantPragma'
use constant _ENABLE_GUI_ => 1 ;                    #Violates 'ProhibitConstantPragma'
use constant _UPDATE_GUI_ => 2 ;                    #Violates 'ProhibitConstantPragma'




# try to use JSON::MaybeXS wrapper
#   for chance of better performance + open code
my $success = eval {
    require JSON::MaybeXS;
    import JSON::MaybeXS qw( decode_json encode_json );
    1;
};

if (!$success) {

    # try to use JSON wrapper
    #   for chance of better performance
    $success = eval {

        # JSON preference order

        if ( !defined( $ENV{PERL_JSON_BACKEND} ) ) {
            local $ENV{PERL_JSON_BACKEND} =
                'Cpanel::JSON::XS,JSON::XS,JSON::PP,JSON::backportPP';
        }

        require JSON;
        import JSON qw( decode_json encode_json );
        1;
    };

    if (!$success) {

        # In rare cases, Cpanel::JSON::XS may
        #   be installed but JSON|JSON::MaybeXS not ...
        $success = eval {
            require Cpanel::JSON::XS;
            import Cpanel::JSON::XS qw(decode_json encode_json);
            1;
        };

        if (!$success) {

            # In rare cases, JSON::XS may
            #   be installed but JSON not ...
            $success = eval {
                require JSON::XS;
                import JSON::XS qw(decode_json encode_json);
                1;
            };

            if (!$success) {

                # Fallback to built-in JSON which SHOULD
                #   be available since 5.014 ...
                $success = eval {
                    require JSON::PP;
                    import JSON::PP qw(decode_json encode_json);
                    1;
                };

                if (!$success) {

                    # Fallback to JSON::backportPP in really rare cases
                    require JSON::backportPP;
                    import JSON::backportPP qw(decode_json encode_json);
                    1;
                }
            }
        }
    }
}


## Import der FHEM Funktionen
#-- Run before package compilation
BEGIN {

    # Import from main context
    GP_Import(
        qw(
            defs
            IsDisabled
            init_done
            Log3
            gettimeofday
            deviceEvents
            DevIo_OpenDev
            DevIo_CloseDev
            DevIo_SimpleWrite
            DevIo_SimpleRead
            DevIo_IsOpen
            InternalTimer
            RemoveInternalTimer
            AttrVal
            readingFnAttributes
            readingsSingleUpdate
            readingsBeginUpdate
            readingsBulkUpdate
            readingsEndUpdate
            )
    );
}

#-- Export to main context with different name
GP_Export(
    qw(
      Initialize
      )
);



my %devicesChannelDescriptions ;
my %devicesSetParameters ;
my %devicesServerCommands ;
my %devicesScreenCommands ;



#####################################
#
#       Log
#

sub Log {
    my ( $self, $loglevel, $text ) = @_;

    my $xline       = ( caller(0) )[2];
    my $xsubroutine = ( caller(1) )[3];
    my $sub         = ( split( ':', $xsubroutine ) )[2];
    $sub =~ s/SolvisClient_//;

    my $instName = ( ref($self) eq 'HASH' ) ? $self->{NAME} : $self;
    Log3 $self, $loglevel, MODULE." $instName: $sub.$xline $text";

    return;
} # end Log



#####################################
#
#       Initialize
#

sub Initialize {
    my $modulData = shift;

    $modulData->{DefFn}         = \&Define;
    $modulData->{UndefFn}       = \&Undef;
    $modulData->{DeleteFn}      = \&Delete;
    $modulData->{ReadFn}        = \&Read;
    $modulData->{ReadyFn}       = \&Ready;
    $modulData->{ShutdownFn}    = \&Undef;
    $modulData->{SetFn}         = \&Set;
    $modulData->{GetFn}         = \&Get;
    $modulData->{NotifyFn}      = \&Notify;
    $modulData->{AttrFn}        = \&Attr;
    $modulData->{AttrList}      =
                                  'SolvisName '.
                                  'GuiCommandsEnabled:TRUE,FALSE '.
                                  $readingFnAttributes;
    $modulData->{DbLog_splitFn} = \&DbLog_splitFn;

    return FHEM::Meta::InitMod( __FILE__, $modulData );

} # end Initialize



#####################################
#
#       Define
#

sub Define {  #define heizung SolvisClient 192.168.1.40 SGollmer e$am1kro
    my $self = shift;
    my $def = shift;

    if ( !FHEM::Meta::SetInternals($self)  ) {
        return ($self,$def);
    }

    my @args = split( '[ \t][ \t]*', $def );
    my $url  = $args[2];
    my $name = $self->{NAME};
    
    if ( defined($self->{DeviceName}) && DevIo_IsOpen($self) ) {
        DevIo_CloseDev($self);
    }


    $self->{DeviceName}  = $url;    #Für DevIO, Name fest vorgegeben
    DevIoConnected($self);


    if( $init_done ) {
        my $result = Connect( $self, 0 );
    } else {
        $self->{NOTIFYDEV} = 'global';
    }

    $self->{helper}{GuiEnabled} = undef;

    use version 0.77;
    our $CLIENT_VERSION = FHEM::Meta::Get( $self, 'version' );
    $self->{VERSION_CLIENT} = version->parse($CLIENT_VERSION)->normal;

    readingsSingleUpdate($self,'HumanAccess','none',1);

    return;
} # end Define



#####################################
#
#       Attribute
#
sub Attr {
    my $cmd = shift;
    my $name = shift;
    my $attrName = shift;
    my $attrValue   = shift;

    my $self = $defs{$name};

    my %switch = (
        'GuiCommandsEnabled' => sub {
            if ( defined $attrValue  && $attrValue ne 'TRUE' && $attrValue   ne 'FALSE' ) {
                return "Unknown value $attrValue for $attrName, choose one of TRUE FALSE";
            }
       },
       'SolvisName' => sub {
       }
    );

    if ( defined($switch{ $attrName })) {
        $switch{ $attrName }->();
    }
} # end Attr



#####################################
#
#       Try connection if FHEM is initalized or configuration was rereaded
#
sub Notify {
    my $self = shift;
    my $eventObject = shift;

    my $ownName = $self->{NAME}; # own name

    if ( IsDisabled($ownName)) {
        return ;    # Return without any further action if the module is disabled
    }

    my $devName = $eventObject->{NAME}; # Device that created the events
    my $events = deviceEvents($eventObject, 1);

    if($devName eq 'global' && grep( { m/^INITIALIZED|REREADCFG$/x } @{$events})) {

        $self->{helper}{GuiEnabled} = undef;
        my $result = Connect( $self, 0 );
        $self->{NOTIFYDEV} = '';
        Log($self, 3, 'New Connection in case of rereadcfg or initialized');
    }
    return;
} # end Notify



#####################################
#
#       Connect to Server
#
sub Connect {
    my $self = shift;
    my $reopen = shift;

    my $connectedSub = $reopen?\&SendReconnectionData:\&SendConnectionData;

    if (DevIo_IsOpen($self)) {
        Log( $self, 3, "Connection wasn't closed");
        ReconnectAfterDismiss( $self, GetNextConnectionInterval( $self ) );
        return;
    }

    $self->{helper}{BUFFER} = '';

    $self->{helper}{ConnectionOngoing}  = 1;    #Verbindungsaufbau in Arbeit

    DevIo_OpenDev($self, $reopen, $connectedSub, \&ConnectionCallback );
    
} # end Connect


#####################################
#
#       Connection Callback
#
sub ConnectionCallback {
    my $self = shift;
    my $error = shift;

    $self->{helper}{ConnectionOngoing}  = 0;    #Verbindungsaufbau nicht in Arbeit
    
    if ( defined($error)) {
        Log( $self, 4, "Connection not successfull, error := $error ");
        ReconnectAfterDismiss( $self, GetNextConnectionInterval( $self ) );
    }

}





#####################################
#
#       DevIoConnected
#
sub DevIoConnected {
    my $self = shift;

    $self->{helper}{ConnectionOngoing}  = 0;    #Verbindungsaufbau nicht in Arbeit
    $self->{helper}{ConnectionInterval}  = MIN_CONNECTION_INTERVAL ;
    $self->{helper}{NEXT_OPEN} = undef;
}



####################################
#
#       Send connection data
#
sub SendConnectionData {
    my $self = shift;

    DevIoConnected($self);

    $self->{CLIENT_ID} = undef;

    SendData( $self, 'CONNECT', 'Id', AttrVal( $self->{NAME}, 'SolvisName', $self->{NAME} ) );

    return;

} # end SendConnectionData



################################################
#
#       Send reconnection data
#
sub SendReconnectionData {
    my $self = shift;

    DevIoConnected($self);
    
    if ( defined( $self->{CLIENT_ID} ) ) {

        SendData( $self, 'RECONNECT', 'Id', $self->{CLIENT_ID} );

    } else {
         SendConnectionData($self);
    }

    return;
} # end SendReconnectionData


###############################################
#
#       Get new connection interval after error or unsuccessfull onnection

sub GetNextConnectionInterval {
    
    my $self = shift;
    
    if ( $self->{helper}{ConnectionInterval} < MAX_CONNECTION_INTERVAL ) {
        $self->{helper}{ConnectionInterval} += MIN_CONNECTION_INTERVAL;
    }

    return $self->{helper}{ConnectionInterval};
}



################################################
#
#       Ein Reconnect wird versucht
#
sub Ready {
    my $self = shift;

    my $now = time;

    if ( $self->{helper}{ConnectionOngoing} == 1 || defined( $self->{helper}{NEXT_OPEN} ) && $self->{helper}{NEXT_OPEN} > $now) {
        #Log( $self, 4, "ConnectionOngoing = $self->{helper}{ConnectionOngoing}");
        return;
    }
    
    $self->{helper}{NEXT_OPEN} = $now + GetNextConnectionInterval( $self );

    Log( $self, 4, 'Reconnection try');
    return Connect($self, 1) ; # reopen

} # end Ready



################################################
#
#       Daten vom Server erhalten
#
sub Read {
    my $self = shift;

    my $name = $self->{NAME};

    RemoveInternalTimer($self, \&WatchDogTimeout );
    my $timeStamp = gettimeofday() + WATCH_DOG_INTERVAL;
    InternalTimer($timeStamp, \&WatchDogTimeout, $self );

    Log($self, 5, 'Read entered');

    # einlesen der bereitstehenden Daten
    my $buf = DevIo_SimpleRead($self);

    if ( ! defined( $buf ) ) {
        return;
    }

    $self->{helper}{BUFFER} .= $buf;

    #Log($self, 3, "Current buffer content: $self->{helper}{BUFFER}");

    while ( length($self->{helper}{BUFFER}) >= 3 ) {

        my $bufferLength = length($self->{helper}{BUFFER});

        my @parts = unpack('CCC', $self->{helper}{BUFFER} );
        my $length = $parts[2] | $parts[1] << 8 | $parts[0] << 16;

        Log($self, 5, "Length of package: $length");

        if ( $length > $bufferLength - 3 ) {
            return;
        }

        @parts = unpack('CCCa'.$length.'a*', $self->{helper}{BUFFER} );
        $self->{helper}{BUFFER} = $parts[4];

        Log($self, 5, "Package encoded: $parts[3]");

        my $receivedData = {};

        eval {
            $receivedData = decode_json ($parts[3]);
        };
        if ( $@ ) {
            Log($self, 3, "Error on receiving JSON package occured: $@, try reconnection");
            ReconnectAfterDismiss($self, RECONNECT_AFTER_TRANSFER_ERROR);
            return;
        }

        ExecuteCommand($self, $receivedData);
    }

    return;
} # end Read



#####################################
#
#       Execute server commands
#
sub ExecuteCommand {
    my $self = shift;
    my $receivedData = shift;

    my @key = keys %$receivedData;

    my $command = $key[0];

    Log($self, 4, "Command detected: $command");

    my %switch = (
        'CONNECTED' => sub {
            Connected($self, $receivedData);
            EnableGui( $self );
        },
        'MEASUREMENTS' => sub {
            UpdateReadings($self, $receivedData->{MEASUREMENTS});
            EnableGui( $self );
        },
        'DESCRIPTIONS' => sub {
            CreateGetSetServerCommands($self, $receivedData->{DESCRIPTIONS});
            EnableGui( $self );
        },
        'CONNECTION_STATE' => sub {
            InterpreteConnectionState($self, $receivedData->{CONNECTION_STATE});
        },
        'SOLVIS_STATE' => sub {
            InterpreteSolvisState($self, $receivedData->{SOLVIS_STATE});
            EnableGui( $self );
        }
    );

    if ( defined($switch{ $command  })) {
        $switch{ $command  }->();
    } else {
        Log($self, 3, "Warning: Unknown command $command");
    }

    return;
} # end ExecuteCommand



##########################################
#
#       Connected with the server
#
sub Connected {
    my $self = shift;
    my $receivedData = shift;

    my $connected = $receivedData->{CONNECTED};

    $self->{CLIENT_ID} = $connected->{ClientId};

    if ( defined ($connected->{ServerVersion}) ) {

        $self->{VERSION_SERVER} = $connected->{ServerVersion};

        my $formatVersion = $connected->{FormatVersion}*1.0;
		
		$self->{DATA_FORMAT} = $formatVersion;
		
        if ( $formatVersion < MIN_SUPPORTED_FORMAT ) {
            Log($self, 3, "Format version $formatVersion of server is not supported, use a newer server, if available.");
            $self->{INFO} = 'Format version is too old';
        }
        elsif ( $formatVersion >= MAX_SUPPORTED_FORMAT ) {
            Log($self, 3, "Format version $formatVersion of server is not supported, use a newer client, if available.");
            $self->{INFO} = 'Format version is too new';
        }
        elsif ( $formatVersion < MIN_RECOMMENDED_FORMAT ) {
            Log($self, 3, "Format version $formatVersion of server is deprecated, use a newer server, if available.");
            $self->{INFO} = 'Format version is deprecated but still supported. A new Server ist recommended.';
        }

        my $buildDate = '';
        if ( defined($connected->{BuildDate})) {
            $buildDate = ", build date: $connected->{BuildDate}";
        }

        Log($self, 3, "Server version: $self->{VERSION_SERVER}$buildDate, Format version: $formatVersion");


    }
    return;
} # end Connected



##########################################
#
#       EnableGui handling
#
sub EnableGui {
    my $self = shift;

    my $attrVal = AttrVal($self->{NAME}, 'GuiCommandsEnabled', 'TRUE');
    my $enabled = $attrVal eq 'TRUE';

    if (!defined($self->{helper}{GuiEnabled}) || $self->{helper}{GuiEnabled} != $enabled ) {

        my $command = $enabled?'GUI_COMMANDS_ENABLE':'GUI_COMMANDS_DISABLE';

        SendServerCommand($self, $command);

        $self->{helper}{GuiEnabled} = $enabled;
        Log($self, 3, "Command <$command> is sent to server");
    }
    return;
} # end EnableGui



##########################################
#
#       Interprete connection status
#
sub InterpreteConnectionState {
    my $self = shift;
    my $state = shift;

    my @keys = keys(%$state);

    my $stateString;
    my $message;

    foreach my $key( keys(%$state)) {

        if ( $key eq 'State') {
            $stateString = $state->{$key };
        } elsif ( $key eq 'Message') {
            $message = $state->{$key };
        }
    }


    Log($self, 5, "Connection status: $stateString");

    my %switch = (

        'CLIENT_UNKNOWN' => sub {
            $self->{helper}{GuiEnabled} = undef;
            $self->{CLIENT_ID} = undef;
            Log($self, 3, "Client unknown: $message");
            ReconnectAfterDismiss($self, RECONNECT_AFTER_UNKNOWN_CLIENT);
        },
        'CONNECTION_NOT_POSSIBLE' => sub {
            $self->{helper}{GuiEnabled} = undef;
            Log($self, 3, "Connection not possible: $message");
            ReconnectAfterDismiss($self, RECONNECT_AFTER_DISMISS);
        },
        'ALIVE' => sub {
            Log($self, 4, 'Alive received');
        },
        'USER_ACCESS_DETECTED' => sub {                                 #in case compatibility to format version 1.x

            readingsSingleUpdate($self,'HumanAccess','user',1);
            Log($self, 3, 'User access detected');
        },
        'SERVICE_ACCESS_DETECTED' => sub {                              #in case compatibility to format version 1.x
            readingsSingleUpdate($self,'HumanAccess','service',1);
            Log($self, 3, 'User access detected');
        },
        'HUMAN_ACCESS_FINISHED' => sub {                                #in case compatibility to format version 1.x
            readingsSingleUpdate($self,'HumanAccess','none',1);
            Log($self, 3, 'User access finished');
        },
        'COMMAND_ERROR' => sub {
            Log($self, 3, "Command error: $message");
        }
    );

    if ( defined($switch{ $stateString  })) {
        $switch{ $stateString  }->();
    } else {
        Log($self, 3, "Connection status unknown: $stateString");
    }

    return;
} # end InterpreteConnectionState



##########################################
#
#       Reconnection nach verbindungsfehler
#
sub ReconnectAfterDismiss {
    my $self = shift;
    my $reconnectionDelay = shift;

    DevIo_CloseDev($self);

    my $timeStamp = gettimeofday() + $reconnectionDelay;
    InternalTimer($timeStamp, \&Reconnect, $self );

    return;
} # end ReconnectAfterDismiss







##########################################
#
#       Interprete solvis state
#
sub InterpreteSolvisState {
    my $self = shift;
    my $state = shift;

    my @keys = keys(%$state);

    my $stateString;
    my $message;

    foreach my $key( keys(%$state)) {
        if ( $key eq 'SolvisState') {
            $stateString = $state->{$key };
        }
    }

    Log($self, 4, "Solvis status: $stateString");

    my %switch = (

        'POWER_OFF' => sub {
            readingsSingleUpdate($self,'state',$stateString,1);
            Log($self, 3, 'Power off detected');
       },
        'REMOTE_CONNECTED' => sub {
            readingsSingleUpdate($self,'state',$stateString,1);
            Log($self, 3, 'Remote connected');
        },
        'SOLVIS_CONNECTED' => sub {
            readingsSingleUpdate($self,'state',$stateString,1);
             Log($self, 3, 'Solvis connected');
       },
        'SOLVIS_DISCONNECTED' => sub {
            readingsSingleUpdate($self,'state',$stateString,1);
             Log($self, 3, 'Solvis disconnected');
        },
        'USER_ACCESS_DETECTED' => sub {
            readingsSingleUpdate($self,'HumanAccess','user',1);
            Log($self, 3, 'User access detected');
        },
        'SERVICE_ACCESS_DETECTED' => sub {
            readingsSingleUpdate($self,'HumanAccess','service',1);
            Log($self, 3, 'User access detected');
        },
        'HUMAN_ACCESS_FINISHED' => sub {
            readingsSingleUpdate($self,'HumanAccess','none',1);
            Log($self, 3, 'User access finished');
        },
    );

    if ( defined($switch{ $stateString  })) {
        $switch{ $stateString  }->();
    } else {
        Log($self, 3, "Connection status unknown: $stateString");
    }

    #DoTrigger($self->{NAME}, $stateString);

    return;
} # end InterpreteSolvisState



##########################################
#
#       Timeout der Verbindung
#
sub WatchDogTimeout {
    my $self = shift;

    Log($self, 4, 'Timeout of connection detected. Try reconnection');
    Reconnect($self);

    return;
} # end WatchDogTimeout



################################################
#
#       Reconnect
#
sub Reconnect {
    my $self = shift;

    Log($self, 4, 'Retry reconnection');
    DevIo_CloseDev($self);
    Connect($self,0);

    return;
} # end Reconnect



#######################################
#
#       Create data for Set and Get commands
#

sub CreateGetSetServerCommands {
    my $self = shift;
    my $descriptions = shift;

    my $device = $self->{NAME};

    my %ChannelDescriptions = ();
    $devicesChannelDescriptions{$device} = \%ChannelDescriptions;
    my $serverCommands = '';
    $devicesServerCommands{$device} = \$serverCommands;
    my @serverCommand_Array = ();
    my $screenCommands = '';
    $devicesScreenCommands{$device} = \$screenCommands;
    my @screenCommand_Array = ();

    foreach my $description( keys(%{$descriptions})) {
        my %descriptionHash = %$descriptions{$description};
        my @keys = keys %descriptionHash;
        my $name = $keys[0];
        Log($self, 5, "Processing of description: $name");
        my %channelHash = %{$descriptionHash{$name}};
        if ( $channelHash{Type} eq 'ServerCommand') {
            #if ( $name ne 'GUI_COMMANDS_DISABLE' && $name ne 'GUI_COMMANDS_ENABLE' ) {
                push(@serverCommand_Array, $name);
            #}
        } elsif ( $channelHash{Type} eq 'SelectScreen') {
            push(@screenCommand_Array, $name);
        } else {
            $ChannelDescriptions{$name} = {};
            $ChannelDescriptions{$name}{SET} = $channelHash{Writeable};
            $ChannelDescriptions{$name}{GET} = $channelHash{Type} eq 'CONTROL';

            Log($self, 5, "Writeable: $channelHash{Writeable}");

            my %switch = (
                'Accuracy' => sub {
                    $ChannelDescriptions{$name}{Accuracy} = $channelHash{Accuracy};
                },
                'Modes' => sub {
                    $ChannelDescriptions{$name}{Modes} = {};
					foreach my $mode (@{$channelHash{Modes}}) {
						if ( $self->{DATA_FORMAT} < 3) {
							$ChannelDescriptions{$name}{Modes}{$mode} = 'BOTH';
						} else {
							my %modeHash = %$mode;
							my $modeName = $modeHash{Name};
							my $handling = $modeHash{Handling};
							#Log($self, 3, "ModeName: $modeName, Handling: $handling");
							$ChannelDescriptions{$name}{Modes}{$modeName} = $handling;
						}
					}
				},
                'Upper' => sub {
                    $ChannelDescriptions{$name}{Upper} = $channelHash{Upper};
                },
                'Lower' => sub {
                    $ChannelDescriptions{$name}{Lower} = $channelHash{Lower};
                },
                'Step' => sub {
                    $ChannelDescriptions{$name}{Step} = $channelHash{Step};
                },
                'IncrementChange' => sub {
                    $ChannelDescriptions{$name}{IncrementChange} = $channelHash{IncrementChange};
                },
                'ChangedIncrement' => sub {
                    $ChannelDescriptions{$name}{ChangedIncrement} = $channelHash{ChangedIncrement};
                },
                'IsBoolean' => sub {
                    $ChannelDescriptions{$name}{IsBoolean} = $channelHash{IsBoolean};
                },
                'Unit' => sub {
                    $ChannelDescriptions{$name}{Unit} = $channelHash{Unit};
                }
            );
            foreach my $keyName ( keys %channelHash ) {
                if ( defined($switch{ $keyName  })) {
                    $switch{ $keyName  }->();
                }
            }
        }
    }


    $serverCommands = join(',',sort(@serverCommand_Array));
    $screenCommands = join(',',sort(@screenCommand_Array));

    CreateSetParams($self);

    return;

} # end CreateGetSetServerCommands



#######################################
#
#       Create data for Set Parameters
#
sub CreateSetParams {

    my $self = shift;
    my $device = $self->{NAME};

    my $setParameters = '';
    $devicesSetParameters{$device} = \$setParameters;

    my %ChannelDescriptions = %{$devicesChannelDescriptions{$device}};

    my @channels = keys(%ChannelDescriptions);
    my $firstO = _TRUE_;
    foreach my $channel (@channels) {
        if ( $ChannelDescriptions{$channel}{SET} == _FALSE_ ) {
            next;
        }
        if ( ! $firstO ) {
            $setParameters .= ' ';
        } else {
            $firstO = _FALSE_;
        }
        $setParameters .= $channel;
        my $firstI = _TRUE_;
        if ( defined ($ChannelDescriptions{$channel}{Modes}) ) {
			my %modeHash = %{$ChannelDescriptions{$channel}{Modes}};
            foreach my $mode (keys(%modeHash)) {
				my $handling = $modeHash{$mode};
				#Log($self, 3, "Handling: $handling");
				if ( $handling =~ m/(BOTH)|(WRITE)/) {
					if($firstI) {
						$setParameters .= ':';
						$firstI = _FALSE_;
					} else {
						$setParameters .= ','
					}
					$setParameters .=$mode;
				}
            }
        } elsif ( defined ($ChannelDescriptions{$channel}{Upper}) ) {
            my $upper = $ChannelDescriptions{$channel}{Upper};
            my $step = $ChannelDescriptions{$channel}{Step};
            my $incrementChange ;
            my $changedStep ;

            if ( defined ($ChannelDescriptions{$channel}{IncrementChange})) {
                $incrementChange = $ChannelDescriptions{$channel}{IncrementChange};
                $changedStep = $ChannelDescriptions{$channel}{ChangedIncrement};
            } else {
                $incrementChange = $upper;
                $changedStep = $step;
            }


            for ( my $count = $ChannelDescriptions{$channel}{Lower} ; $count <= $upper ; $count += $count>=$incrementChange? $changedStep: $step) {
                if($firstI) {
                    $setParameters .= ':';
                    $firstI = _FALSE_;
                } else {
                    $setParameters .= ','
                }
                $setParameters .=$count;
            }

        } elsif ( defined ($ChannelDescriptions{$channel}{IsBoolean}) && $ChannelDescriptions{$channel}{IsBoolean} != _FALSE_ ) {
            $setParameters .= ':off,on';
        }
    }
    return;
}



######################################
#
#       Update Readings
#
sub UpdateReadings {
    my $self = shift;
    my $readings = shift;

    my $device = $self->{NAME};

    my %ChannelDescriptions = %{$devicesChannelDescriptions{$device}};

    readingsBeginUpdate($self);

    foreach my $readingName( keys(%$readings)) {
        if ( defined( $readings->{$readingName} )) {
            my $value = $readings->{$readingName};
            if ( $ChannelDescriptions{$readingName}{IsBoolean} != _FALSE_ ) {
                $value = $value?'on':'off';
            }

            readingsBulkUpdate($self,$readingName,$value);
        }
    }
    readingsEndUpdate($self, 1);

    return;
} # end UpdateReadings



######################################
#
#       Undefine
#
sub Undef {
    my $self = shift;
    my $args = shift;

    DevIo_CloseDev($self);

    return;
} # end Undef



#####################################
#
#       Delete module
#
sub Delete {
    my $self = shift;
    my $args = shift;

    return;
}# end Delete



#####################################
#
#       send json package
#
sub SendData {
    my $self = shift;
    my $command = shift;
    my $key  = shift;
    my $val  = shift;

    my %sendPackage = (
        $command => {
            $key => $val
        }
    );

    my $byteString = encode_json ( \%sendPackage );

    my $length = length $byteString;
#    $byteString = pack('CCCa*', $length&0xff, $length>>8&0xff, $length>>16&0xff, $byteString );
    $byteString = pack('CCCa*', $length>>16&0xff, $length>>8&0xff, $length&0xff, $byteString );

    Log($self, 5, "ByteString: $byteString");

    DevIo_SimpleWrite($self, $byteString, 0);

    return;
}# end SendData



#####################################
#
#       send set data
#
sub SendSetData {
    my $self = shift;
    my $channel = shift;
    my $data = shift;

    SendData( $self, 'SET', $channel, $data );

    return;
}# end SendSetData



#####################################
#
#       send get data
#
sub SendGetData {
    my $self = shift;
    my $channel = shift;

    SendData( $self, 'GET', $channel, undef );

    return;
}# end SendGetData



#####################################
#
#       Set command
#
sub Set {
    my ( $self, $name, @aa ) = @_;
    my ( $cmd, @args ) = @aa;

    my $device = $self->{NAME};

#    Log($self, 3, "Device: $device");
#    Log($self, 3, "Hash Skalar: %devicesChannelDescriptions");
#    Log($self, 3, "Skalar: $devicesChannelDescriptions{$device}");

    if ( ! defined($devicesChannelDescriptions{$device})) {
        return "Device  $device currently not connected.";
    }

    my %ChannelDescriptions = %{$devicesChannelDescriptions{$device}};
    my $setParameters = ${$devicesSetParameters{$device}};
    my $serverCommands = ${$devicesServerCommands{$device}};
    my $screenCommands = ${$devicesScreenCommands{$device}};

    if ( $cmd eq '?' ) {
        return "unknown argument $cmd choose one of $setParameters ServerCommand:$serverCommands SelectScreen:$screenCommands,NONE";
    }

    ### If not enough arguments have been provided
    if ( @args < 1 ) {
        return "\"set $self->{NAME}\" needs at least two arguments";
    }

    if ( $cmd eq 'ServerCommand') {

        my $serverCommand = $args[0];

        SendServerCommand($self, $serverCommand);

    } elsif ( $cmd eq 'SelectScreen') {

        my $screenCommand = $args[0];

        SendScreenCommand($self, $screenCommand);

    } else {


        my $channel = $cmd;
        my $value = $args[0];

        Log($self, 4, "Set entered, device := $name, Cannel := $channel, Value := $value");

        if ( defined($ChannelDescriptions{$channel}) ) {
            if ( defined ($ChannelDescriptions{$channel}{Modes}) ) {
                if ( ! defined ($ChannelDescriptions{$channel}{Modes}{$value})) {
                    my @modes = keys(%{$ChannelDescriptions{$channel}{Modes}});
                    Log($self, 5, 'Mode 1: '.join(' ', $modes[0]));
                    return "unknown value $value choose one of " . join(' ', @modes);
                }
            } elsif ( defined ($ChannelDescriptions{$channel}{IsBoolean}) && $ChannelDescriptions{$channel}{IsBoolean} != _FALSE_ ) {
                if ( $value eq "on" ) {
                    $value = \1;
                } elsif ( $value eq "off" ) {
                    $value = \0;
                } else {
                    return "unknown value $value choose one of on off";
                }
            } else {
                my $factor = int(1.0 / $ChannelDescriptions{$channel}{Accuracy} + 0.5);
                $value = int($value * $factor + 0.5);
                $value /= $factor;
            }
            SendSetData($self, $channel, $value);
        } else {
            my @channels = keys(%ChannelDescriptions);
            Log($self, 5, 'Channels: '.join(' ', @channels));
            return "unknown argument $channel choose one of " . join(' ', @channels);
        }
    }

    return;
} # end Set



#####################################
#
#       Send server command
#
sub SendServerCommand {
    my $self = shift;
    my $command = shift;

    SendData($self, 'SERVER_COMMAND', 'Command', $command);

    return;
} # end SendServerCommand



#####################################
#
#       Send screen command
#
sub SendScreenCommand {
    my $self = shift;
    my $screen = shift;

    SendData($self, 'SELECT_SCREEN', 'Screen', $screen);

    return;
} # end SendServerCommand



#####################################
#
#      Get
#
sub Get {

    my $self = shift;
    my $name = shift;
    my $opt = shift;
    #my @args = shift;

    my $device = $self->{NAME};

    if ( ! defined($devicesChannelDescriptions{$device})) {
        return "Device  $device currently not connected.";
    }

    my %ChannelDescriptions = %{$devicesChannelDescriptions{$device}};

   ### If not enough arguments have been provided
    if ( !defined($opt) ) {
        return '\"get Solvis\" needs at least one argument';
    }

    my $channel = $opt;

    if ( $channel eq '?' || ! defined($ChannelDescriptions{$channel} ) ) {
        my @channels = keys(%ChannelDescriptions);
        my $params = '';
        my $firstO = _TRUE_;
        foreach my $channel (@channels) {
            if ( $ChannelDescriptions{$channel}{GET} == _FALSE_ ) {
                next;
            }
            if ( ! $firstO ) {
                $params .= ' ';
            } else {
                $firstO = _FALSE_;
            }
            $params .= $channel;
        }
        return "unknown argument $channel choose one of $params";
    } else {

        Log($self, 4, "Get entered, device := $name, Cannel := $channel");
        SendGetData($self, $channel);

        return 'The reading process was started.\nThe value will be output only in the readings.';
    }

    return;

} # end Get
#



#####################################
#
#      DbLog event interpretation
#
sub DbLog_splitFn {
    my $event = shift;
    my $device = shift;

    my %ChannelDescriptions = %{$devicesChannelDescriptions{$device}};

    my ($reading, $value, $unit);

    my @splited = split(/ /,$event);

    $reading = $splited[0];;
    $reading =~ tr/://d;

    $unit = '';

    if ( defined( $ChannelDescriptions{$reading}{Unit} ) ) {
        $unit = $ChannelDescriptions{$reading}{Unit};
        $value = $splited[1];
    }
    return ($reading, $value, $unit);
} # end DbLog_splitFn

1;

###START###### Description for fhem commandref ################################################################START####
=pod

=item device
=item summary       Module for controlling solvis heating system
=item summary_DE    Modul zur Steuerung einer Solvis Heizungsanlage

=begin html_DE

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
        Zur Nutzung des SolvisClient-Moduls, muss man mit dem
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
            <tr><td align="right" valign="top"><code>X01.BrennerStarts</code> : </td><td align="left" valign="top">Anzahl der Brennerstarts der Stufe 1</td></tr>
            <tr><td align="right" valign="top"><code>X03.BrennerStufe2Starts</code> : </td><td align="left" valign="top">Anzahl der Brennerstarts der Stufe 2</td></tr>
            <tr><td align="right" valign="top"><code>X02.BrennerLaufzeit_s</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 1</td></tr>
            <tr><td align="right" valign="top"><code>X04.BrennerStufe2Laufzeit_s</code> : </td><td align="left" valign="top">Laufzeit des Brenners in der Stufe 2</td></tr>
            <tr><td align="right" valign="top"><code>X05.BrennerStatus</code> : </td><td align="left" valign="top">Aktuelle Brennstufe des Brenners (off, Stufe1 oder Stufe2)</td></tr>
            <tr><td align="right" valign="top"><code>X06.UhrzeitSolvis</code> : </td><td align="left" valign="top">Uhrzeit der SolvisControl</td></tr>
            <tr><td align="right" valign="top"><code>X07.MischerPosition0_HK1</code> : </td><td align="left" valign="top">Mischer des Heizkreises 1 in Ruhestellung</td></tr>
            <tr><td align="right" valign="top"><code>X08.MischerPosition0_HK2</code> : </td><td align="left" valign="top">Mischer des Heizkreises 2 in Ruhestellung</td></tr>
            <tr><td align="right" valign="top"><code>X09.LaufzeitSolarpumpe_s</code> : </td><td align="left" valign="top">Laufzeit der Solarpumpe (A01)</td></tr>
            <tr><td align="right" valign="top"><code>X10.LaufzeitSolarpumpe2_s</code> : </td><td align="left" valign="top">Laufzeit der Solarpumpe 2 (A07)</td></tr>
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
            <tr><td align="right" valign="top"><code>C24.LaufzeitSolarpumpe</code> : </td><td align="left" valign="top">Laufzeit der Solarpumpe (A01)</td></tr>
            <tr><td align="right" valign="top"><code>C25.LaufzeitSolarpumpe2</code> : </td><td align="left" valign="top">Laufzeit der Solarpumpe 2 (A02)</td></tr>
            <tr><td align="right" valign="top"><code>C26.Warmwasserzirkulation_Puls</code> : </td><td align="left" valign="top">Warmwasserzirkulation Modus Puls </td></tr>
            <tr><td align="right" valign="top"><code>C27.Warmwasserzirkulation_Zeit</code> : </td><td align="left" valign="top">Warmwasserzirkulation Modus Zeit </td></tr>
            <tr><td align="right" valign="top"><code>C28.WW_Pumpe_Min_Laufzeit</code> : </td><td align="left" valign="top">Warmwasserzirkulation minimale Pulszeit </td></tr>
            <tr><td align="right" valign="top"><code>C29.BetriebsartVT_HK1</code> : </td><td align="left" valign="top">Algorithmus Vorlauftemperatur Heizkreis 1 </td></tr>
            <tr><td align="right" valign="top"><code>C30.Steilheit_HK1</code> : </td><td align="left" valign="top">Steilheit Vorlauftemperatur Heizkreis 1</td></tr>
            <tr><td align="right" valign="top"><code>C31.Fix_Vorlauf_Tag_HK1</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Tag Heizkreis 1 </td></tr>
            <tr><td align="right" valign="top"><code>C32.Fix_Vorlauf_Absenk_HK1</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Nacht Heizkreis 1 </td></tr>
            <tr><td align="right" valign="top"><code>C33.Min_Vorlauf_Temp_HK1</code> : </td><td align="left" valign="top">Minimale Vorlauftemperatur Heizkreis 1 </td></tr>
            <tr><td align="right" valign="top"><code>C34.BetriebsartVT_HK2</code> : </td><td align="left" valign="top">Algorithmus Vorlauftemperatur Heizkreis 2 </td></tr>
            <tr><td align="right" valign="top"><code>C35.Steilheit_HK2</code> : </td><td align="left" valign="top">Steilheit Vorlauftemperatur Heizkreis 2</td></tr>
            <tr><td align="right" valign="top"><code>C36.Fix_Vorlauf_Tag_HK2</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Tag Heizkreis 2 </td></tr>
            <tr><td align="right" valign="top"><code>C37.Fix_Vorlauf_Absenk_HK2</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Nacht Heizkreis 2 </td></tr>
            <tr><td align="right" valign="top"><code>C38.Min_Vorlauf_Temp_HK2</code> : </td><td align="left" valign="top">Minimale Vorlauftemperatur Heizkreis 2 </td></tr>
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
                    <tr><td align="right" valign="top"><code>C26.Warmwasserzirkulation_Puls</code> : </td><td align="left" valign="top">Warmwasserzirkulation Modus Puls </td></tr>
                    <tr><td align="right" valign="top"><code>C27.Warmwasserzirkulation_Zeit</code> : </td><td align="left" valign="top">Warmwasserzirkulation Modus Zeit </td></tr>
                    <tr><td align="right" valign="top"><code>C28.WW_Pumpe_Min_Laufzeit</code> : </td><td align="left" valign="top">Warmwasserzirkulation minimale Pulszeit </td></tr>
                    <tr><td align="right" valign="top"><code>C29.BetriebsartVT_HK1</code> : </td><td align="left" valign="top">Algorithmus Vorlauftemperatur Heizkreis 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C30.Steilheit_HK1</code> : </td><td align="left" valign="top">Steilheit Vorlauftemperatur Heizkreis 1</td></tr>
                    <tr><td align="right" valign="top"><code>C31.Fix_Vorlauf_Tag_HK1</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Tag Heizkreis 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C32.Fix_Vorlauf_Absenk_HK1</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Nacht Heizkreis 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C33.Min_Vorlauf_Temp_HK1</code> : </td><td align="left" valign="top">Minimale Vorlauftemperatur Heizkreis 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C34.BetriebsartVT_HK2</code> : </td><td align="left" valign="top">Algorithmus Vorlauftemperatur Heizkreis 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C35.Steilheit_HK2</code> : </td><td align="left" valign="top">Steilheit Vorlauftemperatur Heizkreis 2</td></tr>
                    <tr><td align="right" valign="top"><code>C36.Fix_Vorlauf_Tag_HK2</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Tag Heizkreis 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C37.Fix_Vorlauf_Absenk_HK2</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Nacht Heizkreis 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C38.Min_Vorlauf_Temp_HK2</code> : </td><td align="left" valign="top">Minimale Vorlauftemperatur Heizkreis 2 </td></tr>
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
            <tr><td align="right" valign="top"><code>COMMAND_OPTIMIZATION_ENABLE</code>: </td><td align="left" valign="top">
              Normalerweise werden die Control-Kommandos in einer optimierten Reihenfolge ausgef&uuml;hrt. Befehle, welche im gleichen Screen Aktionen ausl&ouml;sen, werden zusammen gefasst ausgef&uuml;hrt. Falls eine strikte Einhaltung der Befehls-Sequenz erdorderlich ist,
              hann das durch diesen Server-Befehl verhindert werden<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>COMMAND_OPTIMIZATION_INHIBIT</code>: </td><td align="left" valign="top">
              Gegenstück zu dem ServerCommand COMMAND_OPTIMIZATION_ENABLE<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>GUI_COMMANDS_ENABLE</code>: </td><td align="left" valign="top">
              Mithlife dieses Befehls kann die GUI-Steuerung tempor&auml;r deaktiviert werden, z.B. wenn die Wartung der Anlage erfolgt. Steuert man dies &uuml;ber die FHEM-Oberfl&auml;che, so ist ein SET-Befehl g&uuml;nstiger. Aktiviert/Deaktiviert man
              die h&auml;ndisch, sollte man das entsprechende Attribut setzen.<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>GUI_COMMANDS_DISABLE</code>: </td><td align="left" valign="top">
              Gegenstück zu dem ServerCommand GUI_COMMANDS_ENABLE<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>SERVICE_RESET</code>: </td><td align="left" valign="top">
              Setzt einen erkannten Service-Zugriff zur&uumlck.<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>UPDATE_CHANNELS</code>: </td><td align="left" valign="top">
              Update aller Kan&auml;le, welche nur &uuml;ber das GUI zug&auml;lich sind<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>RESTART</code>: </td><td align="left" valign="top">
              Startet den Server neu<BR>
            </td></tr>
          </table>
        </ul><BR>
            Diese Tabelle gibt nicht unbedingt den aktuellen Stand wieder. Es k&ouml;nnen mehr oder weniger Server-Befehle definiert sein, da die Server-Befehle vom Server selber dem Client &uuml;bergeben werden (bei jeder neuen Verbindung). Der Server selber bestimmt daher, was
            der Client anbietet. Maßgebend ist daher immer die Ausgabe in der Web-Oberfl&auml;che.
            <BR><BR>
            Zus&auml;tzlich kann mittels dem SET-Befehl "ScreenSelect" ein bestimmter Solvis-Bildschirm ausgew&auml;hlt werden, der default-m&auml;&szlig;ig angefahren wird. Die m&ouml;glichen Bildschirm-Namen
            sind der Web-Oberfl&auml;che zu entnehmen.
        </ul>
      </ul><BR><BR><BR>
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
                    <tr><td align="right" valign="top"><code>C24.LaufzeitSolarpumpe</code> : </td><td align="left" valign="top">Laufzeit der Solarpumpe (A01)</td></tr>
                    <tr><td align="right" valign="top"><code>C25.LaufzeitSolarpumpe2</code> : </td><td align="left" valign="top">Laufzeit der Solarpumpe 2 (A02)</td></tr>
                    <tr><td align="right" valign="top"><code>C26.Warmwasserzirkulation_Puls</code> : </td><td align="left" valign="top">Warmwasserzirkulation Modus Puls </td></tr>
                    <tr><td align="right" valign="top"><code>C27.Warmwasserzirkulation_Zeit</code> : </td><td align="left" valign="top">Warmwasserzirkulation Modus Zeit </td></tr>
                    <tr><td align="right" valign="top"><code>C28.WW_Pumpe_Min_Laufzeit</code> : </td><td align="left" valign="top">Warmwasserzirkulation minimale Pulszeit </td></tr>
                    <tr><td align="right" valign="top"><code>C29.BetriebsartVT_HK1</code> : </td><td align="left" valign="top">Algorithmus Vorlauftemperatur Heizkreis 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C30.Steilheit_HK1</code> : </td><td align="left" valign="top">Steilheit Vorlauftemperatur Heizkreis 1</td></tr>
                    <tr><td align="right" valign="top"><code>C31.Fix_Vorlauf_Tag_HK1</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Tag Heizkreis 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C32.Fix_Vorlauf_Absenk_HK1</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Nacht Heizkreis 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C33.Min_Vorlauf_Temp_HK1</code> : </td><td align="left" valign="top">Minimale Vorlauftemperatur Heizkreis 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C34.BetriebsartVT_HK2</code> : </td><td align="left" valign="top">Algorithmus Vorlauftemperatur Heizkreis 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C35.Steilheit_HK2</code> : </td><td align="left" valign="top">Steilheit Vorlauftemperatur Heizkreis 2</td></tr>
                    <tr><td align="right" valign="top"><code>C36.Fix_Vorlauf_Tag_HK2</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Tag Heizkreis 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C37.Fix_Vorlauf_Absenk_HK2</code> : </td><td align="left" valign="top">Vorlauftemperatur Fix Nacht Heizkreis 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C38.Min_Vorlauf_Temp_HK2</code> : </td><td align="left" valign="top">Minimale Vorlauftemperatur Heizkreis 2 </td></tr>
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
                    <tr><td align="right" valign="top"><code>GuiCommandsEnabled</code> : </td><td align="left" valign="top">TRUE/FALSE: Gui-Befehle sind enabled/disabled. Im Service-Fall empfiehlt es sich dieses Atrribut auf FALSE zu setzen, damit der Service nicht von dem SolvisSmartHomeServer irritiert wird. Default: TRUE</td></tr>
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
                  Der Name des Wertes, welcher gesetzt werden soll.<BR></td></tr>
              <tr><td align="right" valign="top"><code>&lt;value&gt;</code> : </td><td align="left" valign="top">
                  Ein g&uuml;ltiger Wert.<BR></td></tr>
          </table><BR><BR>
        </ul>
      </ul>

      <table>
        <tr><td><a name="SolvisClientState"></a><b>state</b></td></tr>
        <tr><td>
          <ul>
              Das spezielle Reading "state" kann folgende Werte annehmen:<BR>
            <ul>
              <ul>
                <table>
                    <tr><td align="right" valign="top"><code>disconnected</code> : </td><td align="left" valign="top">Server ist nicht mit FHEM verbunden.</td></tr>
                    <tr><td align="right" valign="top"><code>opened</code> : </td><td align="left" valign="top">Die Verbindung zum Server wird aufgebaut.</td></tr>
                    <tr><td align="right" valign="top"><code>SOLVIS_DISCONNECTED</code> : </td><td align="left" valign="top">Die Verbindung zum Server hergestellt, jedoch keine zur SolvisRemote</td></tr>
                    <tr><td align="right" valign="top"><code>REMOTE_CONNECTED</code> : </td><td align="left" valign="top">Die Verbindung zum Server und SolvisRemote hergestellt. Keine Verbindung zur Heizungsanlage.</td></tr>
                    <tr><td align="right" valign="top"><code>SOLVIS_CONNECTED</code> : </td><td align="left" valign="top">Alle Verbindungen hergestellt.</td></tr>
                    <tr><td align="right" valign="top"><code>POWER_OFF</code> : </td><td align="left" valign="top">Solvis-Anlage ist offenbar ausgeschaltet.</td></tr>
                    <tr><td align="right" valign="top"><code>ERROR</code> : </td><td align="left" valign="top">SolvisControl zeigt eine Fehlermeldung an.</td></tr>
                </table>
              </ul>
            </ul><BR>
          </ul>
        </td></tr>
      </table>

      <table>
        <tr><td><a name="SolvisClientHumanAccess"></a><b>HumanAccess</b></td></tr>
        <tr><td>
          <ul>
              Das spezielle Reading "HumanAccess" kann folgende Werte annehmen:<BR>
            <ul>
              <ul>
                <table>
                    <tr><td align="right" valign="top"><code>none</code> : </td><td align="left" valign="top">Kein externer Zugriff erkannt</td></tr>
                    <tr><td align="right" valign="top"><code>service</code> : </td><td align="left" valign="top">Zugriff vom Service</td></tr>
                    <tr><td align="right" valign="top"><code>user</code> : </td><td align="left" valign="top">Zugriff vom User</td></tr>
                </table>
              </ul>
            </ul><BR>
          </ul>
        </td></tr>
      </table>
    </td></tr>
  </table>
</ul>
=end html_DE

=begin html

<a name="SolvisClient"></a>
<h3>SolvisClient</h3>
<ul>
  <table>
    <tr><td>
        In order to be able to access the
        <a href="https://www.solvis.de/solvisben">SolvisBen</a> or <a href="https://www.solvis.de/solvismax">SolvisMax</a>
        solar heating systems using FHEM, the <a href="https://www.solvis.de/solvisremote">SolvisRemote</a> is required. It is a communication device with a
        <a href="https://s3.eu-central-1.amazonaws.com/solvis-files/seiten/produkte/solvisremote/remote-mobile.png">web interface</a>.<BR>.
        <BR>
        To use the SolvisClient module, you must first define the IP address, the user and the password with the
        <a href="https://s3.eu-central-1.amazonaws.com/solvis-files/seiten/produkte/solvisremote/Download/konfig-remote.zip">configuration program</a>.<BR>
        The router can optionally specify the IP address, but then a fixed IP address should be defined in the router for the
        <a href="https://www.solvis.de/solvisremote">SolvisRemote</a>
        so that the SolvisSmartHome server can also find it.<BR>
        <BR>
        In addition to the <a href="https://www.solvis.de/solvisremote">SolvisRemote</a>, the SolvisSmartHome server must also be installed and set up, which is part of this FHEM module. According to the enclosed instructions, this must first be set up and the system parameters defined there.<BR>
        <BR>
        A connection to the server is then attempted with the define instruction of the FHEM module. Depending on whether the connection was successful, a corresponding message appears in the status field.<BR>
        If the connection is successful, the client is regularly supplied with data from the server by the heating system.<BR>
        <BR>
        In addition to recording these values, the server also uses these values ​​to calculate the burner starts and burner runtime for the two burner stages. In addition, a reading of the burner status and the rest position of the mixer are calculated.<BR>
        <BR>
        The following readings are also calculated:
      <ul>
        <ul>
          <table>
            <tr><td align="right" valign="top"><code>X01.BrennerStarts</code> : </td><td align="left" valign="top">Number of burner starts of level 1</td></tr>
            <tr><td align="right" valign="top"><code>X03.BrennerStufe2Starts</code> : </td><td align="left" valign="top">Number of burner starts of level 2</td></tr>
            <tr><td align="right" valign="top"><code>X02.BrennerLaufzeit_s</code> : </td><td align="left" valign="top">Burner runtime in level 1</td></tr>
            <tr><td align="right" valign="top"><code>X04.BrennerStufe2Laufzeit_s</code> : </td><td align="left" valign="top">Burner runtime in level 2</td></tr>
            <tr><td align="right" valign="top"><code>X05.BrennerStatus</code> : </td><td align="left" valign="top">Current burner firing level (off, Stufe1 oder Stufe2)</td></tr>
            <tr><td align="right" valign="top"><code>X06.UhrzeitSolvis</code> : </td><td align="left" valign="top">Time of the SolvisControl</td></tr>
            <tr><td align="right" valign="top"><code>X07.MischerPosition0_HK1</code> : </td><td align="left" valign="top">Mixer of heating circuit 1 in the rest position</td></tr>
            <tr><td align="right" valign="top"><code>X08.MischerPosition0_HK2</code> : </td><td align="left" valign="top">Mixer of heating circuit 2 in the rest position</td></tr>
             <tr><td align="right" valign="top"><code>X09.LaufzeitSolarpumpe_s</code> : </td><td align="left" valign="top">Running time of the solar pump (A01)</td></tr>
            <tr><td align="right" valign="top"><code>X10.LaufzeitSolarpumpe2_s</code> : </td><td align="left" valign="top">Running time of the solar pump 2 (A07)</td></tr>
         </table>
        </ul>
      </ul><BR><BR>
        There are also other values that are determined from the SolvisControl GUI using OCR. These are only updated if the value is read / changed using a GET / SET command. So they are not always up to date.<BR>
        <BR>
        The following readings are determined from the GUI:
      <ul>
        <ul>
          <table>
            <tr><td align="right" valign="top"><code>C01.StartsBrenner</code> : </td><td align="left" valign="top">Number of burner starts of level 1</td></tr>
            <tr><td align="right" valign="top"><code>C02.LaufzeitBrenner</code> : </td><td align="left" valign="top">Burner runtime in level 1 (in h)</td></tr>
            <tr><td align="right" valign="top"><code>C03.LaufzeitAnforderung2</code> : </td><td align="left" valign="top">Burner runtime in stage 2 (in h)</td></tr>
            <tr><td align="right" valign="top"><code>C04.WarmwasserPumpe</code> : </td><td align="left" valign="top">hot water pump (on / off / auto)</td></tr>
            <tr><td align="right" valign="top"><code>C05.WassertemperaturSoll</code> : </td><td align="left" valign="top">target temperature hot water (10 .. 65&deg;C)</td></tr>
            <tr><td align="right" valign="top"><code>C06.Anlagenmodus_HK1</code> : </td><td align="left" valign="top">Mode of heating circuit 1 (Tag/Nacht/Standby/Timer)</td></tr>
            <tr><td align="right" valign="top"><code>C07.Tagestemperatur_HK1</code> : </td><td align="left" valign="top">Set temperature day heating circuit 1</td></tr>
            <tr><td align="right" valign="top"><code>C08.Nachttemperatur_HK1</code> : </td><td align="left" valign="top">Set temperature night heating circuit 1</td></tr>
            <tr><td align="right" valign="top"><code>C09.TemperaturFeineinstellung_HK1</code> : </td><td align="left" valign="top">Fine temperature adjustment heating circuit 1 (-5 ... 5)</td></tr>
            <tr><td align="right" valign="top"><code>C10.Raumeinfluss_HK1</code> : </td><td align="left" valign="top">Room influence of heating circuit 1 (0 ... 90%)</td></tr>
            <tr><td align="right" valign="top"><code>C11.Vorlauf_Soll_HK1</code> : </td><td align="left" valign="top">Setpoint of the flow temperature heating circuit 1</td></tr>
            <tr><td align="right" valign="top"><code>C12.Anlagenmodus_HK2</code> : </td><td align="left" valign="top">Mode of heating circuit 2 (Tag/Nacht/Standby/Timer)</td></tr>
            <tr><td align="right" valign="top"><code>C13.Tagestemperatur_HK2</code> : </td><td align="left" valign="top">target temperature day heating circuit 2</td></tr>
            <tr><td align="right" valign="top"><code>C14.Nachttemperatur_HK2</code> : </td><td align="left" valign="top">Set temperature night heating circuit 2</td></tr>
            <tr><td align="right" valign="top"><code>C15.TemperaturFeineinstellung_HK2</code> : </td><td align="left" valign="top">Fine temperature adjustment heating circuit 2 (-5 ... 5)</td></tr>
            <tr><td align="right" valign="top"><code>C16.Raumeinfluss_HK2</code> : </td><td align="left" valign="top">room influence of heating circuit 2 (0 ... 90%)</td></tr>
            <tr><td align="right" valign="top"><code>C17.Vorlauf_Soll_HK2</code> : </td><td align="left" valign="top">Setpoint of the flow temperature heating circuit 2</td></tr>
            <tr><td align="right" valign="top"><code>C24.LaufzeitSolarpumpe</code> : </td><td align="left" valign="top">Running time of the solar pump (A01)</td></tr>
            <tr><td align="right" valign="top"><code>C25.LaufzeitSolarpumpe2</code> : </td><td align="left" valign="top">Running time of the solar pump 2 (A02)</td></tr>
            <tr><td align="right" valign="top"><code>C26.Warmwasserzirkulation_Puls</code> : </td><td align="left" valign="top">DHW circulation mode pulse </td></tr>
            <tr><td align="right" valign="top"><code>C27.Warmwasserzirkulation_Zeit</code> : </td><td align="left" valign="top">DHW circulation mode time </td></tr>
            <tr><td align="right" valign="top"><code>C28.WW_Pumpe_Min_Laufzeit</code> : </td><td align="left" valign="top">DHW circulation minimum pulse time </td></tr>
            <tr><td align="right" valign="top"><code>C29.BetriebsartVT_HK1</code> : </td><td align="left" valign="top">Algorithm flow temperature heating circuit 1 </td></tr>
            <tr><td align="right" valign="top"><code>C30.Steilheit_HK1</code> : </td><td align="left" valign="top">Flow temperature slope, heating circuit 1</td></tr>
            <tr><td align="right" valign="top"><code>C31.Fix_Vorlauf_Tag_HK1</code> : </td><td align="left" valign="top">Fixed flow temperature day heating circuit 1 </td></tr>
            <tr><td align="right" valign="top"><code>C32.Fix_Vorlauf_Absenk_HK1</code> : </td><td align="left" valign="top">Fixed flow temperature night heating circuit 1 </td></tr>
            <tr><td align="right" valign="top"><code>C33.Min_Vorlauf_Temp_HK1</code> : </td><td align="left" valign="top">Minimum flow temperature heating circuit 1 </td></tr>
            <tr><td align="right" valign="top"><code>C34.BetriebsartVT_HK2</code> : </td><td align="left" valign="top">Algorithm flow temperature heating circuit 2 </td></tr>
            <tr><td align="right" valign="top"><code>C35.Steilheit_HK2</code> : </td><td align="left" valign="top">Flow temperature slope, heating circuit 2</td></tr>
            <tr><td align="right" valign="top"><code>C36.Fix_Vorlauf_Tag_HK2</code> : </td><td align="left" valign="top">Fixed flow temperature day heating circuit 2 </td></tr>
            <tr><td align="right" valign="top"><code>C37.Fix_Vorlauf_Absenk_HK2</code> : </td><td align="left" valign="top">Fixed flow temperature night heating circuit 2 </td></tr>
            <tr><td align="right" valign="top"><code>C38.Min_Vorlauf_Temp_HK2</code> : </td><td align="left" valign="top">Minimum flow temperature heating circuit 2 </td></tr>
          </table>
        </ul>
      </ul><BR><BR><BR><BR>
        The module can be integrated and parameterized in FHEM as follows:
        <BR><BR>

      <table>
        <tr><td><a name="SolvisClientDefine"></a><b>Define</b></td></tr>
      </table>

      <table>
        <tr><td><ul><code>define &lt;device&gt; SolvisClient &lt;url&gt;</code></ul></td></tr>
      </table>

      <ul>
        <ul>
          <table>
            <tr><td align="right" valign="top"><code>&lt;device&gt;</code> :</td><td align="left" valign="top">
              The name of the device. Recommendation: "mySolvisMax".</td></tr>
            <tr><td align="right" valign="top"><code>&lt;url&gt;</code> :</td><td align="left" valign="top">
              A valid url (IP address or internet path) of the SolvisSmartHome server with channel number (usually 10735). Possibly look up in the router which IP address the SolvisSmartHome server was assigned by the DHCP server. If the server is running on the Fhem system, "localhost: 10735" can also be entered.</td></tr>
          </table>
        </ul>
      </ul><BR><BR>

      <table>
        <tr><td><a name="SolvisClientSet"></a><b>Set</b></td></tr>
        <tr><td>
          <ul>
            The set function changes a subset of the system values ​​of the Solvis. The following values ​​can currently be changed:
            <ul>
              <ul>
                <table>
                    <tr><td align="right" valign="top"><code>C04.WarmwasserPumpe</code> : </td><td align="left" valign="top">hot water pump (on / off / auto)</td></tr>
                    <tr><td align="right" valign="top"><code>C05.WassertemperaturSoll</code> : </td><td align="left" valign="top">target temperature hot water (10 .. 65&deg;C)</td></tr>
                    <tr><td align="right" valign="top"><code>C06.Anlagenmodus_HK1</code> : </td><td align="left" valign="top">Mode of heating circuit 1 (Tag/Nacht/Standby/Timer)</td></tr>
                    <tr><td align="right" valign="top"><code>C07.Tagestemperatur_HK1</code> : </td><td align="left" valign="top">Set temperature day heating circuit 1</td></tr>
                    <tr><td align="right" valign="top"><code>C08.Nachttemperatur_HK1</code> : </td><td align="left" valign="top">Set temperature night heating circuit 1</td></tr>
                    <tr><td align="right" valign="top"><code>C09.TemperaturFeineinstellung_HK1</code> : </td><td align="left" valign="top">Fine temperature adjustment heating circuit 1 (-5 ... 5)</td></tr>
                    <tr><td align="right" valign="top"><code>C10.Raumeinfluss_HK1</code> : </td><td align="left" valign="top">Room influence of heating circuit 1 (0 ... 90%)</td></tr>
                    <tr><td align="right" valign="top"><code>C12.Anlagenmodus_HK2</code> : </td><td align="left" valign="top">Mode of heating circuit 2 (Tag/Nacht/Standby/Timer)</td></tr>
                    <tr><td align="right" valign="top"><code>C13.Tagestemperatur_HK2</code> : </td><td align="left" valign="top">target temperature day heating circuit 2</td></tr>
                    <tr><td align="right" valign="top"><code>C14.Nachttemperatur_HK2</code> : </td><td align="left" valign="top">Set temperature night heating circuit 2</td></tr>
                    <tr><td align="right" valign="top"><code>C15.TemperaturFeineinstellung_HK2</code> : </td><td align="left" valign="top">Fine temperature adjustment heating circuit 2 (-5 ... 5)</td></tr>
                    <tr><td align="right" valign="top"><code>C16.Raumeinfluss_HK2</code> : </td><td align="left" valign="top"> room influence of heating circuit 2 (0 ... 90%)</td></tr>
                    <tr><td align="right" valign="top"><code>C26.Warmwasserzirkulation_Puls</code> : </td><td align="left" valign="top">DHW circulation mode pulse </td></tr>
                    <tr><td align="right" valign="top"><code>C27.Warmwasserzirkulation_Zeit</code> : </td><td align="left" valign="top">DHW circulation mode time </td></tr>
                    <tr><td align="right" valign="top"><code>C28.WW_Pumpe_Min_Laufzeit</code> : </td><td align="left" valign="top">DHW circulation minimum pulse time </td></tr>
                    <tr><td align="right" valign="top"><code>C29.BetriebsartVT_HK1</code> : </td><td align="left" valign="top">Algorithm flow temperature heating circuit 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C30.Steilheit_HK1</code> : </td><td align="left" valign="top">Flow temperature slope, heating circuit 1</td></tr>
                    <tr><td align="right" valign="top"><code>C31.Fix_Vorlauf_Tag_HK1</code> : </td><td align="left" valign="top">Fixed flow temperature day heating circuit 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C32.Fix_Vorlauf_Absenk_HK1</code> : </td><td align="left" valign="top">Fixed flow temperature night heating circuit 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C33.Min_Vorlauf_Temp_HK1</code> : </td><td align="left" valign="top">Minimum flow temperature heating circuit 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C34.BetriebsartVT_HK2</code> : </td><td align="left" valign="top">Algorithm flow temperature heating circuit 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C35.Steilheit_HK2</code> : </td><td align="left" valign="top">Flow temperature slope, heating circuit 2</td></tr>
                    <tr><td align="right" valign="top"><code>C36.Fix_Vorlauf_Tag_HK2</code> : </td><td align="left" valign="top">Fixed flow temperature day heating circuit 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C37.Fix_Vorlauf_Absenk_HK2</code> : </td><td align="left" valign="top">Fixed flow temperature night heating circuit 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C38.Min_Vorlauf_Temp_HK2</code> : </td><td align="left" valign="top">Minimum flow temperature heating circuit 2 </td></tr>
                </table>
              </ul>
            </ul><BR>
          </ul>
        </td></tr>
      </table>

      <table>
        <tr><td><ul><code>set &lt;device&gt; &lt;name&gt; &lt;value&gt;</code></ul></td></tr>
      </table>

      <ul>
        <ul>
          <table>
            <tr><td align="right" valign="top"><code>&lt;device&gt;</code> : </td><td align="left" valign="top">
                    The name of the device. Recommendation: "mySolvisMax".</td></tr>
            <tr><td align="right" valign="top"><code>&lt;name&gt;</code> : </td><td align="left" valign="top">
                    The name of the value to be set. E.g.: <code>C08.Nachttemperatur_HK1</code>"<BR></td></tr>
            <tr><td align="right" valign="top"><code>&lt;value&gt;</code> : </td><td align="left" valign="top">
                    A valid value.<BR></td></tr>
          </table><BR>
        </ul>
            In addition to the SET commands for changing the above system values, there are also the following server commands (&lt;name&gt;: "ServerCommand", &lt;value&gt; , according to the following table):
        <ul>
          <table>
            <tr><td align="right" valign="top"><code>BACKUP</code>: </td><td align="left" valign="top">
              Saves the calculated measured values ​​(X1 .. X8) in a backup file.<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>SCREEN_RESTORE_INHIBIT</code>: </td><td align="left" valign="top">
              Normally, after a parameter query in the GUI, the SolvisControl returns to the previous screen. This behavior is prevented by this command.<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>SCREEN_RESTORE_ENABLE</code>: </td><td align="left" valign="top">
              Counterpart to the server command SCREEN_RESTORE_INHIBIT<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>COMMAND_OPTIMIZATION_ENABLE</code>: </td><td align="left" valign="top">
              Normally the control commands are executed in an optimized order. Commands that trigger actions on the same screen are carried out together. If strict compliance with the command sequence is required, this can be prevented by this server command.<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>COMMAND_OPTIMIZATION_INHIBIT</code>: </td><td align="left" valign="top">
              counterpart to the ServerCommand COMMAND_OPTIMIZATION_ENABLE<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>GUI_COMMANDS_ENABLE</code>: </td><td align="left" valign="top">
              Using this command, the GUI control can be temporarily deactivated, e.g. when the system is serviced. If you control this via the FHEM interface, a SET command is recommended. If you activate / deactivate them manually, you should set the corresponding attribute.<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>GUI_COMMANDS_DISABLE</code>: </td><td align="left" valign="top">
              Counterpart to the ServerCommand GUI_COMMANDS_ENABLE<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>SERVICE_RESET</code>: </td><td align="left" valign="top">
              Resets a detected service access<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>UPDATE_CHANNELS</code>: </td><td align="left" valign="top">
              Update all channels that are only accessible via the GUI.<BR>
            </td></tr>
            <tr><td align="right" valign="top"><code>RESTART</code>: </td><td align="left" valign="top">
              Restarts the server<BR>
            </td></tr>
          </table>
        </ul><BR>
            This table does not necessarily reflect the current status. More or fewer server commands can be defined, since the server commands are handed over to the client by the server itself (with each new connection). The server itself determines what the client offers. The decisive factor is therefore always the output in the web interface.
            <BR><BR>
            In addition, with the SET command "ScreenSelect", a specific Solvis screen can be selected, which is accessed by default. The possible screen names can be found on the web interface.
      </ul><BR><BR><BR>
      <table>
        <tr><td><a name="SolvisClientGet"></a><b>Get</b></td></tr>
        <tr><td>
          <ul>
              The Get function triggers the reading of system values ​​from the GUI of the SolvisControl. The following values ​​can be read out:
            <ul>
              <ul>
                <table>
                    <tr><td align="right" valign="top"><code>C01.StartsBrenner</code> : </td><td align="left" valign="top">Number of burner starts of level 1</td></tr>
                    <tr><td align="right" valign="top"><code>C02.LaufzeitBrenner</code> : </td><td align="left" valign="top">Burner runtime in level 1 (in h)</td></tr>
                    <tr><td align="right" valign="top"><code>C03.LaufzeitAnforderung2</code> : </td><td align="left" valign="top">Burner runtime in level 2 (in h)</td></tr>
                    <tr><td align="right" valign="top"><code>C04.WarmwasserPumpe</code> : </td><td align="left" valign="top">hot water pump (on / off / auto)</td></tr>
                    <tr><td align="right" valign="top"><code>C05.WassertemperaturSoll</code> : </td><td align="left" valign="top">target temperature hot water (10 .. 65&deg;C)</td></tr>
                    <tr><td align="right" valign="top"><code>C06.Anlagenmodus_HK1</code> : </td><td align="left" valign="top">Mode of heating circuit 1 (Tag/Nacht/Standby/Timer)</td></tr>
                    <tr><td align="right" valign="top"><code>C07.Tagestemperatur_HK1</code> : </td><td align="left" valign="top">Set temperature day heating circuit 1</td></tr>
                    <tr><td align="right" valign="top"><code>C08.Nachttemperatur_HK1</code> : </td><td align="left" valign="top">Set temperature night heating circuit 1</td></tr>
                    <tr><td align="right" valign="top"><code>C09.TemperaturFeineinstellung_HK1</code> : </td><td align="left" valign="top">Fine temperature adjustment heating circuit 1 (-5 ... 5)</td></tr>
                    <tr><td align="right" valign="top"><code>C10.Raumeinfluss_HK1</code> : </td><td align="left" valign="top">Room influence of heating circuit 1 (0 ... 90%)</td></tr>
                    <tr><td align="right" valign="top"><code>C11.Vorlauf_Soll_HK1</code> : </td><td align="left" valign="top">Setpoint of the flow temperature heating circuit 1</td></tr>
                    <tr><td align="right" valign="top"><code>C12.Anlagenmodus_HK2</code> : </td><td align="left" valign="top">Mode of heating circuit 2 (Tag/Nacht/Standby/Timer)</td></tr>
                    <tr><td align="right" valign="top"><code>C13.Tagestemperatur_HK2</code> : </td><td align="left" valign="top">target temperature day heating circuit 2</td></tr>
                    <tr><td align="right" valign="top"><code>C14.Nachttemperatur_HK2</code> : </td><td align="left" valign="top">Set temperature night heating circuit 2</td></tr>
                    <tr><td align="right" valign="top"><code>C15.TemperaturFeineinstellung_HK2</code> : </td><td align="left" valign="top">Fine temperature adjustment heating circuit 2 (-5 ... 5)</td></tr>
                    <tr><td align="right" valign="top"><code>C16.Raumeinfluss_HK2</code> : </td><td align="left" valign="top">room influence of heating circuit 2 (0 ... 90%)</td></tr>
                    <tr><td align="right" valign="top"><code>C17.Vorlauf_Soll_HK2</code> : </td><td align="left" valign="top">Setpoint of the flow temperature heating circuit 2</td></tr>
                    <tr><td align="right" valign="top"><code>C24.LaufzeitSolarpumpe</code> : </td><td align="left" valign="top">Running time of the solar pump (A01)</td></tr>
                    <tr><td align="right" valign="top"><code>C25.LaufzeitSolarpumpe2</code> : </td><td align="left" valign="top">Running time of the solar pump 2 (A02)</td></tr>
                    <tr><td align="right" valign="top"><code>C26.Warmwasserzirkulation_Puls</code> : </td><td align="left" valign="top">DHW circulation mode pulse </td></tr>
                    <tr><td align="right" valign="top"><code>C27.Warmwasserzirkulation_Zeit</code> : </td><td align="left" valign="top">DHW circulation mode time </td></tr>
                    <tr><td align="right" valign="top"><code>C28.WW_Pumpe_Min_Laufzeit</code> : </td><td align="left" valign="top">DHW circulation minimum pulse time </td></tr>
                    <tr><td align="right" valign="top"><code>C29.BetriebsartVT_HK1</code> : </td><td align="left" valign="top">Algorithm flow temperature heating circuit 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C30.Steilheit_HK1</code> : </td><td align="left" valign="top">Flow temperature slope, heating circuit 1</td></tr>
                    <tr><td align="right" valign="top"><code>C31.Fix_Vorlauf_Tag_HK1</code> : </td><td align="left" valign="top">Fixed flow temperature day heating circuit 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C32.Fix_Vorlauf_Absenk_HK1</code> : </td><td align="left" valign="top">Fixed flow temperature night heating circuit 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C33.Min_Vorlauf_Temp_HK1</code> : </td><td align="left" valign="top">Minimum flow temperature heating circuit 1 </td></tr>
                    <tr><td align="right" valign="top"><code>C34.BetriebsartVT_HK2</code> : </td><td align="left" valign="top">Algorithm flow temperature heating circuit 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C35.Steilheit_HK2</code> : </td><td align="left" valign="top">Flow temperature slope, heating circuit 2</td></tr>
                    <tr><td align="right" valign="top"><code>C36.Fix_Vorlauf_Tag_HK2</code> : </td><td align="left" valign="top">Fixed flow temperature day heating circuit 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C37.Fix_Vorlauf_Absenk_HK2</code> : </td><td align="left" valign="top">Fixed flow temperature night heating circuit 2 </td></tr>
                    <tr><td align="right" valign="top"><code>C38.Min_Vorlauf_Temp_HK2</code> : </td><td align="left" valign="top">Minimum flow temperature heating circuit 2 </td></tr>
                </table>
              </ul>
            </ul><BR>
          </ul>
        </td></tr>
      </table>
      <table>
        <tr><td><ul><code>get &lt;device&gt; &lt;name&gt;</code></ul></td></tr>
      </table>
      <ul>
        <ul>
          <table>
            <tr><td align="right" valign="top"><code>&lt;device&gt;</code> : </td><td align="left" valign="top">
                The name of the device. Recommendation: "mySolvisMax".</td></tr>
            <tr><td align="right" valign="top"><code>&lt;name&gt;</code> : </td><td align="left" valign="top">
                 The name of the value to be read out. E.g .: "<code>C08.Nachttemperatur_HK1</code>"<BR></td></tr>
          </table>
        </ul>
      </ul><BR><BR>
      <table>
        <tr><td><a name="SolvisClientAttr"></a><b>Attribute</b></td></tr>
        <tr><td>
          <ul>
              The following module-specific attributes can be set in addition to the known global attributes such as: <a href="#room">room</a>.<BR>
            <ul>
              <ul>
                <table>
                    <tr><td align="right" valign="top"><code>SolvisName</code> : </td><td align="left" valign="top">Name of the id of the server unit (defined in base.xml). If the attribute is not defined, the FHEM device name must be identical to the ID of the unit of the base.xml of the server.</td></tr>
                    <tr><td align="right" valign="top"><code>GuiCommandsEnabled</code> : </td><td align="left" valign="top">TRUE / FALSE: Gui commands are enabled / disabled. In the case of service, it is recommended to set this attribute to FALSE so that the service is not irritated by the SolvisSmartHomeServer. Default: TRUE</td></tr>
                </table>
              </ul>
            </ul><BR>
          </ul>
        </td></tr>
      </table>

      <table>
        <tr><td><ul><code>attr &lt;devive&gt; &lt;name&gt; &lt;value&gt;</code></ul></td></tr>
      </table>
      <ul>
        <ul>
          <table>
              <tr><td align="right" valign="top"><code>&lt;device&gt;</code> : </td><td align="left" valign="top">
                  The name of the device. Recommendation: "mySolvisMax".</td></tr>
              <tr><td align="right" valign="top"><code>&lt;name&gt;</code> : </td><td align="left" valign="top">
                  The name of the value to be set.<BR></td></tr>
              <tr><td align="right" valign="top"><code>&lt;value&gt;</code> : </td><td align="left" valign="top">
                  A valid value.<BR></td></tr>
          </table><BR><BR>
        </ul>
      </ul>

      <table>
        <tr><td><a name="SolvisClientState"></a><b>state</b></td></tr>
        <tr><td>
          <ul>
              The special reading "state" can have the following values:<BR>
            <ul>
              <ul>
                <table>
                    <tr><td align="right" valign="top"><code>disconnected</code> : </td><td align="left" valign="top">Server is not connected to the client.</td></tr>
                    <tr><td align="right" valign="top"><code>opened</code> : </td><td align="left" valign="top">The connection to the server is established.</td></tr>
                    <tr><td align="right" valign="top"><code>SOLVIS_DISCONNECTED</code> : </td><td align="left" valign="top">DThe connection to the server is established, but none to the SolvisRemote</td></tr>
                    <tr><td align="right" valign="top"><code>REMOTE_CONNECTED</code> : </td><td align="left" valign="top">The connection to the server and SolvisRemote is established. No connection to the heating system.</td></tr>
                    <tr><td align="right" valign="top"><code>SOLVIS_CONNECTED</code> : </td><td align="left" valign="top">All connections established.</td></tr>
                    <tr><td align="right" valign="top"><code>POWER_OFF</code> : </td><td align="left" valign="top">Solvis system is apparently switched off.</td></tr>
                    <tr><td align="right" valign="top"><code>ERROR</code> : </td><td align="left" valign="top">SolvisControl displays an error message.</td></tr>
                </table>
              </ul>
            </ul><BR>
          </ul>
        </td></tr>
      </table>
      <table>
        <tr><td><a name="SolvisClientHumanAccess"></a><b>HumanAccess</b></td></tr>
        <tr><td>
          <ul>
              The special reading "HumanAccess" can have the following values:<BR>
            <ul>
              <ul>
                <table>
                    <tr><td align="right" valign="top"><code>none</code> : </td><td align="left" valign="top">No external access detected</td></tr>
                    <tr><td align="right" valign="top"><code>service</code> : </td><td align="left" valign="top">Access from the service</td></tr>
                    <tr><td align="right" valign="top"><code>user</code> : </td><td align="left" valign="top">Access from the user</td></tr>
                </table>
              </ul>
            </ul><BR>
          </ul>
        </td></tr>
      </table>
    </td></tr>
  </table>
</ul>
=end html

=for :application/json;q=META.json 73_SolvisClient.pm
{
  "abstract": "Module for controlling solvis heating system",
  "x_lang": {
    "de": {
      "abstract": "Modul zur Steuerung einer Solvis Heizungsanlage"
    }
  },
  "keywords": [
    "fhem-mod-device",
    "fhem-core",
    "Solvis",
    "SolvisBen",
    "SolvisMax",
    "SolvisRemote"
  ],
  "release_status": "stable",
  "license": "GPL_2",
  "version": "v00.02.22",
  "author": [
    "Stefan Gollmer <Stefan.Gollmer@gmail.com>"
  ],
  "x_fhem_maintainer": [
    "SCMP77"
  ],
  "prereqs": {
    "runtime": {
      "requires": {
        "FHEM": 5.00918799,
        "perl": 5.016,
        "Meta": 0,
        "JSON": 0
      },
      "recommends": {
      },
      "suggests": {
      }
    }
  }
}
=end :application/json;q=META.json

=cut
