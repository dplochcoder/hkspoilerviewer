package hollow.knight.logic;

// An exception while saving to ICDL format.
public final class ICDLException extends Exception {
  private static final long serialVersionUID = 1L;

  public ICDLException(String message) {
    super(message);
  }
}
