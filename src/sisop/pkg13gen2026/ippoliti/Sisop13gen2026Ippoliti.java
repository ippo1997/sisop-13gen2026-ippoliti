/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package sisop.pkg13gen2026.ippoliti;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author Gabriele
 * - repo https://github.com/ippo1997/sisop-13gen2026-ippoliti
 */
public class Sisop13gen2026Ippoliti {

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        //implementato sistema di input e aggiunte richieste in output
         Scanner input = new Scanner(System.in);
         System.out.print("Inserire il numero di GeneratorThread = ");
         int N = input.nextInt();
         System.out.print("Inserire il tempo di generazione dei valori = ");
         int X = input.nextInt();
         System.out.print("Inserire dimensione della coda = ");
         int L = input.nextInt();
         System.out.print("Inserire numero di WorkerThread = ");
         int M = input.nextInt();
         System.out.print("Inserire il tempo minimo di elaborazione del risultato = ");
         int T = input.nextInt();
         System.out.print("Inserire intervallo massimo rispetto al tempo minimo = ");
         int TT = input.nextInt();
         input.close();
         int count = 0;
         
         //creazione oggetti
         Queue q = new Queue(L, M);
         Uscita u = new Uscita();
         GeneratorThread[] gt = new GeneratorThread[N];
         WorkerThread[] wt = new WorkerThread[M];
         PrintThread p1 = new PrintThread(u, 1);
         PrintThread p2 = new PrintThread(u, 2);
         
         for (int i = 0; i<N; i++)
             gt[i] = new GeneratorThread(i, X, q);
         
         for (int k = 0; k<M; k++)
             wt[k] = new WorkerThread(T, TT, q, u, k+1);
         
        //avvio dei thread    
         for(GeneratorThread generator : gt)        //maggiore leggibilità
            generator.start();
            
         for(WorkerThread worker : wt)
             worker.start();
         
         p1.start();
         p2.start();
         
         Thread.sleep(10000);                       //tempo di lavoro dei thread prima dell'interruzione
         
        //interruzione dei thread
         for(GeneratorThread generator : gt)
             generator.interrupt();
         
         for(WorkerThread worker : wt)
             worker.interrupt();
         
         p1.interrupt();
         p2.interrupt();
         
         //per assicurarsi che i thread figli abbiano terminato correttamente (es. output dopo interruzione)
         for(GeneratorThread generator : gt)
             generator.join();
         
         for(WorkerThread worker : wt)
             worker.join();
         
         p1.join();
         p2.join();
         
         count = p1.getCount() + p2.getCount();     //calcolo totale array stampati
         
