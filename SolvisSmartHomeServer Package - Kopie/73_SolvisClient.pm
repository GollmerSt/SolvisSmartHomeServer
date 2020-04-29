########################################################################################################################
#
# Attention: it's not the FHEM archive, it's a private one
# $Id: 73_SolvisClient.pm 158 2020-04-02 17:55:26Z stefa_000 $
#
#  (c) 2019-2020 Copyright: Stefan Gollmer (Stefan dot Gollmer at gmail dot com)
#  All rights reserved
#
#                                          CHANGELOG
#
#   Version     Date        Programmer          Subroutine                              Description of Change
#   00.01.00    17.12.2019  SCMP77              All                                     Initial Release
#   00.02.00    21.01.2020  SCMP77              SolvisClient_CreateGetSetServerCommands Server commands are determined by the server itself
#   00.02.01    06.02.2020  SCMP77              SolvisClient_InterpreteConnectionState  Updated to current messages
#   00.02.02    13.02.2020  SCMP77              SolvisClient_InterpreteConnectionState  Attribut enable gui commands addes
#   00.02.03    24.03.2020  SCMP77              All                                     Using FHEM::SolvisClient-Package, Supports FHEM:Meta, Allgemeine Perl

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

use constant WATCH_DOG_INTERVAL => 300 ;    #Mindestens alle 5 Minuten muss der Client Daten vom Servere erhalten haben.
                                            # Andernfalls wird die Verbindung neu aufgebaut
use constant RECONNECT_AFTER_DISMISS => 30 ;
use constant MIN_CONNECTION_INTERVAL => 10 ;

use constant _FALSE_ => 0 ;
use constant _TRUE_ => 1;

use constant FORMAT_VERSION_REGEXP => 'm/01\.../' ;

use constant _DISABLE_GUI_ => 0 ;
use constant _ENABLE_GUI_ => 1 ;
use constant _UPDATE_GUI_ => 2 ;



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
                'Cpanel::JSON::XS,JSON::XS,JSON::PP,JSON::backportPP' ;
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



my %ChannelDescriptions ;
my $setParameters ;
my $serverCommands = '';
my @serverCommand_Array = ();



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
    my $modulData = shift ;

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
    my $self = shift ;
    my $def = shift ;
    
    if ( !FHEM::Meta::SetInternals($self)  ) {
        return ($self,$def);
    }

    my @args = split( '[ \t][ \t]*', $def );
    my $url  = $args[2];
    my $name = $self->{NAME};


    $self->{DeviceName}  = $url;    #Für DevIO, Name fest vorgegeben

    if( $init_done ) {
        my $result = Connect( $self, 0 );
    } else {
        $self->{NOTIFYDEV} = 'global';
    }

    $self->{helper}{GuiEnabled} = undef ;

    use version 0.77;
    our $CLIENT_VERSION = FHEM::Meta::Get( $self, 'version' );
    $self->{VERSION_CLIENT} = version->parse($CLIENT_VERSION)->normal ;

    return ;
} # end Define



#####################################
#
#       Attribute
#
sub Attr {
    my $cmd = shift ;
    my $name = shift ;
    my $attrName = shift ;
    my $attrValue   = shift ;

    my $self = $defs{$name} ;

    if ( $attrName eq 'GuiCommandsEnabled' ) {
        if ( defined $attrValue  && $attrValue   ne 'TRUE' && $attrValue   ne 'FALSE' ) {
            return "Unknown value $attrValue for $attrName, choose one of TRUE FALSE";
        }
    }

    return ;
} # end Attr



#####################################
#
#       Try connection if FHEM is initalized or configuration was rereaded
#
sub Notify {
    my $self = shift ;
    my $eventObject = shift ;

    my $ownName = $self->{NAME}; # own name

    if ( IsDisabled($ownName)) {
        return ;    # Return without any further action if the module is disabled
    }

    my $devName = $eventObject->{NAME}; # Device that created the events
    my $events = deviceEvents($eventObject, 1);

    if($devName eq 'global' && grep( { m/^INITIALIZED|REREADCFG$/x } @{$events}))
    {
        $self->{helper}{GuiEnabled} = undef ;
         my $result = Connect( $self, 0 );
        $self->{NOTIFYDEV} = '';
        Log($self, 3, 'New Connection in case of rereadcfg or initialized');
    }
    return ;
} # end Notify



