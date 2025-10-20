import java.lang.Thread.State;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Semaphore;

public class TestSuite {

  // **** Tests top-level ******************************************************

  public void tests() {
    describe("Test file creation and listing");
    testAvailableFiles();

    describe("Test single threaded open/read/write/close");
    testSingleThread();

    describe("Test multithreading locking");
    testMultiThreadRead();
    testMultiThread1();
    testMultiThread2();
    testMultiThread3();
    testMultiThread4();
    testMultiThread5();
  }

  public void testAvailableFiles() {
    FileServer fs = newFileServer();

    try {
      // Base
      it("No available files");
      assertEquals(fs.availableFiles().size(), 0);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      // Step
      it("One available file. Successful creation");
      fs.create("a", "hello");
      assertEquals(
          fs.availableFiles().toArray(new String[0]),
          new String[] { "a" });
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Initially closed file");
      assertEquals(fs.fileStatus("a"), Mode.CLOSED);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      // Step step
      it("Two available files");
      fs.create("b", "hello2");
      assertEquals(
          fs.availableFiles().toArray(new String[0]),
          new String[] { "a", "b" });
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Unknown file status");
      assertEquals(fs.fileStatus("c"), Mode.UNKNOWN);
    } catch (Exception e) {
      failure("Exception");
    }

    Optional<File> of = null;
    try {
      it("Opening a file (read) keeps the same available files");
      of = fs.open("a", Mode.READABLE);
      assertEquals(
          fs.availableFiles().toArray(new String[0]),
          new String[] { "a", "b" });
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      fs.close(of.get());
      it("File still avilable after closing");
      assertEquals(fs.availableFiles().toArray(new String[0])[0], "a");
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Opening a file (write) keeps the same available files");
      fs.open("a", Mode.READWRITEABLE);
      assertEquals(
          fs.availableFiles().toArray(new String[0]),
          new String[] { "a", "b" });
    } catch (Exception e) {
      failure("Exception");
    }
  }

  public void testSingleThread() {
    FileServer fs = newFileServer();
    fs.create("a", "hello");
    fs.create("b", "world");

    Optional<File> f = null;
    try {
      it("Read mode test -- successful open");
      f = fs.open("a", Mode.READABLE);
      assertEquals(f.isPresent(), true);
    } catch (Exception e) {
      failure("Exception: " + e.getLocalizedMessage());
    }

    try {
      it("Read mode test -- successful read");
      assertEquals(f.get().read(), "hello");
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Read mode test -- file status");
      assertEquals(fs.fileStatus("a"), Mode.READABLE);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Read mode test -- Closing");
      fs.close(f.get());
      assertEquals(fs.fileStatus("a"), Mode.CLOSED);
    } catch (Exception e) {
      failure("Exception");
    }

    Optional<File> ofw = null;
    try {
      it("Write mode test -- successful open");
      ofw = fs.open("a", Mode.READWRITEABLE);
      assertEquals(ofw.isPresent(), true);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Write mode test -- file status");
      assertEquals(fs.fileStatus("a"), Mode.READWRITEABLE);
    } catch (Exception e) {
      failure("Exception");
    }

    File fw = null;
    Optional<File> ofw2 = null;
    try {
      it("Write mode test -- writing works");
      fw = ofw.get();
      fw.write("wibble");

      fs.close(fw);

      ofw2 = fs.open("a", Mode.READABLE);
      assertEquals(ofw2.get().read(), "wibble");
      fs.close(ofw2.get());
    } catch (Exception e) {
      failure("Exception");
    }

    it(
        "Reclosing a file in the wrong mode (i.e. now reading file but trying to close write mode file) does not change the state or its contents");
    try {
      fw.write("plop");
      fs.close(fw);
      Optional<File> ofwp = fs.open("a", Mode.READABLE);
      assertEquals(ofwp.get().read(), "wibble");
      fs.close(ofwp.get());
    } catch (Exception e) {
      failure("");
      e.printStackTrace();
    }

    Optional<File> ofb = null;
    try {
      it("Write mode test -- non interference with other files");
      ofb = fs.open("b", Mode.READWRITEABLE);
      assertEquals(ofb.get().read(), "world");
    } catch (Exception e) {
      failure("Exception");
    }

    Optional<File> ofa = null;
    try {
      it("Write mode test -- two independent files open for write allowed");
      ofa = fs.open("a", Mode.READWRITEABLE);
      assertEquals(ofa.isPresent(), true);
    } catch (Exception e) {
      failure("Exception");
    }

    File fb = null;
    File fa = null;
    Optional<File> ofa2 = null;
    Optional<File> ofb2 = null;
    try {
      it("Write mode test -- two independent files non-intefering");
      fb = ofb.get();
      fb.write("wobble");
      fs.close(fb);
      fa = ofa.get();
      fa.write("waggle");
      fs.close(fa);
      ofa2 = fs.open("a", Mode.READABLE);
      ofb2 = fs.open("b", Mode.READABLE);
      assertEquals(ofa2.get().read(), "waggle");
      assertEquals(ofb2.get().read(), "wobble");
      fs.close(ofa2.get());
      fs.close(ofb2.get());
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Closed status");
      assertEquals(fs.fileStatus("a"), Mode.CLOSED);
      assertEquals(fs.fileStatus("b"), Mode.CLOSED);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Reclosing a file doesn't do anything");
      fs.close(ofa2.get());
      assertEquals(fs.fileStatus("a"), Mode.CLOSED);
    } catch (Exception e) {
      failure("Exception");
    } catch (Error e) {
      failure("Error");
    }

    Optional<File> ff;
    try {
      it("Opening a file in closed mode returns Optional.empty()");
      ff = fs.open("a", Mode.CLOSED);
      assertEquals(ff.isPresent(), false);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Opening a file in unknown mode returns Optional.empty()");
      Optional<File> fg = fs.open("a", Mode.UNKNOWN);
      assertEquals(fg.isPresent(), false);
    } catch (Exception e) {
      failure("Exception");
    }
  }