         //risultati finali programma        
         System.out.println("Il numero di valori ancora in coda e' " + q.size());
         System.out.println("Numero di array stampati: " + count);    
    }
    
    
    static class Queue {
        private final int L;
        //private final int id;         id non usato, create due classi Queue e Uscita per le code
        public final int M;
        private final ArrayList<Integer> a = new ArrayList<>();
        private int count = 0;            
        
        public Queue (int L, int M) {   //int M non count
            this.L = L;
            this.M = M;
        }
        
        public synchronized void put(int v) throws InterruptedException {
            while (a.size() == L)       //aggiunta condizione di attesa perché i gt generino nuovi numeri
                wait();
            
            a.add(v);
            
            notifyAll();            //sblocca eventuali worker in attesa
        }
        
        public synchronized int get() throws InterruptedException {
            while(a.isEmpty() || count == M)
                wait();
           
            int v = a.get(0);       //aggiunto get
            count++;
            
            /* --> il calcolo del valore da inserire nell'array di uscita lo la GeneratorT, non va qui
            int r = a[0] * id;
            count++;
            }
            */
            
            if(count == M) {        //tutti gli worker hanno estratto il valore
                a.remove(0);
                count = 0;
                notifyAll();        //quando rimuovi allora notifica ai Generator
            }            
            
            return v;
        }
        
        public synchronized int size() {        //per output finale di eventuali valori ancora in coda
            return a.size();
        }
    }
    
    
    static class Uscita {
        private final ArrayList<Message> output;
        
        public Uscita(){
            this.output = new ArrayList<>();
        }
        
        public synchronized void put(Message messaggio) {            
            output.add(messaggio);      //aggiungo direttamente l'array messaggio -> prima inserivano singoli valori
            
            notifyAll();                //il wait riferito è getMessage -> sblocca i PrintThread
        }
        
        public synchronized Message getMessage() throws InterruptedException {       //il tipo è Messaggio
            while(output.isEmpty())     //inserendo direttamente il messaggio intero nell'uscita non devo controllare se ha raggiunto la grandezza M, ma che non sia vuoto
                wait();
            
            return output.remove(0);    //dall'uscita viene preso e rimosso il primo messaggio inserito
        }
        
        /*  --> usata per controllare non rimanessero messaggi
        public synchronized int size() {
            return output.size();
        }
        */
    }
    
    static class GeneratorThread extends Thread {
        private final int X;                            //serve X non N
        private int value;
        private final int id;
        
        //aggiunte var count e q
        private int count = 0;
        private final Queue q;
        
        
        public GeneratorThread(int id, int X, Queue q) {        // no [] spiega
            //this.N = N;
            this.id = id;
            this.X = X;
            this.q = q;
            value = id * 100 + 1;                               //il valore iniziale
        }
        
        @Override
        public void run() {
            try{
                while(!isInterrupted()) {
                    q.put(value++);                             //genera la squenza prendendo value e incrementandolo per il ciclo dopo
                    count++;
                    Thread.sleep(X);
                }
            }
            catch(InterruptedException e) {}
            
            //numeri prodotti dal gt in questione
            System.out.println("GeneratorThread n. " + id + " ha prodotto: " + count + " valori");
        }
    }
    
    static class WorkerThread extends Thread {

        private final int T;
        private final int TT;
        private final Queue q;
        private final Uscita u;
        private int count = 0;
        private final int id;
        private static final Object barrier = new Object();     //creo oggetto condiviso da tutti gli worker 
        private static int attesa = 0;                          //quanti worker sono in attesa di riprendere a lavorare
        private static int[] finiti;                            //vettore condiviso da tutti gli WorkerThread
        private static boolean init = false;                    //il vettore finiti è già stato inizilizzato?
        
        public WorkerThread(int T, int TT, Queue q, Uscita u, int id) {
            this.T = T;
            this.TT = TT;
            this.q = q;
            this.u = u;
            this.id = id;
        
            
            synchronized (WorkerThread.class) {     //sincronizza gli accessi (lock) per tutti i thread della classe WorkerThread (monitor)
                if (!init) {
                    finiti = new int[q.M];
                    init = true;
                }
            }
        }
        
        @Override
        public void run() {
            try{
                while(!isInterrupted()) {
                    int n = q.get();                            //ogni worker ha il proprio n, lo dichiaro in run quindi
                    Thread.sleep(T + (int)(Math.random()*TT));  //come randomizzare corretto
                    finiti[id - 1] = n * id;                    //calcolo del risultato aggiunto
                    
                    count ++;
                    
                    synchronized (barrier) {                        //barrier serva come monitor per sincroniccare gli WorkerThread
                        attesa++;
                        if (attesa < finiti.length) {
                            barrier.wait();                         //attendono che tutti abbiano finito
                        } else {
                            u.put(new Message(finiti.clone()));     //ultimo worker inserisce copia di finiti, sennò sovrascriverei messaggio col successivo
                            attesa = 0;
                            barrier.notifyAll();                    //sblocca tutti gli altri
                        }
                    }
                }
            }
            catch(InterruptedException e) {}
            
            //numeri prodotti dal gt in questione
            System.out.println("WorkerThread n. " + id + " ha calcolato: " + count + " valori");
        }
    }
    
    static class PrintThread extends Thread {
        //private Message messaggio;                messo in run per chiarezza, riusata da tutti i cicli, rischio di doverlo sincronizzare per uso di più thread
        private final Uscita u;
        private int count = 0;
        private final int id;
        
        public PrintThread(Uscita u, int id) {
            this.u = u;
            this.id = id;
        }
        
        @Override
        public void run() {
            try{
                while(!isInterrupted()) {
                    Message messaggio = u.getMessage();     //serve solo temporaneamente per ogni iterazione
                    System.out.println("PrintThread" + id + ": " + messaggio );
                    count++;
                }
            }
            catch(InterruptedException e) {}
            
            //numeri prodotti dal pt in questione
            System.out.println("PrintThread n. " + id + " ha stampato: " + count + " valori");
        }
        
        public int getCount() {
            return count;               //ritorna il numero di vettori stampati dal Printer in questione
        }
    }
    
    static class Message {                      //perché non serviva?
        private final int[] vett;
        
        public Message(int[] length) {
            vett = length;                      //clone dell'array
            }
        
        @Override
        public String toString() {
            return Arrays.toString(vett);       //visualizzazione array (perché)
        }           
    }
}