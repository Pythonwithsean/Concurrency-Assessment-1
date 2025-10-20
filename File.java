// Captures the client's view of a file
public class File {
  private Mode mode;
  private String content;
  private String filename;

  // constructor
  public File(String filename, String content, Mode mode) {
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