#########################################
#
#       Create Send data
#
sub CreateSendData {
    my $self = shift ;
    my $sendData = shift ;

    my $byteString = encode_json ( $sendData );

    my $length = length $byteString ;
    $byteString = pack('CCCa*', $length>>16&0xff, $length>>8&0xff, $length&0xff, $byteString ) ;

    Log( $self, 5, "ByteString: $byteString");

    return $byteString ;

} # end CreateSendData



#####################################
#
#       Connect to Server
#
sub Connect {
    my $self = shift ;
    my $reopen = shift ;

    my $connectedSub = \&SendConnectionData ;

    if ( $reopen ) {

        $connectedSub = \&SendReconnectionData ;

    }

    my $error = DevIo_OpenDev($self, $reopen, $connectedSub);

    $self->{helper}{BUFFER} = '' ;

    return $error;

} # end Connect



####################################
#
#       Send connection data
#
sub SendConnectionData {
    my $self = shift ;


    $self->{CLIENT_ID} = undef ;

    my %sendConnectionInfo = (
        CONNECT => {
            Id => AttrVal( $self->{NAME}, 'SolvisName', $self->{NAME} )
        }
    ) ;

    DevIo_SimpleWrite($self, CreateSendData($self, \%sendConnectionInfo ), 0);

    return ;

} # end SendConnectionData



################################################
#
#       Send reconnection data
#
sub SendReconnectionData {
    my $self = shift ;

    if ( defined( $self->{CLIENT_ID} ) ) {

        my %sendReconnectionInfo = (
            RECONNECT => {
                Id => $self->{CLIENT_ID}
            }
        ) ;

        DevIo_SimpleWrite($self, CreateSendData($self, \%sendReconnectionInfo ), 0);

    } else {
         SendConnectionData($self) ;
    }

    return ;
} # end SendReconnectionData



################################################
#
#       Ein Reconnect wird versucht
#
sub Ready {
    my $self = shift ;

    $self->{helper}{GuiEnabled} = undef ;

    my $error = undef ;

    if($self->{STATE} eq 'disconnected' && isNewConnectionAllowed($self) == _TRUE_ ) {
        Log( $self, 4, 'Reconnection try');
        $error = Connect($self, 1) ; # reopen
    }

    return !defined($error);

} # end Ready



################################################
#
#       Untersuchen, ob reconnect wieder erlaubt
#
sub isNewConnectionAllowed {
    my $self = shift ;

    my $now = gettimeofday() ;

    my $connect = _FALSE_ ;

    if ( defined( $self->{helper}{nextConnection})) {
        if ( $now > $self->{helper}{nextConnection} ) {
            $connect = _TRUE_ ;
        }
    } else {
        $connect = _TRUE_ ;
        $self->{helper}{connectionInterval} = undef ;
    }
    if ( $connect == _TRUE_ ) {
        my $interval = MIN_CONNECTION_INTERVAL ;
        if ( defined( $self->{helper}{connectionInterval})) {
            $interval = $self->{helper}{connectionInterval} ;
        }
        $self->{helper}{connectionInterval} = $interval * 2 ;
        $self->{helper}{nextConnection} = $now + $interval ;
    }
    return $connect
}


################################################
#
#       Daten vom Server erhalten
#
sub Read {
    my $self = shift ;

    my $name = $self->{NAME};

    RemoveInternalTimer($self, \&Reconnect );
    my $timeStamp = gettimeofday() + WATCH_DOG_INTERVAL ;
    InternalTimer($timeStamp, \&Reconnect, $self );


    Log($self, 5, 'Read entered');

    # einlesen der bereitstehenden Daten
    my $buf = DevIo_SimpleRead($self);

    if ( ! defined( $buf ) ) {
        return ;
    }

    $self->{helper}{BUFFER} .= $buf;

    #Log($self, 3, "Current buffer content: $self->{helper}{BUFFER}");

    while ( length($self->{helper}{BUFFER}) >= 3 ) {

        my $bufferLength = length($self->{helper}{BUFFER}) ;

        my @parts = unpack('CCC', $self->{helper}{BUFFER} ) ;
        my $length = $parts[2] | $parts[1] << 8 | $parts[0] << 16 ;

        Log($self, 5, "Length of package: $length");

        if ( $length > $bufferLength - 3 ) {
            return ;
        }

        @parts = unpack('CCCa'.$length.'a*', $self->{helper}{BUFFER} ) ;
        $self->{helper}{BUFFER} = $parts[4] ;

        Log($self, 5, "Package encoded: $parts[3]");

        my $receivedData = decode_json ($parts[3]);

        executeCommand($self, $receivedData) ;
    }

    return ;
} # end Read



