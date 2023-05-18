package ooo.sansk.vaccine.exception;

public class PackageLoadFailedException extends RuntimeException {
    public PackageLoadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