  public void testMultiThreadRead() {
    /*
     * Create server with 'a and 'b'
     * 
     * Open 'a' for R . Close
     * | Open 'a' for R . Close
     * | Open 'b' for R
     * 
     * Check that there is no blocking
     */

    FileServer fs = newFileServer();
    fs.create("a", "coheed");
    fs.create("b", "cambria");

    Thread t1 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            it("Multi threads can read consistent state (thread 1)");
            try {
              Optional<File> ofa1 = fs.open("a", Mode.READABLE);
              File fa1 = ofa1.get();

              assertEquals(fa1.read(), "coheed");
              fs.close(fa1);
            } catch (Exception e) {
              failure("");
              e.printStackTrace();
            }
          }
        });
    Thread t2 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            it("Multi threads can read consistent state (thread 2)");
            try {
              Optional<File> ofa2 = fs.open("a", Mode.READABLE);
              File fa2 = ofa2.get();

              assertEquals(fa2.read(), "coheed");
              fs.close(fa2);
            } catch (Exception e) {
              failure("");
              e.printStackTrace();
            }
          }
        });
    Thread t3 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            it("Multi threads can read (different file state) (thread 3)");
            try {
              Optional<File> ofa3 = fs.open("b", Mode.READABLE);
              File fa3 = ofa3.get();
              assertEquals(fa3.read(), "cambria");
            } catch (Exception e) {
              failure("");
              e.printStackTrace();
            }
          }
        });
    t1.start();
    t2.start();
    t3.start();
    try {
      t1.join(300);
      t2.join(300);
      t3.join(300);
    } catch (InterruptedException e) {
      assertEquals(false, true);
    }
    it("Multiple read is allowed with no blocking (thread 1)");
    assertEquals(t1.getState(), Thread.State.TERMINATED);

    it("Multiple read is allowed with no blocking (thread 2)");
    assertEquals(t2.getState(), Thread.State.TERMINATED);

    it(
        "Multiple read (to another file) is allowed with no blocking (thread 3)");
    assertEquals(t3.getState(), Thread.State.TERMINATED);
  }

  public void testMultiThread1() {
    FileServer fs = newFileServer();
    fs.create("a", "coheed");
    fs.create("b", "cambria");

    // -----------------------------------------------------------------------------
    // Scenario 1:
    // Thread main: opens A for reading
    // Thread wt1: open A for writing
    // - GETS BLOCKED
    // Thread main: closes A
    // Thread wt1: -- GETS UNBLOCKED
    // closes A

    Thread main = new Thread(
        new Runnable() {

          @Override
          public void run() {
            Optional<File> ofA = fs.open("a", Mode.READABLE);

            Signal signal = new Signal();
            signal.flag = false;

            // Start off another thread which gets blocked
            Thread wt1 = new Thread(
                new Runnable() {

                  @Override
                  public void run() {
                    try {
                      Optional<File> ofaw1 = fs.open("a", Mode.READWRITEABLE);
                      File faw1 = ofaw1.get();
                      it(
                          "Process trying to open in write mode has been eventually unblocked (unblocked signal should be true)");
                      assertEquals(signal.flag, true);
                      fs.close(faw1);
                    } catch (Exception e) {
                      // In case an exception happens
                      it(
                          "Process trying to open in write mode has been eventually unblocked (unblocked signal should be true)");
                      failure("");
                      e.printStackTrace();
                    }
                  }
                });
            wt1.start();
            try {
              wt1.join(500);
            } catch (InterruptedException e) {
              failure("Interrupt");
            }
            // Detect that the thread is blocked
            it(
                "File open for reading; another process trying to open it for writing is blocked");
            assertEquals(wt1.getState(), Thread.State.WAITING);

            // Set observable checkpoint in other thread
            signal.flag = true;
            // Triggers unblock of wt1
            fs.close(ofA.get());

            try {
              wt1.join(500);
            } catch (InterruptedException e) {
              failure("Interrupt");
            }

            it("Successfuly unblocked writing process");
            assertEquals(wt1.getState(), Thread.State.TERMINATED);
          }
        });
    main.start();

    try {
      main.join(1000);
    } catch (InterruptedException e) {
      failure("Interrupt");
    }

    it("Successfuly closed files after blocking interaction");
    assertEquals(main.getState(), Thread.State.TERMINATED);
  }

  public void testMultiThread2() {
    FileServer fs = newFileServer();
    fs.create("a", "coheed");
    fs.create("b", "cambria");

    /*
     * Situation
     * Main: Open b for Read
     * sub1: Open a for Write
     * -- check success
     * (WAIT on SEMAPHORE)
     * sub2: Open a for Read
     * -- check it gets block
     * kill this thread
     * Main: Open b for Read (not blocked)
     * Read b . Close b
     * sub3: Open a for write
     * -- check it gets blocked
     * Main: signal sub1 to procedd
     * sub1: write "claudio" to a
     * close a
     * sub3 should get unblocked
     * -- check unblocked
     * -- get the file and read it 'claudio'
     * -- write 'ambelina'
     * -- close a
     * -- check sub3 is not blocked and termintes
     */
    // Open b
    Optional<File> ofb = fs.open("b", Mode.READABLE);

    Semaphore signaller = new Semaphore(1);
    try {
      signaller.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    Signal doneOpen = new Signal();
    Signal signal = new Signal();

    Thread sub1 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            try {
              Optional<File> ofa1 = fs.open("a", Mode.READWRITEABLE);
              File fa1 = ofa1.get();
              it(
                  "Process with write mode of file succeeds in reading while blocking others");
              assertEquals(fa1.read(), "coheed");
              doneOpen.flag = true;

              // Wait
              signaller.acquire();
              fa1.write("claudio");
              // Set observable checkpoint in other thread
              signal.flag = true;
              // Triggers unblock
              fs.close(fa1);
            } catch (Exception e) {
              failure("");
              e.printStackTrace();
            }
          }
        });
    sub1.start();

    // Wait and check that it succesfully opened
    try {
      sub1.join(500);
    } catch (InterruptedException e) {
      failure("Interrupt");
    }
    it(
        "Another process trying to read whilst another writes gets blocked. Successful open and read.");
    assertEquals(doneOpen.flag, true);

    // Start off another thread which gets blocked
    Thread sub2 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            Optional<File> ofaa = fs.open("a", Mode.READABLE);
            File faa = ofaa.get();
            // Eventually close
            fs.close(faa);
          }
        });
    sub2.start();
    try {
      sub2.join(500);
    } catch (InterruptedException e) {
      failure("Interrupt");
    }

    // Detect that the thread is blocked
    assertEquals(sub2.getState(), Thread.State.WAITING);
    // Give up on the thread

    it(
        "Another process trying to read a file (while another writes a different file) is not blocked");
    Optional<File> ofb2 = fs.open("b", Mode.READABLE);

    try {
      File fb2 = ofb2.get();
      assertEquals(fb2.read(), "cambria");
      fs.close(fb2);
    } catch (Exception e) {
      failure("Exception");
    }

    signal.flag = false;

    Thread sub3 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            try {
              Optional<File> ofa2 = fs.open("a", Mode.READWRITEABLE);
              // I'm unblocked!
              it("Second write attempt gets unblocked");
              assertEquals(signal.flag, true);

              // Signalled to go (the signal will come after the
              // other thread has closed)
              File fa2 = ofa2.get();

              it(
                  "Unlocked process sees write from previous locking write process");
              try {
                assertEquals(fa2.read(), "claudio");
                fa2.write("ambelina");
                fs.close(fa2);
              } catch (Exception e) {
                failure("");
                e.printStackTrace();
              }
            } catch (Exception e) {
              failure("");
              e.printStackTrace();
            }
          }
        });
    sub3.start();
    // Let a bit of time elapse to allow the thread to get blocked
    try {
      sub3.join(500);
    } catch (InterruptedException e) {
      failure("Interrupt");
    }
    // Detect block
    it("Another process trying to write whilst another writes gets blocked");
    assertEquals(sub3.getState(), Thread.State.WAITING);

    // Make the sub1 close the file, unclocking sub3
    signaller.release();

    // Let a bit of time elapse to allow the thread to get unblocked
    try {
      sub3.join(500);
    } catch (InterruptedException e) {
      failure("Interrupt");
    }
    // Detect block
    it("Second write attempt was indeed unblocked");
    assertEquals(sub3.getState(), Thread.State.TERMINATED);

    try {
      sub2.join(500);
    } catch (InterruptedException e) {
      failure("Interrupt");
    }

    try {
      // Detect that the thread 2 was unblocked eventually
      it("Blocked reader thread was eventually unblocked");
      assertEquals(sub2.getState(), Thread.State.TERMINATED);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Second write attempt gets unblocked, and now the file is closed");
      assertEquals(fs.fileStatus("a"), Mode.CLOSED);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it(
          "Two processed open 'b' for reading, but only one closed it, so it should still be marked as readable");
      assertEquals(fs.fileStatus("b"), Mode.READABLE);
    } catch (Exception e) {
      failure("Exception");
    }

    try {
      it("Second write change observed");
      Optional<File> fa4 = fs.open("a", Mode.READABLE);
      assertEquals(fa4.get().read(), "ambelina");
    } catch (Exception e) {
      failure("Exception");
    }
  }

  public void testMultiThread3() {
    FileServer fs = newFileServer();
    fs.create("a", "hello");

    // Oppen a file for reading and close
    Optional<File> fileReaderO = fs.open("a", Mode.READABLE);
    File fR = fileReaderO.get();
    fs.close(fR);

    // Open "a" for writing
    Optional<File> fileWriterO1 = fs.open("a", Mode.READWRITEABLE);
    File fW1 = fileWriterO1.get();

    // Re-close "a" for reading (bug here)
    fs.close(fR);

    // Now do an illegal open "a" for writing
    Thread w = new Thread(
        new Runnable() {

          @Override
          public void run() {
            Optional<File> fileWriterO2 = fs.open("a", Mode.READWRITEABLE);
            File fW2 = fileWriterO2.get();
          }
        });
    w.start();
    try {
      w.join(500);
    } catch (Exception e) {
    }
    it("Thread should be blocked trying to re-open for writing");
    assertEquals(w.getState(), Thread.State.WAITING);
  }

  public void testMultiThread4() {
    FileServer fs = newFileServer();
    fs.create("a", "hello");
    fs.create("b", "world");

    Thread wt1 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            Optional<File> fileRm1 = fs.open("a", Mode.READWRITEABLE);
          }
        });
    wt1.start();

    Thread wt2 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            Optional<File> fileRm1 = fs.open("b", Mode.READWRITEABLE);
          }
        });
    wt2.start();

    try {
      wt1.join(200);
      wt2.join(200);
      it("Two independent writers open");
      assertEquals(wt1.getState(), Thread.State.TERMINATED);
      assertEquals(wt2.getState(), Thread.State.TERMINATED);
    } catch (Exception e) {
    }
  }

  public void testMultiThread5() {
    // Good for detecting problems with write-read exclusion which was not in the
    // given tests.
    FileServer fs = newFileServer();
    fs.create("a", "coheed");

    // -----------------------------------------------------------------------------
    // Scenario
    // Thread wt1: opens A for writing
    // Thread w21: open A for reading
    // - SHOULD GET BLOCKED

    // Start off a thread which gets blocked
    Thread wt1 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            Optional<File> fileAm = fs.open("a", Mode.READWRITEABLE);
          }
        });

    wt1.start();
    try {
      Thread.sleep(200);
    } catch (Exception e) {
    }
    // Start off a thread which gets blocked
    Thread wt2 = new Thread(
        new Runnable() {

          @Override
          public void run() {
            Optional<File> fileA2m = fs.open("a", Mode.READABLE);
          }
        });
    wt2.start();

    try {
      wt2.join(500);
      wt1.join(500);
    } catch (InterruptedException e) {
      failure("Interrupt");
    }
    // Detect that the thread is blocked
    it("write-read exclusion: reader coming after should get blocked");
    assertEquals(wt2.getState(), Thread.State.WAITING);
    it("write-read exclusion: writer coming before not blocked");
    assertEquals(wt1.getState(), Thread.State.TERMINATED);
  }

  // ************ TEST HARNESS *************************************************

  public String className;

  public static final String ANSI_RED = "\u001B[31m\033[1m";
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_GREEN = "\u001B[32m\033[1m";
  public static final String ANSI_BLUE = "\u001B[34m\033[1m";

  private Integer testCount = 0;
  private Integer passedTests = 0;
  private String currentTestName = "";
  private boolean prevTestFailed = false;

  public static void main(String[] args) {
    System.out.println("CO661 - Assessment 1 - Test Suite v1.3s");
    // First string provides that name of your FileServer class
    if (args.length < 1) {
      System.out.println(
          "Please pass the name of your FileServer class as an argument");
    } else {
      // Ok
      String className = args[0];
      TestSuite ts = new TestSuite(className);
      ts.go();
    }
  }

  public TestSuite(String className) {
    this.className = className;
  }

  public FileServer newFileServer() {
    // Create a new JavaClassLoader
    ClassLoader classLoader = this.getClass().getClassLoader();
    // Load the target class using its binary name
    FileServer fs = null;

    try {
      Class loadedMyClass = classLoader.loadClass(className);
      // System.out.println("Loaded class name: " + loadedMyClass.getName());

      // Create a new instance from the loaded class
      Constructor constructor = loadedMyClass.getConstructor();
      Object myClassObject = constructor.newInstance();
      fs = (FileServer) myClassObject;
    } catch (ClassNotFoundException e) {
      System.out.println("Error: Could not find class " + className);
      System.exit(1);
    } catch (NoSuchMethodException e) {
      System.out.println(
          "Error: " + className + " is missing its constructor.");
      System.exit(1);
    } catch (Exception e) {
      System.out.println("Error: " + className + " could not be instantiated.");
      System.exit(1);
    }

    return fs;
  }

  public void go() {
    tests();
    System.out.println("\n" + ANSI_BLUE + "Tests: " + testCount + ANSI_RESET);
    System.out.println(ANSI_GREEN + "Passed: " + passedTests + ANSI_RESET);
    if (passedTests == testCount) {
      System.out.println("\nOk.");
    } else {
      System.out.println(
          ANSI_RED + "Failed: " + (testCount - passedTests) + ANSI_RESET);
    }

    describe("Done");
  }

  public void describe(String msg) {
    System.out.println("\n" + msg);
  }

  public void it(String msg) {
    this.currentTestName = msg;
  }

  // Messages
  public synchronized void success() {
    this.passedTests++;
    this.prevTestFailed = false;
    System.out.print(".");
  }

  public synchronized void failure(String msg) {
    if (!this.prevTestFailed) {
      System.out.print("\n");
    }
    System.out.println(
        ANSI_RED + currentTestName + ".\n\tFailed: " + msg + "\n" + ANSI_RESET);
    this.prevTestFailed = true;
  }

  // Assertion boilerplate
  public synchronized void assertEquals(String s1, String s2) {
    if (s1.equals(s2)) {
      success();
    } else {
      failure("Expected " + s2 + " got " + s1);
    }
    this.testCount++;
  }

  // Assertion boilerplate
  public synchronized void assertEquals(String[] s1, String[] s2) {
    boolean eq = (s1.length == s2.length);
    for (int i = 0; i < s1.length; i++) {
      eq = eq & (s1[i].equals(s2[i]));
    }
    if (eq) {
      success();
    } else {
      failure("Expected " + s2 + " got " + s1);
    }
    this.testCount++;
  }

  public synchronized void assertEquals(int s1, int s2) {
    if (s1 == s2) {
      success();
    } else {
      failure("Expected " + s2 + " got " + s1);
    }
    this.testCount++;
  }

  public synchronized void assertEquals(boolean s1, boolean s2) {
    if (s1 == s2) {
      success();
    } else {
      failure("Expected " + s2 + " got " + s1);
    }
    this.testCount++;
  }

  public synchronized void assertEquals(Mode s1, Mode s2) {
    if (s1 == s2) {
      success();
    } else {
      failure("Expected " + s2 + " got " + s1);
    }
    this.testCount++;
  }

  public synchronized void assertEquals(Thread.State s1, Thread.State s2) {
    if (s1 == s2) {
      success();
    } else {
      failure("Expected " + s2 + " got " + s1);
    }
    this.testCount++;
  }
}

class Signal {
  public boolean flag;
}