#####################################
#
#       Execute server commands
#
sub executeCommand {
    my $self = shift ;
    my $receivedData = shift ;

    my @key = keys %$receivedData ;

    my $command = $key[0] ;

    Log($self, 4, "Command detected: $command");

    my %switch = (
        'CONNECTED' => sub {
            Connected($self, $receivedData) ;
            EnableGui( $self ) ;
        },
        'MEASUREMENTS' => sub {
            UpdateReadings($self, $receivedData->{MEASUREMENTS}) ;
            EnableGui( $self ) ;
            $self->{helper}{nextConnection} = undef ;
        },
        'DESCRIPTIONS' => sub {
            CreateGetSetServerCommands($self, $receivedData->{DESCRIPTIONS}) ;
            EnableGui( $self ) ;
        },
        'CONNECTION_STATE' => sub {
            InterpreteConnectionState($self, $receivedData->{CONNECTION_STATE}) ;
        },
        'SOLVIS_STATE' => sub {
            InterpreteSolvisState($self, $receivedData->{SOLVIS_STATE}) ;
            EnableGui( $self ) ;
        }
    ) ;
    
    if ( defined($switch{ $command  })) {
        $switch{ $command  }->();
    }

    return ;
} # end executeCommand



##########################################
#
#       Connected with the server
#
sub Connected {
    my $self = shift ;
    my $receivedData = shift ;

    $self->{CLIENT_ID} = $receivedData->{CONNECTED}->{ClientId} ;
    if ( defined ($receivedData->{CONNECTED}->{ServerVersion}) ) {
        $self->{VERSION_SERVER} = $receivedData->{CONNECTED}->{ServerVersion} ;
        my $formatVersion = $receivedData->{CONNECTED}->{FormatVersion} ;
        if ( ! $formatVersion =~ FORMAT_VERSION_REGEXP ) {
            Log($self, 3, "Format version $formatVersion of client is depricated, use a newer client, if available.");
            $self->{INFO} = 'Format version is depricated' ;
        }
        Log($self, 3, "Server version: $self->{VERSION_SERVER}") ;
    }
    return ;
} # end Connected



##########################################
#
#       EnableGui handling
#
sub EnableGui {
    my $self = shift ;

    my $attrVal = AttrVal($self->{NAME}, 'GuiCommandsEnabled', 'TRUE');
    my $enabled = $attrVal eq 'TRUE' ;

    if (!defined($self->{helper}{GuiEnabled}) || $self->{helper}{GuiEnabled} != $enabled ) {
        my $command = $enabled?'GUI_COMMANDS_ENABLE':'GUI_COMMANDS_DISABLE' ;

        SendServerCommand($self, $command) ;

        $self->{helper}{GuiEnabled} = $enabled ;
        Log($self, 3, "Command <$command> is sent to server") ;
    }
    return ;
} # end EnableGui



