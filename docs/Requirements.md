# Server

- Il server deve essere multithreaded realizzato usando JAVA thread pooling
- Il client interagisce con il server, secondo il modello client-server (richieste/risposte),
sulla connessione TCP persistente creata, inviando uno dei comandi elencati in
sezione 2.1 secondo il formato dei messaggi JSON indicato in sezione 5. Tutte le
operazioni devono avvenire su questa connessione TCP. E’ richiesto che Il client
gestisca questa connessione utilizzando NIO
- Il server definisce strutture dati opportunamente sincronizzate per memorizzare le
informazioni relative agli utenti e allo stato del gioco corrente o storico di giochi
passati
- Quando il server deve mandare comunicazioni asincrone, ad esempio la notifica di
fine partita allo scadere del tempo, le notifiche vengono inviate utilizzando il
protocollo UDP. Il client deve quindi essere progettato in modo che sia in grado di
ricevere dal server notifiche asincrone

# Client

## Operations

Always:
- **Sign up** (un utente può registrarsi indicando un nome utente e password sse il nome utente non è già registrato)
- **Update credentials** (un utente può aggiornare il proprio nome utente o la password collegata ad un nome utente se è in grado di dimostrare di conoscere la password attualmente associata a quel nome utente)
- **Login** (un utente può effettuare il login se conosce la password corretta relativa ad un certo nome utente. Effettuare il login automaticamente fa accedere alla partita attualmente in corso ricevendone le informazioni necessarie (set di parole, proposte corrette eventualmente già inviate da quell’utente, numero di errori, tempo rimanente, punteggio corrente))
- **Logout** (effettuare il logout informa interrompe la possibilità di ricevere notifiche dal server ed inviare proposte sulla partita corrente al server)

When logged in:
- **Send answer** (invia una nuova proposta di 4 parole tra quelle non ancora
correttamente inserite in un gruppo da quell’utente. All’invio segue risposta
del server per notificare la correttezza o meno della proposta)
- **Request game state** (l’utente specifica l’id univoco della partita a cui
è interessato oppure che è interessato alla partita in corso. Se la partita è in
corso serve a ricevere il tempo rimanente per quella partita, le proposte
corrette, le parole rimaste ancora da raggruppare correttamente, il numero
di errori ed il punteggio corrente. Se la partita è conclusa (per vittoria,
sconfitta, o tempo scaduto) serve a ricevere la corretta assegnazione delle 16
parole ai 4 gruppi, il numero di proposte corrette, il numero di errori fatti, ed
il punteggio ottenuto)
- **Request game statistics** (l’utente specifica l’id univoco della partita a cui
è interessato oppure che è interessato alla partita in corso. Se la partita è in
corso serve a ricevere il tempo rimanente per quella partita, il numero di
giocatori con quella partita ancora in corso, il numero di giocatori che hanno
concluso la partita ed il numero di giocatori che hanno concluso la partita
con una vittoria. Se la partita è conclusa serve a ricevere il numero di
giocatori che hanno partecipato alla partita, il numero di giocatori che hanno
concluso la partita, il numero di giocatori che hanno concluso la partita con
una vittoria ed il punteggio medio dei giocatori che hanno partecipato alla
partita)
- **Request leaderboard info** (in base ai parametri della richiesta può richiedere
l’intera classifica di tutti gli utenti, la classifica dei top K utenti o la posizione
nella classifica di un certo utente (sia esso il richiedente o meno))
- **Request personal stats** (per richiedere statistiche sull’utente corrente nella
seguente forma ispirata alle statistiche offerte dal New York Times: Puzzles Completed, Win Rate %, Loss Rate %, Current Streak, Max Streak, Perfect Puzzles, Mistake Histogram)


# Informazioni generali
- Il codice deve essere commentato
- Le classi con un metodo main devono avere Main nel nome
- E' necessario anche consegnare un file .jar eseguibile per ogni applicazione (client e server)
- I parametri di input (numeri di porta, inidirizzi, valori di timeout...) devono essere letti automaticamente da file di configurazione da consegnare insieme al resto del codice
- Bisogna fare una relazione sintetica contentente:
    - la definizione delle scelte effettuate nei punti del progetto lasciati alla
personale interpretazione;
    - uno schema generale dei thread attivati sia lato server che lato client;
    - una definizione delle strutture dati utilizzate sia lato server che lato client;
    - una descrizione delle eventuali primitive di sincronizzazione utilizzate dai
thread per accedere a strutture dati condivise;
    - una sezione di istruzioni su come compilare ed eseguire il progetto (librerie
esterne usate, argomenti da passare al codice, sintassi dei comandi per
eseguire le varie operazioni, ecc.). Questa sezione deve essere un manuale di
istruzioni chiaro per gli utilizzatori del sistema.
- Relazione e codice sorgente devono essere consegnati su Moodle in un unico archivio
compresso in formato zip (non rar, non gz).