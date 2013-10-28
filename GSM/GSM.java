package gsm;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GSM {

    private static int calls = 100;
    private static int towers = 50;
    private static int threads = 15;
    private static Call callsArr[] = new Call[calls];
    private static Tower towersArr[] = new Tower[towers];
    private static int numberCalls[] = new int[towers];
    private static int time = 10;

    public static void main(String[] args) {
        generateBinFiles();

        try {

            long before = System.currentTimeMillis();
            Thread t1 = new Thread(new CallsFromFile());
            Thread t2 = new Thread(new TowersFromFile());
            t1.start();
            t2.start();

            Thread t3[] = new Thread[threads - 2];

            int loops_count = calls / (threads - 2);
            int last_loop = calls - calls / (threads - 2) * (threads - 3);
            for (int t = 0; t < threads - 2; t++) {
                int loop;
                if (t != threads - 3) {
                    loop = loops_count;
                } else {
                    loop = last_loop;
                }
                for (int i = 0; i < loop; i++) {
                    for (int j = 0; j < towers; j++) {
                        if (callsArr[i] == null) {
                            callsArr[i] = new Call();
                        }
                        if (towersArr[j] == null) {
                            towersArr[j] = new Tower();
                        }
                        t3[t] = new Thread(new CalcInAreaCalls(j, i));
                        t3[t].start();
                    }
                }
                t3[t].join();
            }

            t1.join();
            t2.join();
            long after = System.currentTimeMillis();
            System.out.println("time = " + (after - before) + " ms");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        for (int i = 0; i < towers; i++) {
            System.out.println(numberCalls[i]);
        }
    }

    private static void generateBinFiles() {
        try {
            Random rand = new Random();
            for (int i = 0; i < calls; i++) {
                Path file = Paths.get("src", String.valueOf(i) + "c.bin");
                byte[] buf = {(byte) (rand.nextInt(10) + 1), (byte) (rand.nextInt(10) + 1), (byte) (rand.nextInt(10) + 1), (byte) (rand.nextInt(10) + 1)};
                Files.write(file, buf);
            }
            for (int i = 0; i < towers; i++) {
                Path file = Paths.get("src", String.valueOf(i) + "t.bin");
                byte[] buf = {(byte) (rand.nextInt(10) + 1), (byte) (rand.nextInt(10) + 1), (byte) (rand.nextInt(10) + 1), (byte) (rand.nextInt(10) + 1),
                    (byte) (rand.nextInt(4) + 1), (byte) (rand.nextInt(4) + 1)};
                Files.write(file, buf);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class CalcInAreaCalls extends Thread {

        public CalcInAreaCalls(int i, int j) {
            this.j = j;
            this.i = i;
        }
        int j;
        int i;

        @Override
        public void run() {
            try {
                synchronized (callsArr[j]) {
                    while (callsArr[j].x == 0 || callsArr[j].y == 0) {
                        callsArr[j].wait();
                    }
                }
                synchronized (towersArr[i]) {
                    while (towersArr[i].x == 0 || towersArr[i].y == 0 || towersArr[i].r == 0) {
                        towersArr[i].wait();
                    }
                }
                if ((towersArr[i].x - towersArr[i].r) <= callsArr[j].x && callsArr[j].x
                        <= (towersArr[i].x + towersArr[i].r)) {
                    numberCalls[i] += 1;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class CallsFromFile extends Thread {

        @Override
        public void run() {
            try {
                for (int i = 0; i < calls; i++) {
                    sleep(time);
                    Path file = Paths.get("src", String.valueOf(i) + "c.bin");
                    byte[] fileArray;
                    fileArray = Files.readAllBytes(file);

                    if (callsArr[i] == null) {
                        callsArr[i] = new Call();
                    }
                    synchronized (callsArr[i]) {
                        callsArr[i].x = ((fileArray[0] & 0xFF) << 8) + ((fileArray[1] & 0xFF));
                        callsArr[i].y = ((fileArray[2] & 0xFF) << 8) + (fileArray[3] & 0xFF);
                        callsArr[i].notifyAll();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class TowersFromFile extends Thread {

        @Override
        public void run() {
            try {

                for (int i = 0; i < towers; i++) {
                    sleep(time);
                    Path file = Paths.get("src", String.valueOf(i) + "t.bin");
                    byte[] fileArray;
                    fileArray = Files.readAllBytes(file);

                    if (towersArr[i] == null) {
                        towersArr[i] = new Tower();
                    }
                    synchronized (towersArr[i]) {
                        towersArr[i].x = ((fileArray[0] & 0xFF) << 8) + ((fileArray[1] & 0xFF));
                        towersArr[i].y = ((fileArray[2] & 0xFF) << 8) + (fileArray[3] & 0xFF);
                        towersArr[i].r = ((fileArray[4] & 0xFF) << 8) + (fileArray[5] & 0xFF);
                        towersArr[i].notifyAll();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