##########################################
#
#       Interprete connection status
#
sub InterpreteConnectionState {
    my $self = shift ;
    my $state = shift ;

    my @keys = keys(%$state) ;

    my $stateString  ;
    my $message  ;

    foreach my $key( keys(%$state)) {
        if ( $key eq 'State') {
            $stateString = $state->{$key } ;
        } elsif ( $key eq 'Message') {
            $message = $state->{$key } ;
        }
    }


    Log($self, 5, "Connection status: $stateString");

    my %switch = (
        'CLIENT_UNKNOWN' => sub {
            $self->{helper}{GuiEnabled} = undef ;
            $self->{CLIENT_ID} = undef ;
            Log($self, 3, "Client unknown: $message");
            ReconnectAfterDismiss($self);
        },
        'CONNECTION_NOT_POSSIBLE' => sub {
            $self->{helper}{GuiEnabled} = undef ;
            Log($self, 3, "Connection not possible: $message");
            ReconnectAfterDismiss($self);
        },
        'ALIVE' => sub {
            Log($self, 4, 'Alive received');
        },
        'USER_ACCESS_DETECTED' => sub {
            Log($self, 3, 'User access detected');
        },
        'USER_ACCESS_FINISHED' => sub {
            Log($self, 3, 'User access finished');
        }
    ) ;

    if ( defined($switch{ $stateString  })) {
        $switch{ $stateString  }->();
    } else {
        Log($self, 3, "Connection status: $stateString");
    }

    return ;
} # end InterpreteConnectionState



##########################################
#
#       Reconnection nach verbindungsfehler
#
sub ReconnectAfterDismiss {
    my $self = shift ;

    DevIo_CloseDev($self) ;
    $self->{helper}{GuiEnabled} = undef ;

    my $timeStamp = gettimeofday() + RECONNECT_AFTER_DISMISS ;
    InternalTimer($timeStamp, \&Reconnect, $self );

    return ;
} # end ReconnectAfterDismiss



##########################################
#
#       Interprete solvis state
#
sub InterpreteSolvisState {
    my $self = shift ;
    my $state = shift ;

    my @keys = keys(%$state) ;

    my $stateString  ;
    my $message  ;

    foreach my $key( keys(%$state)) {
        if ( $key eq 'SolvisState') {
            $stateString = $state->{$key } ;
        }
    }

    Log($self, 3, "Solvis status: $stateString");

    readingsSingleUpdate($self,'state',$stateString,1);

    #DoTrigger($self->{NAME}, $stateString);

    return ;
} # end InterpreteSolvisState



################################################
#
#       Reconnect
#
sub Reconnect {
    my $self = shift ;


    $self->{helper}{GuiEnabled} = undef ;

    Log($self, 3, 'Retry reconnection');
    DevIo_CloseDev($self) ;
    Connect($self,0);
    return ;
} # end Reconnect



#######################################
#
#       Create data for Set and Get commands
#
sub CreateGetSetServerCommands {
    my $self = shift ;
    my $descriptions = shift ;

    %ChannelDescriptions = ();
    $serverCommands = '' ;
    @serverCommand_Array = ();

    foreach my $description( keys(%{$descriptions})) {
        my %descriptionHash = %$descriptions{$description} ;
        my @keys = keys %descriptionHash ;
        my $name = $keys[0] ;
        Log($self, 5, "Processing of description: $name");
        my %channelHash = %{$descriptionHash{$name}} ;
        if ( $channelHash{Type} eq 'ServerCommand') {
            #if ( $name ne 'GUI_COMMANDS_DISABLE' && $name ne 'GUI_COMMANDS_ENABLE' ) {
                push(@serverCommand_Array, $name);
            #}
        } else {
            $ChannelDescriptions{$name} = {} ;
            $ChannelDescriptions{$name}{SET} = $channelHash{Writeable} ;
            $ChannelDescriptions{$name}{GET} = $channelHash{Type} eq 'CONTROL' ;
            Log($self, 5, "Writeable: $channelHash{Writeable}");
            my %switch = (
                'Accuracy' => sub {
                    $ChannelDescriptions{$name}{Accuracy} = $channelHash{Accuracy} ;
                },
                'Modes' => sub {
                    $ChannelDescriptions{$name}{Modes} = {} ;
                    foreach my $mode (@{$channelHash{Modes}}) {
                        $ChannelDescriptions{$name}{Modes}{$mode} = 1 ;
                    }
                },
                'Upper' => sub {
                    $ChannelDescriptions{$name}{Upper} = $channelHash{Upper} ; ;
                },
                'Lower' => sub {
                    $ChannelDescriptions{$name}{Lower} = $channelHash{Lower} ; ;
                },
                'Step' => sub {
                    $ChannelDescriptions{$name}{Step} = $channelHash{Step} ; ;
                },
                'IsBoolean' => sub {
                    $ChannelDescriptions{$name}{IsBoolean} = $channelHash{IsBoolean} ;
                },
                'Unit' => sub {
                    $ChannelDescriptions{$name}{Unit} = $channelHash{Unit} ;
                }
            ) ;
            foreach my $keyName ( keys %channelHash ) {
                if ( defined($switch{ $keyName  })) {
                    $switch{ $keyName  }->();
                }
            }
        }
    }

    $serverCommands = join(',',sort(@serverCommand_Array));
    
    CreateSetParams() ;

    return ;

} # end CreateGetSetServerCommands



