import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author: Sean Idisi
 * @version: 3.0
 */

/**
 * This Solution took me 40 minutes to think off and i am really proud of this
 * solution
 *
 * That being said this solution only got 51/52 for the test cases because
 * 
 * If you understand this alogithm and the crazy ideas that flow into my head
 * you understand that testMultiThread3() will never pass all tests cases if you
 * write out what happens on paper it is impossible unless line number 656 gets
 * commented out.
 */

class FileObject extends FileFrame {
    private String fileName;
    private MyFile fileRef;

    public FileObject(String fileName, String content, Mode mode, MyFile fileRef) {
        this.fileName = fileName;
        this.fileRef = fileRef;
        // I do not need the content on the fileObject just the fileRef
        super(null, mode);
    }

    public String getFileName() {
        return fileName;
    }

    public MyFile getFile() {
        return this.fileRef;
    }

    public Mode getMode() {
        return this.mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
}

class MyFile extends File {
    private Mode mode;
    private String content;
    private String filename;

    // constructor
    public MyFile(String filename, String content, Mode mode) {
        super(filename, content, mode);
        this.content = content;
        this.filename = filename;
        this.mode = mode;
    }

    // getter
    public String filename() {
        return this.filename;
    }

    // getter
    public Mode mode() {
        return this.mode;
    }

    // Read the file
    public String read() {
        return this.content;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setFileName(String fileName) {
        this.filename = fileName;
    }

    // Write the file if it has the write mode
    // return true if succesful, otherwise false (non writeable file)
    public boolean write(String content) {
        if (this.mode == Mode.READWRITEABLE) {
            this.content = content;
            return true;
        } else {
            return false;
        }
    }
}

public class MyFileServer implements FileServer {

    private List<AtomicInteger> readers;
    private List<AtomicInteger> writers;
    private List<FileObject> files;
    private List<Semaphore> semaphores;
    private List<Semaphore> writerSems;
    private List<Semaphore> closes;
    private List<Semaphore> enList;

    public MyFileServer() {
        this.files = new ArrayList<>();
        this.semaphores = new ArrayList<>();
        this.readers = new ArrayList<>();
        this.writers = new ArrayList<>();
        this.writerSems = new ArrayList<>();
        this.closes = new ArrayList<>();
        this.enList = new ArrayList<>();
    }

    @Override
    public void create(String filename, String content) {
        // Do not allow duplicate create with the same name
        int fileIndex = getFileIndex(filename);
        if (fileIndex != -1) {
            return;
        }
        this.writers.add(new AtomicInteger(0));
        this.readers.add(new AtomicInteger(0));
        this.files.add(new FileObject(filename, null, Mode.CLOSED, new MyFile(filename, content, Mode.CLOSED)));
        this.semaphores.add(new Semaphore(1));
        this.writerSems.add(new Semaphore(1));
        this.closes.add(new Semaphore(1));
        this.enList.add(new Semaphore(1));
    }

    public Integer getFileIndex(String fileName) {
        int l = 0;
        int r = files.size() - 1;
        files.sort((a, b) -> a.getFileName().compareTo(b.getFileName()));
        while (l <= r) {
            int mid = (r + l) / 2;
            int comp = files.get(mid).getFileName().compareTo(fileName);
            if (comp == 0) {
                return mid;
            } else if (comp < 0) {
                l = mid + 1;
            } else {
                r = mid - 1;
            }
        }
        return -1;
    }

    @Override
    public Optional<File> open(String filename, Mode mode) {
        // If read then fine
        // If write and currently is reading then block
        int fileIndex = getFileIndex(filename);
        // No file exists with such name
        if (fileIndex < 0 || mode == Mode.CLOSED || mode == Mode.UNKNOWN) {
            return Optional.empty();
        }
        FileObject file = this.files.get(fileIndex);

        try {
            this.enList.get(fileIndex).acquire();
            if (file.getMode() != mode) {
                semaphores.get(fileIndex).acquire();
                // Set the File to be the mode and acquire this mode

                // Set the Mode
                file.setMode(mode);
                file.getFile().setMode(mode);

                if (mode == Mode.READABLE) {
                    readers.get(fileIndex).incrementAndGet();
                } else {
                    // For writes there can on be 1 write at a time
                    this.writerSems.get(fileIndex).acquire();
                    writers.get(fileIndex).incrementAndGet();
                }
            } else {
                if (mode == Mode.READABLE) {
                    readers.get(fileIndex).incrementAndGet();
                } else {
                    // For writes there can on be 1 write at a time
                    this.writerSems.get(fileIndex).acquire();
                    writers.get(fileIndex).incrementAndGet();
                }
            }
            this.enList.get(fileIndex).release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(e);
        }
        return Optional.of(file.getFile());
    }

    public void LogWorkers(int fileIndex) {
        System.out.println(
                "Readers: " + readers.get(fileIndex).get() + " Writers: " + writers.get(fileIndex).get());
    }

    @Override
    public void close(File file) {
        int fileIndex = getFileIndex(file.filename());
        if (fileIndex < 0) {
            return;
        }
        FileObject f = files.get(fileIndex);
        if (f.getFile().mode() == Mode.CLOSED || f.getFile().mode() == Mode.UNKNOWN) {
            return;
        }

        if (f.getFile().mode() == Mode.READABLE) {
            readers.get(fileIndex).decrementAndGet();
        } else if (f.getMode() == Mode.READWRITEABLE) {
            writerSems.get(fileIndex).release();
            writers.get(fileIndex).decrementAndGet();
        } else {
            return;
        }

        try {
            this.closes.get(fileIndex).acquire();
            // Close the file
            // set the file to be closed
            if (readers.get(fileIndex).get() == 0 && writers.get(fileIndex).get() == 0) {
                // Set it to be closed before releasing
                files.get(fileIndex).setMode(Mode.CLOSED);
                files.get(fileIndex).getFile().setMode(Mode.CLOSED);
                // Relase for the next mode lock
                semaphores.get(fileIndex).release();
            }
            this.closes.get(fileIndex).release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(e);
        }
    }

    @Override
    public Set<String> availableFiles() {
        return files.stream()
                .map(FileObject::getFileName)
                .collect(Collectors.toSet());
    }

    @Override
    public Mode fileStatus(String filename) {
        int fileIndex = getFileIndex(filename);
        if (fileIndex < 0) {
            return Mode.UNKNOWN;
        }
        return files.get(fileIndex).getMode();
    }
}
