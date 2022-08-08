package hollow.knight.logic;

public class ParseException extends Exception {
  private static final long serialVersionUID = 1L;

  public ParseException(String msg) {
    super(msg);
  }

  public ParseException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