#######################################
#
#       Create data for Set Parameters
#
sub CreateSetParams {
    $setParameters = '' ;

    my @channels = keys(%ChannelDescriptions) ;
    my $firstO = _TRUE_ ;
    foreach my $channel (@channels) {
        if ( $ChannelDescriptions{$channel}{SET} == _FALSE_ ) {
            next ;
        }
        if ( ! $firstO ) {
            $setParameters .= ' ' ;
        } else {
            $firstO = _FALSE_ ;
        }
        $setParameters .= $channel ;
        my $firstI = _TRUE_ ;
        if ( defined ($ChannelDescriptions{$channel}{Modes}) ) {
            foreach my $mode (keys(%{$ChannelDescriptions{$channel}{Modes}})) {
                if($firstI) {
                    $setParameters .= ':' ;
                    $firstI = _FALSE_ ;
                } else {
                    $setParameters .= ','
                }
                $setParameters .=$mode ;
            }
        } elsif ( defined ($ChannelDescriptions{$channel}{Upper}) ) {
            for ( my $count = $ChannelDescriptions{$channel}{Lower} ; $count <= $ChannelDescriptions{$channel}{Upper} ; $count += $ChannelDescriptions{$channel}{Step}) {
                if($firstI) {
                    $setParameters .= ':' ;
                    $firstI = _FALSE_ ;
                } else {
                    $setParameters .= ','
                }
                $setParameters .=$count ;
            }
        }
    }
    return ;
}



######################################
#
#       Update Readings
#
sub UpdateReadings {
    my $self = shift ;
    my $readings = shift ;

    readingsBeginUpdate($self);

    foreach my $readingName( keys(%$readings)) {
        if ( defined( $readings->{$readingName} )) {
            my $value = $readings->{$readingName} ;
            if ( $ChannelDescriptions{$readingName}{IsBoolean} != _FALSE_ ) {
                $value = $value?'on':'off';
            }

            readingsBulkUpdate($self,$readingName,$value);
        }
    }
    readingsEndUpdate($self, 1);

    return ;
} # end UpdateReadings



######################################
#
#       Undefine
#
sub Undef {
    my $self = shift ;
    my $args = shift ;

    DevIo_CloseDev($self) ;

    return ;
} # end Undef



#####################################
#
#       Delete module
#
sub Delete {
    my $self = shift ;
    my $args = shift ;

    return ;
}# end Delete



#####################################
#
#       send json package
#
sub SendData {
    my $self = shift ;
    my $sendPackage = shift ;

    my $byteString = encode_json ( $sendPackage );

    my $length = length $byteString ;
    $byteString = pack('CCCa*', $length&0xff, $length>>8&0xff, $length>>16&0xff, $byteString ) ;

    Log($self, 5, "ByteString: $byteString");

    DevIo_SimpleWrite($self, $byteString, 0);

    return ;
}# end SendData



#####################################
#
#       send set data
#
sub SendSetData {
    my $self = shift ;
    my $channel = shift ;
    my $data = shift ;

    my %SetPackage = (
        SET => {
            $channel => $data
        }
    ) ;

    SendData($self, \%SetPackage);

    return ;
}# end SendSetData



#####################################
#
#       send get data
#
sub SendGetData {
    my $self = shift ;
    my $channel = shift ;

    my %GetPackage = (
        GET => {
            $channel => undef
        }
    ) ;

    SendData($self, \%GetPackage);

    return ;
}# end SendGetData



