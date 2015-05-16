
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.Arrays;

class MssqArea {

    double node;                    /* time integrated number in the node  */

    double queue;                   /* time integrated number in the queue */

    double service;                 /* time integrated number in service   */


    void initAreaParas() {
        node = 0.0;
        queue = 0.0;
        service = 0.0;
    }
}

class MssqT {

    double arrival;                 /* next arrival time                   */

    double completion;              /* next completion time                */

    double current;                 /* current time                        */

    double next;                    /* next (most imminent) event time     */

    double last;                    /* last arrival time                   */

}

class MssqS {

    int number;                     /* number serviced                     */

    double completion = Integer.MAX_VALUE;              /* next completion time      */

    double service;                     /* length of service */

    boolean idle = true;                /* current status                      */

}

class Mssq {

    static double START = 0.0;              /* initial time                   */

    static double STOP = 20000.0;          /* terminal (close the door) time */

    static double INFINITY = 100.0 * STOP;  /* must be much larger than STOP  */

    static double sarrival = START;              /* Why did I do this?       */


    public static void main(String[] args) {

        long index = 0;                  /* used to count departed jobs         */
        long number = 0;                  /* number in the node                  */
        int servernum = 4;               /* number of servers                  */
        int nextServer = -1;

        Mssq s = new Mssq();

        MssqS[] server = new MssqS[servernum];
        for (int i = 0; i < server.length; i++) {
            server[i] = new MssqS();
        }

        Rngs r = new Rngs();
        r.plantSeeds(456789);

        MssqT t = new MssqT();
        t.current = START;           /* set the clock                         */
        t.arrival = s.getArrival(r); /* schedule the first arrival            */
        t.completion = INFINITY;        /* the first event can't be a completion */

        MssqArea area = new MssqArea();
        area.initAreaParas();

        while ((t.arrival < STOP) || (number > 0)) {
            t.next = Math.min(t.arrival, t.completion);  /* next event time   */
            if (number > 0) {                              /* update integrals  */
                area.node += (t.next - t.current) * number;
                area.queue += (t.next - t.current) * (number - 1);
                area.service += (t.next - t.current);
                if (nextServer >= 0) {
                    server[nextServer].service += (t.next - t.current);
                }
            }
            t.current = t.next;                    /* advance the clock */
            if (t.current == t.arrival) {               /* process an arrival */
                number++;
                t.arrival = s.getArrival(r);
                if (t.arrival > STOP) {
                    t.last = t.current;
                    t.arrival = INFINITY;
                }
                long temp = number;
                for (int i = 0; i < server.length; i++) {       /* check if idle servers exist */
                    if (server[i].idle && temp > 0) {
                        temp--;
                        server[i].idle = false;
                        server[i].completion = s.getService(r) + t.current;
                    }
                }
                double nextComplete = INFINITY;                    /* check next completion */
                for (int i = 0; i < server.length; i++) {
                    if (server[i].completion < nextComplete && !server[i].idle) {
                        nextServer = i;
                        nextComplete = server[i].completion;
                    }
                }
                t.completion = nextComplete;
            } else {                                        /* process a completion */
                index++;
                number--;
                server[nextServer].idle = true;
                server[nextServer].number++;
                if (number > 0) {
                    server[nextServer].idle = false;
                    server[nextServer].completion = s.getService(r) + t.current;
                } else {
                    server[nextServer].completion = INFINITY;
                    nextServer = -1;
                }
                double nextComplete = INFINITY;                    /* check next completion */
                for (int i = 0; i < server.length; i++) {
                    if (server[i].completion < nextComplete && !server[i].idle) {
                        nextServer = i;
                        nextComplete = server[i].completion;
                    }
                }
                t.completion = nextComplete;
            }
        }

        DecimalFormat f = new DecimalFormat("###0.00");

        System.out.println("\nfor " + index + " jobs");
        System.out.println("   average interarrival time =   " + f.format(t.last / index));
        System.out.println("   average wait ............ =   " + f.format(area.node / index));
        System.out.println("   average delay ........... =   " + f.format(area.queue / index));
        System.out.println("   average service time .... =   " + f.format(area.service / index));
        System.out.println("   average # in the node ... =   " + f.format(area.node / t.current));
        System.out.println("   average # in the queue .. =   " + f.format(area.queue / t.current));
        System.out.println("   utilization ............. =   " + f.format(area.service / t.current));

        System.out.println("SERVER DATA \n");
        for (int i = 0; i < server.length; i++) {
            System.out.println("SERVER " + (i + 1));
            System.out.println("   number of jobs ........... =   " + f.format(server[i].number));
            System.out.println("   service time ............. =   " + f.format(server[i].service));
            System.out.println("");
        }
    }

    double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }

    double uniform(double a, double b, Rngs r) {
        /* ------------------------------------------------
         * generate an Uniform random variate, use a < b
         * ------------------------------------------------
         */
        return (a + (b - a) * r.random());
    }

    double getArrival(Rngs r) {
        /* ---------------------------------------------
         * generate the next arrival time, with rate 1/2
         * ---------------------------------------------
         */
        r.selectStream(0);
        sarrival += exponential(2.0, r);
        return (sarrival);
    }

    double getService(Rngs r) {
        /* --------------------------------------------
         * generate the next service time with rate 2/3
         * --------------------------------------------
         */
        r.selectStream(1);
        return (uniform(1.0, 2.0, r));
    }
}
