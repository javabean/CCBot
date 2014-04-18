/**
 *
 */
package fr.cedrik.ccbot;

/**
 * @author C&eacute;drik LIME
 */
public class GameDownException extends RuntimeException {

	public GameDownException() {
	}

	public GameDownException(String message) {
		super(message);
	}

	public GameDownException(Throwable cause) {
		super(cause);
	}

	public GameDownException(String message, Throwable cause) {
		super(message, cause);
	}

	public GameDownException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
