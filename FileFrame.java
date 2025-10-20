// For storing file information on the server (contains the content and the mode)
class FileFrame {
  public String content;
  public Mode mode;

  public FileFrame(String content, Mode mode) {
    this.content = content;
    this.mode = mode;
  }
}
