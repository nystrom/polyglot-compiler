package polyglot.dispatch;

public class PassthruError extends RuntimeException {

    public PassthruError(Throwable e) {
	super(e);
    }
}
