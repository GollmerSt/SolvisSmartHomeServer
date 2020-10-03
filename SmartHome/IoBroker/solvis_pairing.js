var solvisPath = 'mqtt-client.0.SolvisSmartHomeServer.mySolvis.';
var solvis = 'mySolvis';
var online = 'mqtt-client.0.SolvisSmartHomeServer.IoBroker.online';

var channels = [
	'C04:WarmwasserPumpe',
	'C05:WassertemperaturSoll',
	'C06:Anlagenmodus_HK1',
	'C07:Tagestemperatur_HK1',
	'C08:Nachttemperatur_HK1',
	'C09:TemperaturFeineinstellung_HK1',
	'C10:Raumeinfluss_HK1',
	'C12:Anlagenmodus_HK2',
	'C13:Tagestemperatur_HK2',
	'C14:Nachttemperatur_HK2',
	'C15:TemperaturFeineinstellung_HK2',
	'C16:Raumeinfluss_HK2',
	'C18:Anlagenmodus_HK3',
	'C19:Tagestemperatur_HK3',
	'C20:Nachttemperatur_HK3',
	'C21:TemperaturFeineinstellung_HK3',
	'C22:Raumeinfluss_HK3',
	'C26:Warmwasserzirkulation_Puls',
	'C27:Warmwasserzirkulation_Zeit',
	'C28:WW_Pumpe_Min_Laufzeit',
	'C29:BetriebsartVT_HK1',
	'C30:Steilheit_HK1',
	'C31:Fix_Vorlauf_Tag_HK1',
	'C32:Fix_Vorlauf_Absenk_HK1',
	'C33:Min_Vorlauf_Temp_HK1',
	'C34:BetriebsartVT_HK2',
	'C35:Steilheit_HK2',
	'C36:Fix_Vorlauf_Tag_HK2',
	'C37:Fix_Vorlauf_Absenk_HK2',
	'C38:Min_Vorlauf_Temp_HK2'
]

if ( getState('mqtt-client.0.info.connection') ) {
    init();
} else {
    on ({id: 'mqtt-client.0.info.connection', change: 'ne'}, function(obj){
            var val = obj.state ? obj.state.val : false ;
            if ( val ) {
                init();
            }
        });
}

function init() {

    if ( !getState('mqtt-client.0.info.connection') ) {
        return;
    }

    // Beim Starten dem SolvisSmartHomeServer mitteilen, dass ioBroker online ist
    setState(online, 'true'); 

    channels.forEach(function(channel) {

        var solvisread = solvisPath + channel + '.data';

        var solviswrite = solvisPath + channel + '.cmnd';

        var combined = 'javascript.0.' + solvis + '.' + channel + '.rw';
        

        //kombiniertes Objekt erzeugen

        createState(combined);

    

        // Initialen Wert aus Solvis in die kombinierte Objekt schreiben:

        setState(combined, getState(solvisread).val, true);

    

        // Wert wird durch Solvis geändert:

        on({id: solvisread}, function(obj) {

            var val = obj.state ? obj.state.val : '';

            if (val != getState(combined).val) {
                setState(combined, (obj.state ? obj.state.val : ''), true);
            }


        });

    

        // Wert wird durch ioBroker web.0 geändert:

        on({id: combined, change: 'ne'}, function(obj) {

            //if ((obj.state ? obj.state.from : '') == 'system.adapter.web.0') {

                setState(solviswrite, (obj.state ? (obj.state.val === true ? 1 : (obj.state.val === false ? 0 : obj.state.val)) : ''), false);

            //}

        });

        // Beim Beenden dem SolvisSmartHomeServer mitteilen, dass ioBroker offline ist
        onStop (function(){
            setState(online, 'false');
        }, 2000);

    });
    


    // Beim Starten dem SolvisSmartHomeServer mitteilen, dass ioBroker online ist
    setState(online, 'true'); 

    channels.forEach(function(channel) {

        var solvisread = solvisPath + channel + '.data';

        var solviswrite = solvisPath + channel + '.cmnd';

        var combined = 'javascript.0.' + solvis + '.' + channel + '.rw';
        

        //kombiniertes Objekt erzeugen

        createState(combined);

    

        // Initialen Wert aus Solvis in die kombinierte Objekt schreiben:

        setState(combined, getState(solvisread).val, true);

    

        // Wert wird durch Solvis geändert:

        on({id: solvisread}, function(obj) {

            var val = obj.state ? obj.state.val : '';

            if (val != getState(combined).val) {
                setState(combined, (obj.state ? obj.state.val : ''), true);
            }


        });

    

        // Wert wird durch ioBroker web.0 geändert:

        on({id: combined, change: 'ne'}, function(obj) {

            //if ((obj.state ? obj.state.from : '') == 'system.adapter.web.0') {

                setState(solviswrite, (obj.state ? (obj.state.val === true ? 1 : (obj.state.val === false ? 0 : obj.state.val)) : ''), false);

            //}

        });

        // Beim Beenden dem SolvisSmartHomeServer mitteilen, dass ioBroker offline ist
        onStop (function(){
            setState(online, 'false');
        }, 2000);

    });
}