#####################################
#
#       Set command
#
sub Set {
    my ( $self, $name, @aa ) = @_;
    my ( $cmd, @args ) = @aa;

    if ( $cmd eq '?' ) {
        return "unknown argument $cmd choose one of $setParameters ServerCommand:$serverCommands";
    }

    ### If not enough arguments have been provided
    if ( @args < 1 ) {
        return "\"set $self->{NAME}\" needs at least two arguments";
    }

    if ( $cmd eq 'ServerCommand') {

        my $serverCommand = $args[0];

        SendServerCommand($self, $serverCommand) ;

    } else {


        my $channel = $cmd ;
        my $value = $args[0];

        Log($self, 4, "Set entered, device := $name, Cannel := $channel, Value := $value");

        if ( defined($ChannelDescriptions{$channel}) ) {
            if ( defined ($ChannelDescriptions{$channel}{Modes}) ) {
                if ( ! defined ($ChannelDescriptions{$channel}{Modes}{$value})) {
                    my @modes = keys(%{$ChannelDescriptions{$channel}{Modes}}) ;
            Log($self, 5, 'Mode 1: '.join(' ', $modes[0]));
                    return "unknown value $value choose one of " . join(' ', @modes);
                }
            } else {
                $value = int($value) ;
            }
            SendSetData($self, $channel, $value) ;
        } else {
            my @channels = keys(%ChannelDescriptions) ;
            Log($self, 5, 'Channels: '.join(' ', @channels));
            return "unknown argument $channel choose one of " . join(' ', @channels);
        }
    }

    return ;
} # end Set



#####################################
#
#       Send server command
#
sub SendServerCommand {
    my $self = shift ;
    my $command = shift ;

    my %ServerCommandPackage = (
        SERVER_COMMAND => {
            'Command' => $command
        }
    ) ;

    SendData($self, \%ServerCommandPackage);

    return ;
} # end SendServerCommand



#####################################
#
#      Get
#
sub Get {

    my $self = shift ;
    my $name = shift ;
    my $opt = shift ;
    #my @args = shift ;

    ### If not enough arguments have been provided
    if ( !defined($opt) ) {
        return '\"get Solvis\" needs at least one argument';
    }

    my $channel = $opt;

    if ( $channel eq '?' || ! defined($ChannelDescriptions{$channel} ) ) {
        my @channels = keys(%ChannelDescriptions) ;
        my $params = '' ;
        my $firstO = _TRUE_ ;
        foreach my $channel (@channels) {
            if ( $ChannelDescriptions{$channel}{GET} == _FALSE_ ) {
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
        return "unknown argument $channel choose one of $params";
    } else {

        Log($self, 4, "Get entered, device := $name, Cannel := $channel");
        SendGetData($self, $channel) ;

        return 'The reading process was started.\nThe value will be output only in the readings.' ;
    }

    return ;

} # end Get
#



#####################################
#
#      DbLog event interpretation
#
sub DbLog_splitFn {
    my $event = shift ;

    my ($reading, $value, $unit) ;

    my @splited = split(/ /,$event);

    $reading = $splited[0];;
    $reading =~ tr/://d;

    $unit = '' ;

    if ( defined( $ChannelDescriptions{$reading}{Unit} ) ) {
        $unit = $ChannelDescriptions{$reading}{Unit} ;
        $value = $splited[1];
    }
    return ($reading, $value, $unit) ;
} # end DbLog_splitFn


1;

###START###### Description for fhem commandref ################################################################START####
=pod

=item device
=item summary       Module for controlling solvis heating system
=item summary_DE    Modul zur Steuerung einer Solvis Heizungsanlage

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
            <tr><td align="right" valign="top"><code>RESTART</code>: </td><td align="left" valign="top">
              Startet den Server neu<BR>
            </td></tr>
          </table>
        </ul><BR>
            Diese Tabelle gibt nicht unbedingt den aktuellen Stand wieder. Es k&ouml;nnen mehr oder weniger Server-Befehle definiert sein, da die Server-Befehle vom Server selber dem Client &uuml;bergeben werden (bei jeder neuen Verbindung). Der Server selber bestimmt daher, was
            der Client anbietet. Maßgebend ist daher immer die Ausgabe in der Web-Oberfl&auml;che.
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
  "release_status": "testing",
  "license": "GPL_2",
  "version": "v00.02.03",
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
